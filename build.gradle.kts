import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.0"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.0"
	kotlin("plugin.spring") version "1.6.0"
}

group = "com.grispi"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework:spring-aop:5.3.14")
	implementation("org.springframework:spring-aspects:5.3.13")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-mustache")
	runtimeOnly("org.springframework.boot:spring-boot-devtools")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.apache.commons:commons-lang3:3.12.0")
	implementation("redis.clients:jedis:3.6.3")
	runtimeOnly("ch.qos.logback:logback-classic")
	implementation("org.jodd:jodd-json:6.0.3")
	implementation("org.jodd:jodd-http:6.0.6")
	implementation("com.googlecode.libphonenumber:libphonenumber:8.12.25")
	implementation("com.vdurmont:emoji-java:5.1.1")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
