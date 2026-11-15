plugins {
    java
    application
}

application {
    mainClass.set("com.sportify.BackendApplication")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.2")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    testImplementation("junit:junit:4.13.2")
}
