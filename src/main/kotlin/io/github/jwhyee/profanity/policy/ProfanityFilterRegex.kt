package io.github.jwhyee.profanity.policy

internal enum class ProfanityFilterRegex(val regex: String) {
    NUMBERS("[\\p{N}]"),
    WHITESPACES("[\\s]");
}