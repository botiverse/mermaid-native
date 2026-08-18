plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
