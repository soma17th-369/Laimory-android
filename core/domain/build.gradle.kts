
plugins {
    id("java-library")
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
}
