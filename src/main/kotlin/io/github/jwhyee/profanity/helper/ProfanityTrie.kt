package io.github.jwhyee.profanity.helper

import io.github.jwhyee.profanity.dto.Profanity
import io.github.jwhyee.profanity.policy.ProfanityPolicy
import org.ahocorasick.trie.PayloadTrie

/**
 * 비속어 필터링을 위한 [PayloadTrie] 생성기입니다.
 */
object ProfanityTrie {

    /**
     * 기본 비속어 목록과 사용자 정의 목록을 병합하여 [PayloadTrie]를 생성합니다.
     *
     * @param customWords 기본 목록 외에 추가로 차단할 단어 리스트
     * @param excludeWords 기본 목록에서 허용(제외)할 단어 리스트
     * @return 설정이 반영된 [PayloadTrie] 인스턴스
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        customWords: List<String> = emptyList(),
        excludeWords: List<String> = emptyList()
    ): PayloadTrie<Profanity> {
        val builder = PayloadTrie.builder<Profanity>()
        val excludeSet = excludeWords.toSet()

        // 1. 기본 목록 처리 (중간 리스트 생성을 피하기 위해 Sequence 사용)
        ProfanityPolicy.DEFAULT_PROFANITY_LIST
            .asSequence()
            .filter { it.word !in excludeSet }
            .forEach { builder.addKeyword(it.word, it) }

        // 2. 사용자 정의 목록 처리
        customWords
            .asSequence()
            .filter { it !in excludeSet }
            .distinct() // 중복 입력 방지
            .forEach { word ->
                builder.addKeyword(word, Profanity(word))
            }

        return builder.build()
    }
}