plugins {
    id("java")
}

group = "com.ubivismedia.ubiregions"
version = "1.0.0"

depositories {
    mavenCentral()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://mvn.intellectualsites.com/content/repositories/releases/") }
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")
    implementation("com.sk89q.worldedit:worldedit-bukkit:7.2.12")
    implementation("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.6.3")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("UbiRegions")
    archiveVersion.set(version)
    from({ configurations.runtimeClasspath.get().filter { it.exists() }.map { zipTree(it) } })
}
