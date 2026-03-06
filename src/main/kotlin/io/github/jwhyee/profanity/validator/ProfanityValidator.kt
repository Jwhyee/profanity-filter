package io.github.jwhyee.profanity.validator

import io.github.jwhyee.profanity.dto.Profanity
import io.github.jwhyee.profanity.policy.ProfanityFilterRegex
import org.ahocorasick.trie.PayloadTrie
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 비속어 유효성 검사 및 마스킹을 수행하는 엔진입니다.
 * 실시간 사전 업데이트(Atomic Swap)와 변칙 우회 방어(Index Mapping)를 지원합니다.
 */
class ProfanityValidator(
    initialTrie: PayloadTrie<Profanity>,
    private val allowWords: Set<String> = emptySet(),
) {
    private val trieReference = AtomicReference(initialTrie)
    private val allowTrieCache = ConcurrentHashMap<Set<ProfanityFilterRegex>, PayloadTrie<String>>()
    private val defaultPolicies = setOf(ProfanityFilterRegex.NUMBERS, ProfanityFilterRegex.WHITESPACES)

    /**
     * 현재 사용 중인 [PayloadTrie] 인스턴스를 반환합니다.
     */
    val currentTrie: PayloadTrie<Profanity>
        get() = trieReference.get()

    /**
     * 새로운 비속어 사전으로 원자적 교체(Atomic Swap)를 수행합니다.
     * @param newTrie 새롭게 빌드된 [PayloadTrie]
     */
    fun updateTrie(newTrie: PayloadTrie<Profanity>) {
        trieReference.set(newTrie)
    }

    /**
     * 문장에 비속어가 포함되어 있는지 검증합니다.
     * @param sentence 검사할 원문
     * @throws ProfanityDetectedException 비속어 발견 시 발생
     */
    fun validate(sentence: String) {
        if (sentence.isBlank()) return

        val mapping = applyPoliciesWithMapping(sentence, defaultPolicies)
        val detectedBans = currentTrie.parseText(mapping.filteredText)
        if (detectedBans.isEmpty()) return

        val allowTrie = getAllowTrie(defaultPolicies)
        val detectedAllows = allowTrie.parseText(mapping.filteredText)

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

    /**
     * 문장 내의 비속어를 마스킹 처리하여 반환합니다.
     * 인덱스 매핑 테이블을 사용하여 원본 텍스트의 변칙 표현(공백, 숫자 포함)까지 정확히 찾아 마스킹합니다.
     *
     * @param sentence 마스킹할 원문
     * @param maskChar 마스킹에 사용할 문자 (기본값: '*')
     * @return 마스킹된 문자열
     */
    fun filter(sentence: String, maskChar: Char = '*'): String {
        if (sentence.isBlank()) return sentence

        val mapping = applyPoliciesWithMapping(sentence, defaultPolicies)
        val detectedBans = currentTrie.parseText(mapping.filteredText)
        if (detectedBans.isEmpty()) return sentence

        val allowTrie = getAllowTrie(defaultPolicies)
        val detectedAllows = allowTrie.parseText(mapping.filteredText)

        val resultChars = sentence.toCharArray()

        detectedBans.forEach { ban ->
            val isAllowed = detectedAllows.any { allow -> overlaps(ban.start, ban.end, allow.start, allow.end) }
            if (!isAllowed) {
                // 매핑 테이블을 통해 원본 인덱스 시작과 끝을 복원
                val originalStart = mapping.indexMap[ban.start]
                val originalEnd = mapping.indexMap[ban.end]
                
                // 원본 텍스트의 해당 범위를 모두 마스킹 (사이의 공백/숫자 포함)
                for (i in originalStart..originalEnd) {
                    resultChars[i] = maskChar
                }
            }
        }

        return String(resultChars)
    }

    /**
     * 전처리를 수행하면서 각 문자의 원본 인덱스를 추적하는 매핑 테이블을 생성합니다.
     */
    private fun applyPoliciesWithMapping(text: String, policies: Set<ProfanityFilterRegex>): IndexMapping {
        if (policies.isEmpty()) {
            return IndexMapping(text, IntArray(text.length) { it })
        }

        val combinedRegex = policies.joinToString("|") { "(${it.regex})" }.toRegex()
        val filtered = StringBuilder()
        val indexMap = IntArray(text.length)
        var filteredIndex = 0

        text.forEachIndexed { originalIndex, char ->
            if (!combinedRegex.matches(char.toString())) {
                filtered.append(char)
                indexMap[filteredIndex++] = originalIndex
            }
        }

        return IndexMapping(filtered.toString(), indexMap.copyOf(filteredIndex))
    }

    private fun getAllowTrie(policies: Set<ProfanityFilterRegex>): PayloadTrie<String> {
        return allowTrieCache.computeIfAbsent(policies) { key ->
            val builder = PayloadTrie.builder<String>()
            allowWords.forEach { word ->
                // 허용 단어도 동일한 정책으로 전처리하여 등록
                val combinedRegex = key.joinToString("|") { "(${it.regex})" }.toRegex()
                val normalized = word.replace(combinedRegex, "")
                if (normalized.isNotBlank()) builder.addKeyword(normalized, normalized)
            }
            builder.build()
        }
    }

    private fun overlaps(s1: Int, e1: Int, s2: Int, e2: Int) = s1 <= e2 && s2 <= e1

    private data class IndexMapping(
        val filteredText: String,
        val indexMap: IntArray
    )
}