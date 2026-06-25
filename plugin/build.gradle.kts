import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    // 2.x. 2.7.0+ 는 Gradle 9 를 요구하므로 루트와 동일한 Gradle 8.13 에 맞춰 2.6.0 고정(T17 에서 유지 확정).
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "net.spooncast.openmocker"
version = "0.1.0-beta2"

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

    // BasePlatformTestCase(플랫폼 컨테이너 부팅 테스트)의 단언이 opentest4j 를 요구하나 플랫폼이
    // 런타임 제공하지 않아 명시한다. 테스트 전용 — 플러그인 런타임 의존성 0 원칙엔 영향 없다.
    testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 사내 zip 수동 배포 — sinceBuild=242(AS Ladybug 2024.2+) 이상이면 모두 로드 가능하도록
            // untilBuild 상한을 두지 않는다(개방형). 미설정 시 2.x 가 기본값 "242.*" 를 박아 최신
            // AS/IDEA 로드를 막으므로 명시적으로 null 을 줘 상한을 제거한다. 결정: decisions/plugin-distribution-internal-zip.md
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
    // 사내 배포 전 호환성 점검. recommended() 는 sinceBuild~untilBuild 범위의 권장 IDE 들을 대상으로
    // IntelliJ Plugin Verifier 를 돌린다. (필요 시 ./gradlew verifyPlugin 으로 수동 실행 — IDE 다운로드 동반)
    pluginVerification {
        ides {
            recommended()
        }
    }
    // 커스텀 설정(Settings UI)이 없는 골격 단계 — 검색 옵션 빌드를 꺼 빌드 시간을 줄인다(T13 에서 재검토).
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(21)
}
