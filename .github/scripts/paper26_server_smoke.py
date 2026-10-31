#!/usr/bin/env python3
"""Exercise the actual Paper 26.3 server startup with both built plugins.

This is a server boot / plugin lifecycle smoke test, NOT a simulated
MythicMobs gameplay test. It never touches a real world or player inventory.
"""
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import urllib.request
import urllib.error

ROOT = Path(__file__).resolve().parents[2]
USER_AGENT = "MyItems-Paper26-GitHubCI/1.0 (https://github.com/KhanhLyNek/MyItems)"
API = "https://fill.papermc.io/v3/projects/paper/versions/26.3/builds"


def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=45) as response:
        return json.load(response)


def download_paper(destination):
    builds = get_json(API)
    if not isinstance(builds, list):
        raise RuntimeError("Unexpected PaperMC v3 response for 26.3 builds")
    # Require an officially stable build for the actual target. No fallback
    # to an older Minecraft release or to experimental paper artifacts.
    stable = [b for b in builds if b.get("channel") == "STABLE"
              and b.get("downloads", {}).get("server:default", {}).get("url")]
    if not stable:
        raise RuntimeError("No stable Paper 26.3 server build in official API")
    chosen = max(stable, key=lambda b: int(b["id"]))
    entry = chosen["downloads"]["server:default"]
    url = entry["url"]
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=120) as response:
        with destination.open("wb") as output:
            shutil.copyfileobj(response, output)
    expected = entry.get("checksums", {}).get("sha256")
    if expected:
        with destination.open("rb") as source:
            actual = hashlib.file_digest(source, "sha256").hexdigest()
        if actual.lower() != expected.lower():
            raise RuntimeError("Paper server download SHA256 mismatch")
    print(f"Verified Paper 26.3 STABLE build {chosen['id']} ({destination.stat().st_size} bytes)", flush=True)


def plugin_jar(directory):
    paths = [p for p in directory.glob("*.jar")
             if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if len(paths) != 1:
        raise RuntimeError(f"Expected one built plugin in {directory}: {paths}")
    return paths[0]


def run_server(folder):
    folder.mkdir(parents=True, exist_ok=True)
    (folder / "plugins").mkdir()
    # Seed old data folders before startup and require preservation by both
    # renamed plugins; never touch a real server or overwrite a real config.
    for old in ("MyItems", "AgarthaLib"):
        legacy = folder / "plugins" / old
        legacy.mkdir()
        (legacy / "legacy-rename-probe.txt").write_text(
            "Original " + old + " config marker", encoding="utf-8")
    (folder / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (folder / "server.properties").write_text(
        # The disposable offline-mode server must NEVER listen publicly.
        "server-ip=127.0.0.1\n"
        "online-mode=false\n"
        "max-players=1\n"
        "spawn-protection=0\n"
        "enable-query=false\n"
        "enable-rcon=false\n"
        "server-port=25581\n"
        "view-distance=2\n"
        "simulation-distance=2\n",
        encoding="utf-8")
    for built in (ROOT / "build/libs", ROOT / "agarthalib/build/libs"):
        jar = plugin_jar(built)
        shutil.copy2(jar, folder / "plugins" / jar.name)
    fixture = ROOT / "build/paper26-live-probe/Paper26LiveProbe.jar"
    if not fixture.is_file():
        raise RuntimeError("Required Paper26LiveProbe.jar not built")
    shutil.copy2(fixture, folder / "plugins" / fixture.name)
    essential_fixture = ROOT / "build/paper26-essentials-fixture/Paper26EssentialsFixture.jar"
    if not essential_fixture.is_file():
        raise RuntimeError("Required test-only Essentials IWarps provider not built")
    shutil.copy2(essential_fixture, folder / "plugins" / essential_fixture.name)
    for provider in ("itemsadder", "oraxen"):
        jar = ROOT / f"build/paper26-{provider}-fixture/Paper26{provider.title()}Fixture.jar"
        if provider == "itemsadder":
            jar = ROOT / "build/paper26-itemsadder-fixture/Paper26ItemsAdderFixture.jar"
        if not jar.is_file():
            raise RuntimeError(f"Required CI-only {provider} model API fixture not built")
        shutil.copy2(jar, folder / "plugins" / jar.name)
    download_paper(folder / "paper.jar")
    process = subprocess.Popen(
        ["java", "-Xms512M", "-Xmx2G", "-jar", "paper.jar", "--nogui"],
        cwd=folder, text=True, encoding="utf-8", errors="replace",
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        bufsize=1)
    lines = []
    events = queue.Queue()
    def reader():
        for line in process.stdout:
            lines.append(line.rstrip())
            print(line, end="", flush=True)
            events.put(line)
    thread = threading.Thread(target=reader, daemon=True)
    thread.start()
    ready = False
    deadline = time.monotonic() + 250
    try:
        while time.monotonic() < deadline and process.poll() is None:
            try:
                line = events.get(timeout=1)
            except queue.Empty:
                continue
            if "Done (" in line and "For help" in line:
                ready = True
                break
        if ready:
            for command in ("version LyMeowzItem", "version LyMeowzLib", "stop"):
                process.stdin.write(command + "\n")
                process.stdin.flush()
                time.sleep(1)
        else:
            raise AssertionError("Paper 26.3 did not finish startup")
        try:
            process.wait(timeout=80)
        except subprocess.TimeoutExpired:
            raise AssertionError("Paper did not shut down cleanly")
    finally:
        if process.poll() is None:
            process.kill()
            process.wait(timeout=10)
        (ROOT / "paper26-server-smoke.log").write_text("\n".join(lines)+"\n", encoding="utf-8")
    all_logs = "\n".join(lines)
    for plugin in ("LyMeowzLib", "LyMeowzItem"):
        if f"[{plugin}] Enabling {plugin}" not in all_logs:
            raise AssertionError(f"Plugin {plugin} did not reach onEnable")
    if "Starting Minecraft server on 127.0.0.1:" not in all_logs:
        raise AssertionError("Offline test server did not bind to loopback only")
    for plugin in ("LyMeowzItem", "LyMeowzLib"):
        if f"{plugin} version " not in all_logs:
            raise AssertionError(f"Paper did not complete the version command for {plugin}")
    if "PASS: LyMeowz plugin identities, command aliases and old data folders" not in all_logs:
        raise AssertionError("Renamed plugin identity, command or migration fixture failed")
    if "PASS: live legacy NBT read-only fallback and PDC precedence" not in all_logs:
        raise AssertionError("Real Paper legacy NBT bridge probe did not pass")
    if "PASS: 25/25 weapon ability classes registered, configured and lore-roundtripped" not in all_logs:
        raise AssertionError("Real Paper 25-ability registration/configuration/lore probe did not pass")
    if "PASS: 3/3 real mob-target skill casts (Freeze, Roots, Curse)" not in all_logs:
        raise AssertionError("Real Paper mob-target ability casts failed")
    if "PASS: EssentialsX 2.22 IWarps contract on real Paper plugin classloaders" not in all_logs:
        raise AssertionError("Modern EssentialsX public warp ABI provider smoke failed")
    if "PASS: ItemsAdder/Oraxen model-only bridges and ItemStack roundtrip" not in all_logs:
        raise AssertionError("Model provider bridge/metadata isolation failed on real Paper")
    if "PASS: Paper Stats item lore NBT and PvP/PvE branch contracts" not in all_logs:
        raise AssertionError("Real Paper Stats lore/NBT contract probe failed")
    if "PASS: Paper HIGH-priority protection cancellation interoperability" not in all_logs:
        raise AssertionError("Cancel-protected Paper combat hit still affected item or health")
    if "PASS: Paper live mob-vs-mob stats damage event" not in all_logs:
        raise AssertionError("Real Paper entity combat probe failed")
    if "PASS: MyItems HEX and legacy item-name/lore commands on real Paper 26.3" not in all_logs:
        raise AssertionError("MyItems HEX/legacy name or lore Paper API integration failed")
    if process.returncode != 0:
        raise AssertionError(f"Paper exited with {process.returncode}")
    for line in lines:
        if any(token in line for token in (
                "Error occurred while enabling", "Error occurred while disabling",
                "Could not load", "NoClassDefFoundError", "ClassNotFoundException",
                "NoSuchMethodError", "NoSuchFieldError", "Exception in thread",
                "java.lang.ExceptionInInitializerError")):
            raise AssertionError(f"Plugin/Paper runtime error: {line}")
    print("PASS: both plugin lifecycle entrypoints loaded on real Paper 26.3", flush=True)


def main():
    with tempfile.TemporaryDirectory(prefix="myitems-paper26-server-") as folder:
        run_server(Path(folder))


if __name__ == "__main__":
    main()
