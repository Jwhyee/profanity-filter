# JVM용 비속어 필터 (Profanity Filter)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg)](https://kotlinlang.org)

> **주의**: 이 저장소에는 비속어 및 인격 모독 등 다소 공격적인 언어가 포함되어 있습니다.

고성능 비속어 탐지 및 마스킹을 위한 Kotlin/Java 라이브러리입니다. 단순 정규식 기반 필터나 변칙(예: 시1발)에 취약한 기본 루프 체크 방식과 달리, 이 라이브러리는 **아호-코라식(Aho-Corasick) 알고리즘**과 **좌표 매핑(Coordinate Mapping)** 시스템을 결합하여 $O(N)$의 시간 복잡도를 달성하면서도 100%의 정확한 마스킹 정밀도를 유지합니다.

[우아한형제들 기술 블로그의 비속어 탐지 전략](https://techblog.woowahan.com/15764/)에서 영감을 받아 구현되었습니다.

## 성능 엔지니어링

이 라이브러리는 지연 시간(Latency)이 중요한 고처리량(High-throughput) 환경을 위해 설계되었습니다. 입력을 정규화하고 트라이(Trie) 기반 검색을 사용함으로써, 사전의 크기에 상관없이 거의 일정한 성능을 유지합니다.

### 벤치마크 결과
*   **환경**: OpenJDK 21, Apple M1 Pro
*   **입력**: 100,000자 (~15,000 단어)
*   **조건**: 비속어 밀도 5%, 변칙 패턴 50% (숫자/공백 혼합)

| 방법 | 변칙 탐지 여부 | 평균 실행 시간 | 비고                  |
| :--- |:--------:|:--------------------|:--------------------|
| **비속어 필터 (본 라이브러리)** |  **가능**  | **3.15 ms**         | **아호-코라식 + 정규화**    |
| 복잡한 정규식 |    가능    | 46.89 ms            | 긴 문자열에서 상당한 오버헤드 발생 |
| 단순 정규식 |   불가능    | 70.64 ms            | "ㅅ1발" 등 변칙 시도 탐지 실패 |
| `String.contains` 루프 |   불가능    | 2.79 ms             | 가장 빠르나 기능적으로 제한적임   |

**결론**: 이 라이브러리는 복잡한 정규식 수준의 방어력을 제공하면서도 **약 27배 더 빠르며**, 단순 루프 기반 체크와 대등한 속도를 보여줍니다.

## 주요 기능

*   **결정론적 성능 (Deterministic Performance)**: $O(N)$ 검색 복잡도를 통해 금지 단어 목록이 수천 개로 늘어나더라도 예측 가능한 지연 시간을 보장합니다.
*   **변칙 복원력 (Bypass Resiliency)**: 설정 가능한 정규화 정책을 통해 `ㅅ123ㅂ` 또는 `시  발`과 같은 의도적인 난독화를 자동으로 처리합니다.
*   **중단 없는 업데이트 (Zero-Downtime Updates)**: 내부 트라이 구조에 **원자적 교체(Atomic Swaps)**를 사용하여, 락 경합(Lock Contention) 없이 런타임에 금지/허용 단어 목록을 업데이트할 수 있습니다.
*   **좌표 복원 (Coordinate Restoration)**: `IndexMap` 시스템을 구현하여 정규화 후에도 (변칙용 노이즈를 포함한) *원본* 캐릭터를 정확하게 마스킹합니다.
*   **오탐 방지 (False Positive Mitigation)**: 겹치는 구간 탐지를 통한 화이트리스트를 지원합니다 (예: "시발"이 금지어라도 "시발점"은 유지됨).

## 사용법

### 빠른 시작
```kotlin
val validator = ProfanityValidator(
    customBannedWords = listOf("badword"),
    allowWords = listOf("goodword")
)

// 마스킹
val masked = validator.filter("This is a bad word") // "This is a *******"

// 검증
try {
    validator.validate("Don't say badword")
} catch (e: ProfanityDetectedException) {
    println("탐지된 단어: ${e.detectedWords}")
}
```

## 기술 아키텍처

필터링 엔진은 다단계 파이프라인으로 작동합니다:

1.  **정규화 (Normalization)**: `ProfanityFilterRegex`에 정의된 노이즈(숫자, 공백 등)를 입력값에서 제거합니다. `IntArray` 매핑은 남은 각 캐릭터의 원본 위치를 추적합니다.
2.  **트라이 검색 (Trie Search)**: 정규화된 텍스트는 $O(N)$ 매칭을 위해 아호-코라식 트라이를 통과합니다.
3.  **중복 해결 (Overlap Resolution)**: 탐지된 토큰들을 "허용 단어" 트라이와 비교합니다. 금지 토큰이 허용 토큰의 일부인 경우(예: "시발점" 내부의 "시발") 무시됩니다.
4.  **인덱스 복원 (Index Restoration)**: 엔진은 `IndexMap`을 참조하여 마스킹을 위한 *원본* 문자열의 정확한 시작/끝 오프셋을 찾습니다.

## 개발 및 기여

### 명령어
- **빌드**: `./gradlew build`
- **테스트**: `./gradlew test`
- **벤치마크 (JMH)**: `./gradlew jmh`
- **벤치마크 (단순 테스트)**: `./gradlew test --tests "io.github.jwhyee.profanity.policy.PerformanceBenchmark" --info`

## 라이선스
MIT 라이선스에 따라 배포됩니다. 자세한 내용은 `LICENSE`를 참조하세요.
