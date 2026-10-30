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
        actual = hashlib.file_digest(destination.open("rb"), "sha256").hexdigest()
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
    (folder / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (folder / "server.properties").write_text(
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
            for command in ("version MyItems", "version AgarthaLib", "stop"):
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
    for plugin in ("AgarthaLib", "MyItems"):
        if f"[{plugin}] Enabling {plugin}" not in all_logs:
            raise AssertionError(f"Plugin {plugin} did not reach onEnable")
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
