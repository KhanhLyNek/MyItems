plugins {
    `java`
}

group = "com.praya"
version = "6.4.6-paper26.3-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")

    // Publish AgarthaLib to Maven local first using:
    // cd ../AgarthaLib && gradle publishToMavenLocal
    compileOnly("com.praya:AgarthaLib:2.17.2-paper26.3-SNAPSHOT")

    // TODO: add supported versions of optional external plugin APIs.
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
