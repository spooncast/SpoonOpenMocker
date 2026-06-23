// Standalone Gradle build for the OpenMocker IntelliJ/Android Studio plugin.
// 루트 안드로이드 빌드와 격리한다 — 루트 settings.gradle.kts 의 FAIL_ON_PROJECT_REPOS 와
// IntelliJ Platform 리포지토리가 충돌하므로, 루트에 include 하지 않고 자체 settings 를 둔다.
// 리포지토리(mavenCentral + intellijPlatform.defaultRepositories)는 build.gradle.kts 에 둔다.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "openmocker-plugin"
