# Profanity Filter Library (Kotlin/Java)

우아한형제들 기술 블로그의 [비속어 탐지 전략](https://techblog.woowahan.com/15764/)을 참고하여 설계된 비속어 필터링 라이브러리입니다. 아호코라식(Aho-Corasick) 알고리즘을 기반으로 하며, 숫자나 공백을 섞은 변칙 욕설 탐지와 정교한 허용 단어(WhiteList) 예외 처리를 지원합니다.

---

## ✨ 핵심 기능

- **고성능 다중 패턴 매칭**: 수천 개의 금칙어를 문장 길이($O(n)$)에 비례하는 속도로 탐색합니다.
- **변칙 우회 방어**: 정책(`ProfanityFilterRegex`)에 따라 숫자(`18ㅅㅂ`), 공백(`시  발`) 등을 제거한 후 정규화된 문장에서 비속어를 탐색합니다.
- **지능형 예외 처리**: "시발점"과 같이 비속어가 포함된 정상 단어를 구간 중첩 알고리즘으로 정확하게 제외합니다.
- **Trie 빌드 캐싱**: 무거운 빌드 과정이 필요한 허용 단어 트리를 정책 조합별로 캐싱(`ConcurrentHashMap`)하여 실시간 성능을 보장합니다.

---

## 🚀 Quick Start

### 1. Jitpack 의존성 추가

사용 중인 빌드 도구에 맞춰 저장소와 의존성을 추가하세요. 이미 Jitpack이 있는 경우 2번으로 넘어가주세요.

<details>
<summary><b>Gradle (Kotlin DSL - settings.gradle.kts)</b></summary>

1. `settings.gradle.kts` 파일 끝에 JitPack 저장소를 추가합니다.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

```

2. `build.gradle.kts`에 의존성을 추가합니다.

```kotlin
dependencies {
    implementation("com.github.Jwhyee:profanity-filter:1.0.0")
}

```

</details>

<details>
<summary><b>Gradle (Groovy - settings.gradle)</b></summary>

1. `settings.gradle` 파일 끝에 JitPack 저장소를 추가합니다.

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

```

2. `build.gradle`에 의존성을 추가합니다.

```groovy
dependencies {
    implementation 'com.github.Jwhyee:profanity-filter:1.0.0'
}

```

</details>

<details>
<summary><b>Maven (pom.xml)</b></summary>

1. `pom.xml`에 JitPack 저장소를 추가합니다.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

```

2. 의존성을 추가합니다.

```xml
<dependency>
    <groupId>com.github.Jwhyee</groupId>
    <artifactId>profanity-filter</artifactId>
    <version>1.0.0</version>
</dependency>

```

</details>

라이브러리 하나만 추가하면 ahocorasick 의존성까지 자동으로 포함됩니다. 만약 의존성 충돌 이슈가 발생할 경우, 직접 추가한 `ahocorasick`을 제거하고, 다시 시도해주세요.

### 2. 사용 예시

#### **Kotlin (Spring Configuration)**

```kotlin
import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.github.jwhyee.profanity.validator.ProfanityValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {
    @Bean
    fun profanityValidator(): ProfanityValidator {
        // 1. 금칙어 트리 생성 (기본 목록 포함 및 커스텀 설정)
        // ProfanityPolicy.DEFAULT_PROFANITY_LIST에 추가 및 제외할 단어 목록 입력
        val banTrie = ProfanityTrie.create(
            customWords = listOf("추가할 비속어"),
            excludeWords = listOf("제외할 비속어")
        )
        
        // 2. 엔진 초기화 (허용 단어 목록 설정)
        return ProfanityValidator(banTrie, setOf("시발점"))
    }
}
```

#### **Java (Standard 방식)**

```java
import io.github.jwhyee.profanity.helper.ProfanityTrie;
import io.github.jwhyee.profanity.validator.ProfanityValidator;
import io.github.jwhyee.profanity.validator.ProfanityDetectedException;

public class MyService {
    private static final ProfanityValidator VALIDATOR = new ProfanityValidator(
        ProfanityTrie.create(),
        Set.of("시발점")
    );

    public void check(String text) {
        try {
            VALIDATOR.validate(text);
        } catch (ProfanityDetectedException e) {
            System.out.println("감지된 비속어 목록: " + e.getDetectedWords());
        }
    }
}

```

---

## 🛠 아키텍처 및 성능 최적화

### 1. 탐지 및 제외 알고리즘

1. **정규화**: 입력 문장에서 숫자와 공백을 정책에 따라 제거합니다. (예: `시 1발` -> `시발`)
2. **금칙어 탐지**: `banTrie`를 통해 1차 비속어를 탐색합니다.
3. **허용 단어 탐지**: 동일한 정책을 적용한 허용 단어 트리를 사용하여 예외 구간을 탐색합니다.
4. **구간 교집합 판단**: 탐지된 금칙어 구간이 허용 단어 구간과 겹치면 제외합니다.
* **판단 수식**:  (두 구간이 겹칠 조건)



### 2. 캐싱 전략

아호코라식 트리의 빌드 비용은 매우 높습니다. 특히 사용자가 설정한 `allowWords`를 정규화하여 트리를 생성하는 과정을 최적화하기 위해 캐싱을 사용합니다.

* **정책 기반 캐싱**: `Set<ProfanityFilterRegex>`를 키로 사용하여, 동일한 전처리 조합에 대해서는 이미 빌드된 트리를 재사용합니다.

---

## ⚙️ 전처리 정책 (`ProfanityFilterRegex`)

| 정책 | 정규식 | 설명 |
| --- | --- | --- |
| `NUMBERS` | `[\p{N}]` | 모든 유니코드 숫자를 제거하여 숫자 혼용 우회 탐색 |
| `WHITESPACES` | `[\s]` | 모든 공백을 제거하여 자음/모음 분리 및 공백 우회 탐색 |

---

## 📂 프로젝트 구조

```text
io.github.jwhyee.profanity
├── dto        # 데이터 모델 (Profanity)
├── helper     # Trie 빌더 및 유틸리티 (ProfanityTrie)
├── policy     # 비속어 목록 및 정책 (ProfanityPolicy, Regex)
└── validator  # 핵심 검증 엔진 및 예외 처리

```

---

## 📝 참고 자료

* [우아한형제들 기술 블로그 - 고르곤졸라는 되지만 고르곤 졸라는 안 돼! 배달의민족에서 금칙어를 관리하는 방법](https://techblog.woowahan.com/15764/)
* [Aho-Corasick Algorithm Wikipedia](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)

---

## 📄 License

이 프로젝트는 **MIT License**를 따릅니다. 누구나 자유롭게 수정 및 배포가 가능하며, 기여(PR)를 환영합니다. 단, 재배포 시 저작권 고지 및 라이선스 문구를 포함해야 합니다. 자세한 내용은 [LICENSE](./LICENSE) 파일을 확인해 주세요.