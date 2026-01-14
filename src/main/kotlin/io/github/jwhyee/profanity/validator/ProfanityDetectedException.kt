package io.github.jwhyee.profanity.validator

class ProfanityDetectedException(
    detected: List<String>
) : RuntimeException("금칙어가 포함되어 있습니다: $detected")