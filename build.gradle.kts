plugins {
    `java`
}

group = "com.praya"
version = "6.4.6-paper26.3-SNAPSHOT"

repositories {
    mavenCentral()
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
    // API-only: reject VaultAPI's stale Bukkit 1.13 transitive dependency.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    // EssentialsX 2.22.0 is an optional runtime integration: its real warp\n    // API is resolved by SupportEssentials without linking its unstable JAR.
    // Still required: complete safe migration of historic NMS and other hooks.
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
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
