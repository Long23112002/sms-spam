// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.android.junit5)
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.detekt.gradle.plugin)
        classpath(libs.dokka.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.android) apply false
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }
}