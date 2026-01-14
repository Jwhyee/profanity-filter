package filter.policy

internal enum class ProfanityFilterRegex(val regex: String) {
    NUMBERS("[\\p{N}]"),
    WHITESPACES("[\\s]");
}