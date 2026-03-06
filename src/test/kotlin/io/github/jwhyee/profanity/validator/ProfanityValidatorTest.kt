package io.github.jwhyee.profanity.validator

import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProfanityValidatorTest : StringSpec({

    "공백이나 숫자가 포함된 변칙 비속어를 정확하게 마스킹한다" {
        val trie = ProfanityTrie.create(customWords = listOf("시발"))
        val validator = ProfanityValidator(trie)
        val input = "이건 시 1 발이야"

        val filtered = validator.filter(input)

        // "시 1 발"은 5글자이므로 "*****"로 마스킹됨 (원본 인덱스 0부터 4까지)
        filtered shouldBe "이건 *****이야"
    }

    "허용 단어에 포함된 경우 마스킹하지 않는다" {
        val trie = ProfanityTrie.create(customWords = listOf("시발"))
        val validator = ProfanityValidator(trie, allowWords = setOf("시발점"))
        val input = "공부의 시발점"

        val filtered = validator.filter(input)

        filtered shouldBe "공부의 시발점"
    }

    "사전 업데이트 후 새로운 금칙어가 즉시 반영된다" {
        val initialTrie = ProfanityTrie.create(customWords = listOf("바보"))
        val validator = ProfanityValidator(initialTrie)
        validator.filter("안녕 바보") shouldBe "안녕 **"

        // when: '천재'를 금칙어로 추가하여 업데이트 (Atomic Swap)
        val newTrie = ProfanityTrie.create(customWords = listOf("바보", "천재"))
        validator.updateTrie(newTrie)

        // then
        validator.filter("안녕 천재") shouldBe "안녕 **"
    }

    "비속어 탐지 시 예외를 발생시킨다" {
        val trie = ProfanityTrie.create(customWords = listOf("시발"))
        val validator = ProfanityValidator(trie)

        shouldThrow<ProfanityDetectedException> {
            validator.validate("이건 시 1 발이야")
        }
    }
})
