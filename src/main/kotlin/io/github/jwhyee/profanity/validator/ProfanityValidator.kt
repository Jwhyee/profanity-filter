package io.github.jwhyee.profanity.validator

import io.github.jwhyee.profanity.dto.Profanity
import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.github.jwhyee.profanity.policy.ProfanityFilterRegex
import org.ahocorasick.trie.PayloadTrie
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference

/**
 * 비속어 유효성 검사 및 마스킹을 수행하는 엔진입니다.
 * 실시간 사전 업데이트(Atomic Swap)와 변칙 우회 방어(Index Mapping)를 지원합니다.
 */
class ProfanityValidator(
    customBannedWords: Collection<String> = emptyList(),
    excludedWords: Collection<String> = emptyList(),
    allowWords: Collection<String> = emptyList(),
) {
    // 실시간 업데이트를 위한 스레드 안전한 단어 목록 관리
    private val _customBannedWords = CopyOnWriteArraySet(customBannedWords)
    private val _excludedWords = CopyOnWriteArraySet(excludedWords)
    private val _allowWords = CopyOnWriteArraySet(allowWords)

    // Trie 및 캐시 관리 (Atomic Swap 적용)
    private val trieReference = AtomicReference<PayloadTrie<Profanity>>()
    private val allowTrieCache = ConcurrentHashMap<Set<ProfanityFilterRegex>, PayloadTrie<String>>()
    private val defaultPolicies = setOf(ProfanityFilterRegex.NUMBERS, ProfanityFilterRegex.WHITESPACES)

    /**
     * 초기 Trie 빌드
     */
    init {
        rebuildTrie()
    }

    /**
     * 외부에서 빌드된 Trie를 주입받아 즉시 교체하는 보조 생성자입니다.
     */
    constructor(trie: PayloadTrie<Profanity>, allowWords: Set<String> = emptySet()) : this(
        allowWords = allowWords
    ) {
        trieReference.set(trie)
    }

    /**
     * 현재 사용 중인 [PayloadTrie] 인스턴스를 반환합니다.
     */
    val currentTrie: PayloadTrie<Profanity>
        get() = trieReference.get()

    /**
     * 새로운 비속어 사전으로 원자적 교체(Atomic Swap)를 수행합니다.
     */
    fun updateTrie(newTrie: PayloadTrie<Profanity>) {
        trieReference.set(newTrie)
    }

    /**
     * 금칙어를 실시간으로 추가합니다. 추가 후 사전이 자동으로 재빌드됩니다.
     */
    fun addBannedWords(vararg words: String) {
        if (_customBannedWords.addAll(words.toList())) {
            rebuildTrie()
        }
    }

    /**
     * 금칙어를 실시간으로 제거합니다. 제거 후 사전이 자동으로 재빌드됩니다.
     */
    fun removeBannedWords(vararg words: String) {
        if (_customBannedWords.removeAll(words.toSet())) {
            rebuildTrie()
        }
    }

    /**
     * 기본 금칙어 목록에서 제외할 단어를 추가합니다.
     */
    fun addExcludedWords(vararg words: String) {
        if (_excludedWords.addAll(words.toList())) {
            rebuildTrie()
        }
    }

    /**
     * 허용 단어(WhiteList)를 실시간으로 추가합니다.
     */
    fun addAllowedWords(vararg words: String) {
        if (_allowWords.addAll(words.toList())) {
            allowTrieCache.clear() // 허용 목록 변경 시 캐시 초기화
        }
    }

    private val isUpdating = AtomicReference<Boolean>(false)

    /**
     * 내부 상태를 바탕으로 Trie를 다시 빌드하여 원자적으로 교체합니다.
     * 여러 스레드에서 동시에 호출하더라도 최종적으로는 모든 단어가 포함된 Trie가 반영됩니다.
     */
    @Synchronized
    private fun rebuildTrie() {
        val currentBanned = _customBannedWords.toList()
        val currentExcluded = _excludedWords.toList()
        
        val newTrie = ProfanityTrie.create(
            customWords = currentBanned,
            excludeWords = currentExcluded
        )
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
                val originalStart = mapping.indexMap[ban.start]
                val originalEnd = mapping.indexMap[ban.end]

                for (i in originalStart..originalEnd) {
                    resultChars[i] = maskChar
                }
            }
        }

        return String(resultChars)
    }

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
            _allowWords.forEach { word ->
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