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
        val validator = ProfanityValidator(customBannedWords = listOf("바보"))
        validator.filter("안녕 바보") shouldBe "안녕 **"

        // 실시간 단어 추가
        validator.addBannedWords("천재")

        // then
        validator.filter("안녕 천재") shouldBe "안녕 **"
    }

    "금칙어 제거 시 즉시 반영되어 더 이상 필터링되지 않는다" {
        val validator = ProfanityValidator(customBannedWords = listOf("바보"))
        validator.filter("안녕 바보") shouldBe "안녕 **"

        // 실시간 단어 제거
        validator.removeBannedWords("바보")

        // then
        validator.filter("안녕 바보") shouldBe "안녕 바보"
    }

    "허용 단어 추가 시 즉시 반영되어 필터링에서 제외된다" {
        val validator = ProfanityValidator(customBannedWords = listOf("시발"))
        // "시발"은 2글자이므로 "**"로 마스킹됨
        validator.filter("공부의 시발점") shouldBe "공부의 **점"

        // 실시간 허용 단어 추가
        validator.addAllowedWords("시발점")

        // then
        validator.filter("공부의 시발점") shouldBe "공부의 시발점"
    }

    "비속어 탐지 시 예외를 발생시킨다" {
        val trie = ProfanityTrie.create(customWords = listOf("시발"))
        val validator = ProfanityValidator(trie)

        shouldThrow<ProfanityDetectedException> {
            validator.validate("이건 시 1 발이야")
        }
    }
})
