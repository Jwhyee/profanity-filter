package io.github.jwhyee.profanity.validator

import io.github.jwhyee.profanity.dto.Profanity
import io.github.jwhyee.profanity.policy.ProfanityFilterRegex
import org.ahocorasick.trie.PayloadTrie
import java.util.concurrent.ConcurrentHashMap

/**
 * 비속어 유효성 검사를 수행하는 엔진입니다.
 * 생성 비용이 높으므로 인스턴스를 하나만 생성하여 재사용(Singleton)하는 것을 권장합니다.
 */
class ProfanityValidator(
    private val trie: PayloadTrie<Profanity>,
    private val allowWords: Set<String> = emptySet(),
) {
    private val allowTrieCache = ConcurrentHashMap<Set<ProfanityFilterRegex>, PayloadTrie<String>>()
    private val defaultPolicies = setOf(ProfanityFilterRegex.NUMBERS, ProfanityFilterRegex.WHITESPACES)

    /**
     * 문장에 비속어가 포함되어 있는지 검증합니다.
     * @param sentence 검사할 원문
     * @throws ProfanityDetectedException 비속어 발견 시 발생
     */
    fun validate(sentence: String) {
        if (sentence.isBlank()) return

        // 1. 정책 기반 전처리 (공백, 숫자 제거 등)
        val filtered = applyPolicies(sentence, defaultPolicies)

        // 2. 일차 탐지
        val detectedBans = trie.parseText(filtered)
        if (detectedBans.isEmpty()) return

        // 3. 허용 단어(WhiteList) 제외 처리
        val allowTrie = getAllowTrie(defaultPolicies)
        val detectedAllows = allowTrie.parseText(filtered)

        val remains = detectedBans.asSequence()
            .filterNot { ban ->
                detectedAllows.any { allow -> overlaps(ban.start, ban.end, allow.start, allow.end) }
            }
            .map { it.payload.word }
            .distinct()
            .toList()

        if (remains.isNotEmpty()) {
            throw ProfanityDetectedException(remains)
        }
    }

    private fun applyPolicies(text: String, policies: Set<ProfanityFilterRegex>): String {
        if (policies.isEmpty()) return text
        val combinedRegex = policies.joinToString("|") { "(${it.regex})" }.toRegex()
        return text.replace(combinedRegex, "")
    }

    private fun getAllowTrie(policies: Set<ProfanityFilterRegex>): PayloadTrie<String> {
        return allowTrieCache.computeIfAbsent(policies) { key ->
            val builder = PayloadTrie.builder<String>()
            allowWords.forEach { word ->
                val normalized = applyPolicies(word, key)
                if (normalized.isNotBlank()) builder.addKeyword(normalized, normalized)
            }
            builder.build()
        }
    }

    private fun overlaps(s1: Int, e1: Int, s2: Int, e2: Int) = s1 <= e2 && s2 <= e1
}