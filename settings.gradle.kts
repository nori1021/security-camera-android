pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// 親フォルダ SecurityCamera をプロジェクトルートとして開いたときに :app を解決する（ML Kit 等の依存が IDE で効くようにする）
rootProject.name = "SecurityCamera"
include(":app")
project(":app").projectDir = file("SecurityCameraAndroid/app")
