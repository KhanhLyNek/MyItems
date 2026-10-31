plugins {
    `java`
}

group = "com.praya"
version = "6.4.6-paper26.3-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mvn.lumine.io/repository/maven-public/")
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Match the optional public APIs selected by AgarthaLib.
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.helpch.at/releases/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io") {
        content { includeGroup("com.github.MilkBowl") }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")

    // Publish AgarthaLib to Maven local first using:
    // cd ../AgarthaLib && gradle publishToMavenLocal
    compileOnly("com.praya:LyMeowzLib:2.17.2-paper26.3-SNAPSHOT")

    // Optional APIs are provided by installed plugins, NEVER bundled in MyItems.
    // Keep these coordinates identical to the corresponding AgarthaLib APIs.
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("org.jetbrains:annotations:26.0.2")
    // Legacy bStats serializes telemetry with org.json.simple. Include this
    // small runtime dependency inside MyItems, not only on the compile path.
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1") { isTransitive = false }
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        isTransitive = false
    }
    // API-only: reject VaultAPI's stale Bukkit 1.13 transitive dependency.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    // EssentialsX 2.22.0 artifact is not resolvable by GitHub CI. Keep
    // its optional runtime warp adapter via verified reflection, not a
    // mandatory compile classpath dependency that prevents all builds.
    // EssentialsX 2.22.0 is an optional runtime integration: its real warp
    // API is resolved by SupportEssentials without linking its unstable JAR.
    // Still required: complete safe migration of historic NMS and other hooks.
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        // Typed MythicMobs 5 API reader is compiled in an isolated CI task
        // against the real 5.x API, never linked into the Paper runtime jar.
        // Production event callbacks use Paper26MythicMobsEventBridge reflection.
        java.exclude("**/modern/Paper26MythicMobsReader.java")
        // Retire only old typed plugin event listeners, never MyItems abilities.
        // All old source remains in Git for migration analysis.
        java.exclude("**/listener/support/ListenerPlayerLevelUp.java")
        java.exclude("**/listener/support/ListenerMythicMobDeath.java")
        java.exclude("**/listener/support/ListenerMythicMobSpawn.java")
        java.exclude("**/listener/support/ListenerPlayerStaminaRegenChange.java")
        java.exclude("**/listener/support/ListenerPlayerStaminaMaxChange.java")
        java.exclude("**/listener/support/ListenerPlayerHealthRegenChange.java")
        resources.setSrcDirs(listOf("resources"))
    }
}

// MyItems is deployed as a Bukkit plugin JAR. Implementation dependencies
// are not automatically available in the server's plugin classloader.
tasks.named<Jar>("jar") {
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("json-simple-") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000", "-Xmaxwarns", "200"))
}

// This verifies the REAL MyItems PAPI and MVdW bridge sources before the full
// AgarthaLib NMS build succeeds. Its isolated classpath never resolves the
// unpublished complete AgarthaLib JAR.
val paper26PlaceholderApi = configurations.create("paper26PlaceholderApi")
dependencies {
    add(paper26PlaceholderApi.name, "io.papermc.paper:paper-api:26.3.build.230-stable")
    add(paper26PlaceholderApi.name, "me.clip:placeholderapi:2.12.3")
    add(paper26PlaceholderApi.name, "org.jetbrains:annotations:26.0.2")
}

tasks.register<JavaCompile>("compilePaper26MyItemsPlaceholderBridges") {
    description = "Compile real MyItems PAPI 2.12 expansion and shared AgarthaLib MVdW registration"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/builder/placeholder/ReplacerPlaceholderAPIBuild.java")
        include("src/com/praya/myitems/builder/placeholder/ReplacerMVDWPlaceholderAPIBuild.java")
        include("src/com/praya/myitems/builder/placeholder/Paper26LegacyPlaceholderId.java")
        include("ci/paper26-placeholder-bridge-stubs/**/*.java")
    }
    classpath = paper26PlaceholderApi + files(
        "agarthalib/build/paper26-api-check"
    )
    destinationDirectory.set(layout.buildDirectory.dir("paper26-myitems-placeholder-api-check"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    options.encoding = "UTF-8"
    options.release.set(25)
}

// Compile the actual read-only MyItems-to-AgarthaLib MythicLib boundary,
// using only the CI-scope ABI signature until the whole library JAR builds.
tasks.register<JavaCompile>("compilePaper26MyItemsMythicLib") {
    description = "Verify optional MythicLib provider boundary without bundling MythicLib"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/modern/Paper26MyItemsMythicLibAdapter.java")
        include("ci/paper26-mythiclib-stubs/**/*.java")
    }
    classpath = paper26PlaceholderApi
    destinationDirectory.set(layout.buildDirectory.dir("paper26-myitems-mythiclib-api-check"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}


// Compile MyItems' actual new optional provider bridge independently from
// AgarthaLib's still-blocked full NMS build. CI ABI stubs are never shipped.
tasks.register<JavaCompile>("compilePaper26MyItemsModernProviders") {
    description = "Compile AuraSkills/MMOCore/Lands/DecentHolograms optional MyItems seam"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/modern/Paper26MyItemsModernProviders.java")
        include("ci/paper26-modern-provider-stubs/**/*.java")
    }
    classpath = paper26PlaceholderApi
    destinationDirectory.set(layout.buildDirectory.dir("paper26-myitems-modern-providers-api-check"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}

// Dependency-free regression checks for the actual MVdW suffix parser.
tasks.register<JavaCompile>("compilePaper26LegacyPlaceholderIdSelfTest") {
    description = "Compile pure Java MyItems legacy placeholder ID regression tests"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/builder/placeholder/Paper26LegacyPlaceholderId.java")
        include("ci/paper26-placeholder-prefix-test/**/*.java")
    }
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("paper26-placeholder-prefix-tests"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<JavaExec>("testPaper26LegacyPlaceholderIds") {
    description = "Verify MVdW ID matching and lossless suffix extraction"
    group = "verification"
    dependsOn("compilePaper26LegacyPlaceholderIdSelfTest")
    classpath = files(layout.buildDirectory.dir("paper26-placeholder-prefix-tests"))
    mainClass.set("com.praya.myitems.builder.placeholder.Paper26LegacyPlaceholderIdSelfTest")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

// The existing GitHub Actions bridge compile now runs the regression test too.
tasks.named("compilePaper26MyItemsPlaceholderBridges") {
    dependsOn("testPaper26LegacyPlaceholderIds")
}


// Verify real MythicMobs 5.x public event/config API without bundling the
// third-party plugin or bringing its whole distribution into our runtime JAR.
// 5.12.1 is the official wiki's documented ABI example; 5.13.x must be
// runtime-tested on the actual Paper target before the old listeners are enabled.
val paper26MythicMobsApi = configurations.create("paper26MythicMobsApi")
dependencies {
    add(paper26MythicMobsApi.name, "io.lumine:Mythic-Dist:5.12.1")
}

tasks.register<JavaCompile>("compilePaper26MythicMobsReader") {
    description = "Compile isolated MythicMobs 5.x event/config reader against published API"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/modern/Paper26MythicMobsReader.java")
    }
    classpath = paper26PlaceholderApi + paper26MythicMobsApi
    destinationDirectory.set(layout.buildDirectory.dir("paper26-mythicmobs-api-check"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}


// Test the production reflection implementation on non-public MythicMobs-like
// implementation classes. Neither those fakes nor MythicMobs are bundled.
tasks.register<JavaCompile>("compilePaper26MythicEventAccessSelfTest") {
    description = "Compile cached public Mythic event ABI accessor and regression cases"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/modern/Paper26MythicEventAccess.java")
        include("ci/paper26-mythic-event-test/**/*.java")
    }
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("paper26-mythic-event-tests"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<JavaExec>("testPaper26MythicEventAccess") {
    description = "Test event API cache, inaccessible implementation classes, and YAML list snapshots"
    group = "verification"
    dependsOn("compilePaper26MythicEventAccessSelfTest")
    classpath = files(layout.buildDirectory.dir("paper26-mythic-event-tests"))
    mainClass.set("com.praya.myitems.modern.Paper26MythicEventAccessSelfTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

// The existing MythicMobs check in the GitHub workflow runs this automatically.
tasks.named("compilePaper26MythicMobsReader") {
    dependsOn("testPaper26MythicEventAccess")
}


// A real-server integration fixture, not part of MyItems' production JAR.
// Uses Paper to deserialize real NBT bytes, then calls the original NBT bridge.
val paper26LiveNbtApi = configurations.create("paper26LiveNbtApi")
dependencies {
    add(paper26LiveNbtApi.name, "net.kyori:adventure-nbt:5.2.0")
}

tasks.register<JavaCompile>("compilePaper26LiveProbe") {
    description = "Compile isolated Paper26 live NBT compatibility probe"
    group = "verification"
    dependsOn("classes")
    source = fileTree("ci/paper26-live-probe/src") { include("**/*.java") }
    classpath = configurations["compileClasspath"] + paper26LiveNbtApi +
            sourceSets.main.get().output
    destinationDirectory.set(layout.buildDirectory.dir("paper26-live-probe/classes"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}

// Fake Essentials is used solely in our disposable Paper server to exercise
// Bukkit plugin classloader boundaries and the public 2.22.0 IWarps ABI.
// No fake class and no real Essentials JAR enters either production artifact.
tasks.register<JavaCompile>("compilePaper26EssentialsFixture") {
    description = "Compile CI-only EssentialsX public warp API provider"
    group = "verification"
    source = fileTree("ci/paper26-essentials-fixture/src") { include("**/*.java") }
    classpath = configurations["compileClasspath"]
    destinationDirectory.set(layout.buildDirectory.dir("paper26-essentials-fixture/classes"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<Jar>("jarPaper26EssentialsFixture") {
    description = "Package CI-only Essentials API-shaped plugin (never a release)"
    group = "verification"
    dependsOn("compilePaper26EssentialsFixture")
    archiveFileName.set("Paper26EssentialsFixture.jar")
    destinationDirectory.set(layout.buildDirectory.dir("paper26-essentials-fixture"))
    from(layout.buildDirectory.dir("paper26-essentials-fixture/classes"))
    from("ci/paper26-essentials-fixture/resources")
}

tasks.register<Jar>("jarPaper26LiveProbe") {
    description = "Assemble a probe plugin loaded by a real Paper 26.3 server"
    group = "verification"
    dependsOn("compilePaper26LiveProbe")
    archiveFileName.set("Paper26LiveProbe.jar")
    destinationDirectory.set(layout.buildDirectory.dir("paper26-live-probe"))
    from(layout.buildDirectory.dir("paper26-live-probe/classes"))
    from("ci/paper26-live-probe/resources")
}


// Optional external model ABI fixtures; never included in the MyItems jar.
tasks.register<JavaCompile>("compilePaper26ItemsAdderFixture") {
    group = "verification"
    source = fileTree("ci/paper26-itemsadder-fixture/src") { include("**/*.java") }
    classpath = configurations["compileClasspath"]
    destinationDirectory.set(layout.buildDirectory.dir("paper26-itemsadder-fixture/classes"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}
tasks.register<Jar>("jarPaper26ItemsAdderFixture") {
    group = "verification"
    dependsOn("compilePaper26ItemsAdderFixture")
    archiveFileName.set("Paper26ItemsAdderFixture.jar")
    destinationDirectory.set(layout.buildDirectory.dir("paper26-itemsadder-fixture"))
    from(layout.buildDirectory.dir("paper26-itemsadder-fixture/classes"))
    from("ci/paper26-itemsadder-fixture/resources")
}

tasks.register<JavaCompile>("compilePaper26OraxenFixture") {
    group = "verification"
    source = fileTree("ci/paper26-oraxen-fixture/src") { include("**/*.java") }
    classpath = configurations["compileClasspath"]
    destinationDirectory.set(layout.buildDirectory.dir("paper26-oraxen-fixture/classes"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}
tasks.register<Jar>("jarPaper26OraxenFixture") {
    group = "verification"
    dependsOn("compilePaper26OraxenFixture")
    archiveFileName.set("Paper26OraxenFixture.jar")
    destinationDirectory.set(layout.buildDirectory.dir("paper26-oraxen-fixture"))
    from(layout.buildDirectory.dir("paper26-oraxen-fixture/classes"))
    from("ci/paper26-oraxen-fixture/resources")
}

 // Dependency-free tests for lore stat parsing and numerical combat/EXP guards.
tasks.register<JavaCompile>("compilePaper26StatsSelfTest") {
    description = "Compile isolated MyItems stats parsing and combat math regression tests"
    group = "verification"
    source = fileTree(".") {
        include("src/com/praya/myitems/modern/Paper26LoreStatsNumbers.java")
        include("src/com/lymeowz/item/stats/LoreStatsNumbers.java")
        include("src/com/praya/myitems/modern/Paper26StatsMath.java")
        include("src/com/lymeowz/item/stats/CombatMath.java")
        include("src/com/praya/myitems/modern/Paper26ModernStatsFormulas.java")
        include("src/com/lymeowz/item/stats/ModernStatsFormulas.java")
        include("ci/paper26-stats-test/**/*.java")
    }
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("paper26-stats-tests"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<JavaExec>("testPaper26Stats") {
    description = "Run safe lore, XP, accuracy, damage and chance regression checks"
    group = "verification"
    dependsOn("compilePaper26StatsSelfTest")
    classpath = files(layout.buildDirectory.dir("paper26-stats-tests"))
    mainClass.set("com.praya.myitems.modern.Paper26StatsSelfTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

tasks.named("check") {
    dependsOn("testPaper26Stats")
}
