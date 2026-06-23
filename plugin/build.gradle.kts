import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    // 2.x. 2.7.0+ 는 Gradle 9 를 요구하므로 루트와 동일한 Gradle 8.13 에 맞춰 2.6.0 고정(상향은 T17 에서).
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "net.spooncast.openmocker"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // 외부 런타임 의존성 0 — HTTP 는 Java HttpClient, JSON 은 플랫폼 번들 Gson 을 쓴다(T11+).
    intellijPlatform {
        // 대상 IDE: IntelliJ IDEA Community 2024.2 (JDK 21). AS 호환 범위(since/until)는 T17 에서 확정.
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.5")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            // untilBuild 와 pluginVerification 상세는 T17(패키징/배포)에서 확정한다.
        }
    }
    // 커스텀 설정(Settings UI)이 없는 골격 단계 — 검색 옵션 빌드를 꺼 빌드 시간을 줄인다(T13 에서 재검토).
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(21)
}
