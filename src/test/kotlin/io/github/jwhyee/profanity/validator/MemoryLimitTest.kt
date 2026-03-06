package io.github.jwhyee.profanity.validator

import io.github.jwhyee.profanity.policy.ProfanityFilterRegex
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class MemoryLimitTest : StringSpec({

    "대규모 금칙어 사전 로딩 시의 메모리 사용량을 측정한다" {
        val runtime = Runtime.getRuntime()
        
        fun getUsedMemory(): Long {
            runtime.gc()
            return runtime.totalMemory() - runtime.freeMemory()
        }

        val initialMemory = getUsedMemory()
        println("초기 메모리 사용량: ${initialMemory / 1024 / 1024} MB")

        // 1. 10만 개의 고유 금칙어 생성 (평균 10자)
        val largeWords = List(100_000) { UUID.randomUUID().toString().take(10) }
        
        // 2. Validator 인스턴스 생성 및 트라이 빌드
        val validator = ProfanityValidator(customBannedWords = largeWords)
        
        val afterBuildMemory = getUsedMemory()
        val memoryUsage = (afterBuildMemory - initialMemory) / 1024 / 1024
        println("10만 개 금칙어 로딩 후 메모리 사용량: ${afterBuildMemory / 1024 / 1024} MB")
        println("트라이 빌드에 사용된 순수 메모리: $memoryUsage MB")

        // 3. 탐색 성능 확인 (Warm up 포함)
        repeat(100) {
            validator.filter("이것은 정상적인 문장입니다. ${largeWords.random()}가 포함될 수도 있죠.")
        }
        
        // 상식적인 수준(예: 500MB 이하)에서 메모리가 유지되는지 확인
        (memoryUsage < 500) shouldBe true
    }
})
