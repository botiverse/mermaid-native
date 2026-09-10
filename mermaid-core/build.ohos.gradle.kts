plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
