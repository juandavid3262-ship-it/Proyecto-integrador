plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

// change the group developer to your nickname
group = "dev.nakano"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // database
    implementation("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.h2database:h2")

    // testing
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
