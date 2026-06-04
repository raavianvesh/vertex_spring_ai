import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("java")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.freefairLombok)
}

group = "org.example"
version = "1.0-SNAPSHOT"

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

repositories {
    mavenCentral()
}

configurations.configureEach {
    exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
}

dependencies {
    implementation(platform(libs.awsSdkBom))
    implementation(platform(libs.springAiBom))
    implementation(platform(libs.springCloudAwsBom))
    implementation(platform(libs.jacksonBom))
    implementation(platform(libs.springCloudGcpBom))
    implementation(platform(libs.googleCloudBom))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.swaggerAnnotationsJakarta)
    implementation(libs.springdocOpenapiStarterWebmvcUi)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("io.awspring.cloud:spring-cloud-aws-docker-compose")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("tools.jackson.core:jackson-core")
    implementation("com.google.cloud:spring-cloud-gcp-starter")
    implementation("com.google.cloud:spring-cloud-gcp-starter-vision")
    implementation("com.google.cloud:spring-cloud-gcp-starter-storage")
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai-embedding")
    implementation("com.google.cloud:google-cloud-document-ai")
    implementation("com.github.librepdf:openpdf:${libs.versions.libreOpenPdf.get()}")
    implementation("com.github.librepdf:openpdf-fonts-extra:${libs.versions.libreOpenPdf.get()}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}