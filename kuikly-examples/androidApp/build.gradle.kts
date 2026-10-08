plugins { id("com.android.application"); kotlin("android") }
val kuiklyVersion: String by project
android {
    namespace = "dev.mermaid.examples"; compileSdk = 35
    defaultConfig { applicationId = "dev.mermaid.examples.officialkuikly"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}
dependencies {
    implementation(project(":shared"))
    implementation("com.tencent.kuikly-open:core-render-android:$kuiklyVersion")
}

android { compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
