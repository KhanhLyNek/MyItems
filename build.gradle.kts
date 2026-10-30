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
    compileOnly("com.praya:AgarthaLib:2.17.2-paper26.3-SNAPSHOT")

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
