# Profanity Filter Library (Kotlin/Java)

우아한형제들 기술 블로그의 [비속어 탐지 전략](https://techblog.woowahan.com/15764/)을 참고하여 설계된 비속어 필터링 라이브러리입니다. 아호코라식(Aho-Corasick) 알고리즘을 기반으로 하며, 자바와 스프링 환경에서 즉시 사용 가능합니다.

---

## ✨ 핵심 기능

- **고성능 다중 패턴 매칭**: 수천 개의 금칙어를 문장 길이($O(n)$)에 비례하는 속도로 탐색합니다.
- **우회 방어 정책**: 숫자(`18ㅅㅂ`), 공백(`시  발`) 등을 제거하는 전처리 정책(`ProfanityFilterRegex`)을 지원합니다.
- **지능형 예외 처리 (WhiteList)**: "시발점"과 같이 비속어가 포함된 정상 단어를 구간 중첩 알고리즘으로 정교하게 제외합니다.
- **Trie 빌드 캐싱**: 정책별로 최적화된 허용 단어 트리를 `ConcurrentHashMap`으로 캐싱하여 성능 저하를 방지합니다.

---

## 🚀 시작하기

### 1. 의존성 추가 (Gradle)
이 라이브러리는 아호코라식 구현체인 `aho-corasick`을 사용합니다.

```kotlin
dependencies {
    implementation("org.ahocorasick:aho-corasick:0.6.3")
}

```

### 2. 사용 예시

#### **Kotlin (Spring Bean 방식)**

```kotlin
@Configuration
class FilterConfig {
    @Bean
    fun profanityValidator(): ProfanityValidator {
        // 1. 금칙어 트리 생성 (기본 목록 + 커스텀)
        val banTrie = ProfanityTrie.create(
            customWords = listOf("새로운 욕"),
            excludeWords = listOf("제외할 단어")
        )
        
        // 2. 엔진 초기화 (허용 단어 설정)
        return ProfanityValidator(banTrie, setOf("시발점"))
    }
}

@Service
class FilterService(
    private val profanityValidator: ProfanityValidator
) {
    fun check(keyword: String) {
        try {
            profanityValidator.validate(keyword)
        } catch(e: ProfanityDetectedException) {
            ...
        }
    }
}

```

#### **Java (Standard 방식)**

```java
public class MyService {
    private static final ProfanityValidator VALIDATOR = new ProfanityValidator(
        ProfanityTrie.create(),
        Set.of("시발점")
    );

    public void check(String text) {
        try {
            VALIDATOR.validate(text);
        } catch (ProfanityDetectedException e) {
            System.out.println("감지된 욕설: " + e.getDetectedWords());
        }
    }
}

```

---

## 🛠 아키텍처 및 성능 최적화

### 1. 탐지 및 제외 알고리즘

1. **정규화**: 입력 문장에서 숫자와 공백을 정책에 따라 제거합니다.
2. **금칙어 탐지**: `banTrie`를 통해 1차 비속어를 추출합니다.
3. **허용 단어 탐지**: 정규화된 문장에서 허용 단어(`allowWords`) 위치를 탐색합니다.
4. **구간 교집합 판단**: 금칙어 구간이 허용 단어 구간에 포함되거나 겹치면 제외합니다.
* **수식**:  (두 구간이 겹칠 조건)

### 2. 캐싱의 중요성

아호코라식의 **트리 빌드(Build)** 과정은 탐색에 비해 훨씬 무거운 작업입니다. 특히 `allowWords`에 정규화 정책을 적용하여 매번 트리를 만드는 것은 실시간 서비스에서 큰 병목을 야기합니다.

* **정책 기반 캐싱**: `ConcurrentHashMap<Set<ProfanityFilterRegex>, PayloadTrie>`를 통해 동일한 전처리 정책 조합에 대해서는 이미 빌드된 트리를 즉시 반환합니다.
* **안정성**: 욕설이 집중적으로 유입되는 상황에서도 추가적인 트리 빌드 없이 즉각적인 필터링이 가능하여 서버 부하를 방지합니다.

---

## ⚙️ 전처리 정책 ([ProfanityFilterRegex])

| 정책 | 정규식 | 설명 |
| --- | --- | --- |
| `NUMBERS` | `[\p{N}]` | 유니코드 숫자를 모두 제거하여 `18ㅅㅂ` 등을 탐색 |
| `WHITESPACES` | `[\s]` | 공백을 모두 제거하여 `시  발` 등을 탐색 |

---

## 📝 참고 자료

* [우아한형제들 기술 블로그 - 고르곤졸라는 되지만 고르곤 졸라는 안 돼! 배달의민족에서 금칙어를 관리하는 방법](https://techblog.woowahan.com/15764/)
* [Aho-Corasick Algorithm Wikipedia](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)