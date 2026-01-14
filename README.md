# Profanity Filter Library (Kotlin/Java)

> 비속어, 인격 모독 등에 대한 단어가 포함된 리포지토리입니다. 코드를 읽으실 때, 이 점 양해해주시면 감사하겠습니다.

Aho-Corasick 알고리즘 기반의 비속어 필터링 라이브러리입니다. 
숫자나 공백을 섞은 변칙 욕설 탐지와 정교한 허용 단어 예외 처리를 지원합니다. 우아한형제들 기술 블로그의 [비속어 탐지 전략](https://techblog.woowahan.com/15764/)을 참고하여 설계되었습니다.

## 주요 기능

- **다중 패턴 매칭**: 수천 개의 금칙어를 O(n) 시간 복잡도로 탐색
- **변칙 우회 방어**: 숫자(`ㅅ123ㅂ`), 공백(`시  발`) 등을 정규화하여 탐지
- **지능형 예외 처리**: "시발점"과 같이 비속어가 포함된 정상 단어를 구간 중첩 알고리즘으로 제외
- **Trie 빌드 캐싱**: 정책 조합별로 허용 단어 트리를 캐싱하여 실시간 성능 보장

## 설치 방법

### Gradle (Kotlin DSL)

**settings.gradle.kts**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**build.gradle.kts**
```kotlin
dependencies {
    implementation("com.github.Jwhyee:profanity-filter:1.0.0")
}
```

### Gradle (Groovy)

**settings.gradle**
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**build.gradle**
```groovy
dependencies {
    implementation 'com.github.Jwhyee:profanity-filter:1.0.0'
}
```

### Maven

**pom.xml**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Jwhyee</groupId>
        <artifactId>profanity-filter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## 설치 이슈

### Maven Repository 충돌

```
Build was configured to prefer settings repositories over project repositories
```

**해결 방법**: `build.gradle(.kts)`의 `repositories` 블록을 제거하고 `settings.gradle(.kts)`에만 유지하세요.

### Dependency 충돌

이 라이브러리는 내부적으로 `ahocorasick` 의존성을 포함합니다. 프로젝트에서 직접 추가한 `ahocorasick` 의존성이 있다면 제거해주세요.

## 사용 예시

### Kotlin (Spring Configuration)

```kotlin
import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.github.jwhyee.profanity.validator.ProfanityValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {
    @Bean
    fun profanityValidator(): ProfanityValidator {
        val banTrie = ProfanityTrie.create(
            customWords = listOf("추가할 비속어"),
            excludeWords = listOf("제외할 비속어")
        )
        
        return ProfanityValidator(banTrie, setOf("시발점"))
    }
}
```

### Java

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
            System.out.println("감지된 비속어: " + e.getDetectedWords());
        }
    }
}
```

## 동작 원리

### 탐지 알고리즘

1. **정규화**: 입력 문장에서 정책에 따라 숫자와 공백을 제거 (예: `시 1발` → `시발`)
2. **금칙어 탐지**: Aho-Corasick 알고리즘으로 비속어 구간 탐색
3. **허용 단어 탐지**: 동일한 정책을 적용한 허용 단어 트리로 예외 구간 탐색
4. **구간 교집합 판단**: 금칙어 구간이 허용 단어 구간과 겹치면 제외

### 캐싱 전략

`Set<ProfanityFilterRegex>`를 키로 사용하여 동일한 정규화 정책 조합에 대해 빌드된 Trie를 재사용합니다. 이를 통해 무거운 빌드 과정을 반복하지 않고 실시간 성능을 보장합니다.

### 전처리 정책

| 정책 | 정규식 | 설명 |
|------|--------|------|
| `NUMBERS` | `[\p{N}]` | 숫자 혼용 우회 탐지 (예: `시1발`) |
| `WHITESPACES` | `[\s]` | 공백 우회 탐지 (예: `시 발`) |

## 프로젝트 구조

```
io.github.jwhyee.profanity
├── dto        # 데이터 모델
├── helper     # Trie 빌더 및 유틸리티
├── policy     # 비속어 목록 및 정규화 정책
└── validator  # 검증 엔진 및 예외 처리
```

## 참고 자료

- [우아한형제들 기술 블로그 - 고르곤졸라는 되지만 고르곤 졸라는 안 돼! 배달의민족에서 금칙어를 관리하는 방법](https://techblog.woowahan.com/15764/)
- [Aho-Corasick Algorithm Wikipedia](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)

## License

이 프로젝트는 MIT License를 따릅니다. 자세한 내용은 [LICENSE](./LICENSE) 파일을 확인해주세요.