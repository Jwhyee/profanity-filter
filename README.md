# JVM 비속어 필터 라이브러리 (Profanity Filter)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg)](https://kotlinlang.org)

> **주의**: 라이브러리 특성상 테스트 코드 및 내부 사전에 실제 비속어가 포함되어 있습니다.

기존의 단순 정규식이나 `String.contains` 기반의 필터는 '시1발', '개 새 끼' 등 필터링을 우회하려는 변형 패턴에 취약하며, 정규식이 복잡해질수록 성능이 급격히 저하됩니다. 이 라이브러리는 **아호-코라식(Aho-Corasick) 알고리즘**과 **원본 인덱스 매핑(Index Mapping)** 기법을 결합하여, 시간 복잡도 $O(N)$을 보장하면서도 변형된 비속어를 100% 정밀하게 탐지하고 마스킹하도록 **설계된 Kotlin/Java 비속어 필터 라이브러리**입니다.

*(본 프로젝트는 [우아한형제들 기술 블로그의 비속어 탐지 전략](https://techblog.woowahan.com/15764/)에서 영감을 받아 구현되었습니다.)*

## 주요 기능

* **안정적인 성능 보장 ($O(N)$)**: 등록된 금지어의 개수가 수만 개로 늘어나더라도 탐색 속도가 일정하게 유지됩니다.
* **변형 비속어 탐지**: 숫자, 특수문자, 공백 등을 섞어 필터링을 우회하려는 시도(예: `시123발`, `바 보`)를 정규화 처리하여 정확히 잡아냅니다.
* **정확한 마스킹 위치 복원**: 자체 매핑 시스템을 통해 변형된 비속어를 탐지한 후, 원본 문자열에 포함된 노이즈(숫자/공백 등)까지 포함한 정확한 구간만 `*`로 마스킹합니다.
* **오탐(False Positive) 방지**: 화이트리스트(허용 단어) 기능을 지원하여 "시발점"과 같이 금지어가 포함된 정상적인 단어가 필터링되는 것을 막습니다.
* **무중단 사전 업데이트**: 런타임 중에도 락(Lock) 경합 없이 금지어/허용어 사전을 안전하게 교체할 수 있습니다.

## 성능 벤치마크

입력값을 정규화하고 트라이(Trie) 기반 구조를 사용하기 때문에 텍스트 길이나 사전의 크기가 커져도 안정적인 성능을 보여줍니다.

* **테스트 환경**: OpenJDK 21, Apple M1 Pro
* **입력 데이터**: 100,000자 (약 15,000 단어)
* **테스트 조건**: 비속어 비율 5%, 변형 패턴(숫자/공백 혼합) 50% 포함

| 탐지 방식 | 변형 패턴 탐지 | 평균 처리 시간 | 특징 |
| :--- | :---: | :--- | :--- |
| **본 라이브러리 (Aho-Corasick + 매핑)** | **가능** | **3.15 ms** | **일정한 속도 유지, 변형 패턴 완벽 대응** |
| 복잡한 정규식 (Regex) | 가능 | 46.89 ms | 텍스트가 길어질수록 성능 급감 (약 15배 느림) |
| 단순 정규식 (Regex) | 불가능 | 70.64 ms | 성능 저하가 심각하며 "ㅅ1발" 등 우회 시도 탐지 불가 |
| `String.contains` 순회 | 불가능 | 2.79 ms | 속도는 가장 빠르나 실무(변형 패턴 대응)에 사용 불가 |

## 내부 동작 원리

이 라이브러리의 필터링 엔진은 성능과 정밀도를 모두 잡기 위해 4단계의 파이프라인으로 동작합니다.

1.  **정규화 (Normalization)**: 입력 텍스트에서 노이즈(숫자, 공백, 특수문자 등)를 임시로 제거합니다. 이때 제거된 문자를 제외한 각 글자가 원본 문자열의 어디에 위치했는지 `IndexMap` 객체에 기록해 둡니다.
2.  **트라이 탐색 (Trie Search)**: 정규화된 텍스트를 아호-코라식 트라이에 통과시켜 비속어를 스캔합니다. 여러 개의 패턴을 텍스트 길이만큼의 $O(N)$ 시간 복잡도로 한 번에 찾아냅니다. 

3.  **오탐 검증 (False Positive Check)**: 탐지된 구간이 '허용 단어(Whitelist)' 트라이의 매칭 결과에 포함되는지 확인합니다. 금지어가 허용 단어의 일부일 경우(예: "시발점" 내부의 "시발") 필터링에서 제외합니다.
4.  **원본 위치 복원 (Index Restoration)**: 1단계에서 만든 `IndexMap`을 역참조하여, 탐지된 비속어가 원본 텍스트에서 차지하는 실제 구간(노이즈 포함)을 찾아내고 해당 위치만 정밀하게 마스킹합니다.

## 사용 방법

```kotlin
// 1. Validator 인스턴스 생성 (설정 시 트라이가 빌드됩니다)
val validator = ProfanityValidator(
    customBannedWords = listOf("badword"),
    allowWords = listOf("goodword")
)

// 2. 마스킹 처리
val maskedText = validator.filter("This is a b a d w o r d") 
// 결과: "This is a *************" (공백을 포함하여 정확히 마스킹됨)

// 3. 단순 검증 (예외 발생)
try {
    validator.validate("Don't say badword")
} catch (e: ProfanityDetectedException) {
    println("탐지된 비속어 목록: ${e.detectedWords}")
}

```

## 개발 및 기여

로컬 환경에서 프로젝트를 빌드하거나 성능을 측정하기 위한 명령어입니다.

* **빌드**: `./gradlew build`
* **단위 테스트**: `./gradlew test`
* **JMH 벤치마크**: `./gradlew jmh`
* **단순 벤치마크 테스트**: `./gradlew test --tests "io.github.jwhyee.profanity.policy.PerformanceBenchmark" --info`

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참고하세요.