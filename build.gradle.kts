import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val sqliteJdbcVersion: String by project
val plotlyVersion: String by project

plugins {
  application
  kotlin("jvm") version "1.5.30"
}
group = "us.albertim.fan"
version = "0.0.1"
application {
  mainClass.set("us.albertim.fan.ApplicationKt")
}

tasks.withType(KotlinCompile::class.java) {

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
  }
}
repositories {
  mavenCentral()
  maven("https://repo.kotlin.link")
}

dependencies {
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("space.kscience:plotlykt-server:$plotlyVersion")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
}