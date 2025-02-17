plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "net.qilla"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation(":QLibrary-1.0.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    destinationDirectory.set(file("C:\\Users\\Richard\\Development\\Servers\\1.21.4\\plugins"))
}