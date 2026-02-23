plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    kotlin("jvm")
}

group = "ru.lewis.leykabot"
version = "1.0-SNAPSHOT"

repositories {
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

dependencies {
    // Spring
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.webmvc)
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Database
    runtimeOnly(libs.postgresql)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Telegram
    implementation(libs.telegram.client)
    implementation(libs.telegram.longpolling)

    implementation("org.ton.ton4j:smartcontract:1.3.5")

    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    implementation(kotlin("stdlib-jdk8"))
}
kotlin {
    jvmToolchain(21)
}