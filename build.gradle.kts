// Top-level build file for Curio Notepad.
// Shared plugin versions are declared in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.serialization) apply false
}
