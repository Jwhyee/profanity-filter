package io.github.jwhyee.profanity.policy

import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.github.jwhyee.profanity.validator.ProfanityDetectedException
import io.github.jwhyee.profanity.validator.ProfanityValidator
import io.kotest.core.spec.style.StringSpec
import kotlin.time.measureTime

class PerformanceBenchmark : StringSpec({

    "benchmark performance" {
        // Setup
        val words = ProfanityPolicy.DEFAULT_PROFANITY_LIST.map { it.word }
        val text = generateTestText(100_000) // 100k chars

        // 1. Library Setup
        val trie = ProfanityTrie.create()
        val validator = ProfanityValidator(trie)

        // 2. Simple Regex Setup
        val simpleRegex = Regex(words.joinToString("|") { Regex.escape(it) })

        // 3. Complex Regex Setup (Variant support)
        // Insert [\d\s]* between characters to mimic normalization behavior
        val complexPattern = words.joinToString("|") { word ->
            word.toCharArray().joinToString("[\\d\\s]*") { Regex.escape(it.toString()) }
        }
        val complexRegex = Regex(complexPattern)

        println("Benchmark Configuration:")
        println("- Text Length: ${text.length} chars")
        println("- Word Count: ${words.size}")
        println("- Complex Regex Pattern Length: ${complexPattern.length}")
        println("--------------------------------------------------")

        // Warmup
        println("Warming up...")
        repeat(5) {
            runLibrary(validator, text)
            runRegex(simpleRegex, text)
            runContains(words, text)
            // Skip complex regex warmup to save time, or do just 1
            runRegex(complexRegex, text)
        }

        // Benchmark
        val iterations = 20
        println("Running Benchmark ($iterations iterations)...")

        val libraryTime = measureTime {
            repeat(iterations) { runLibrary(validator, text) }
        } / iterations

        val simpleRegexTime = measureTime {
            repeat(iterations) { runRegex(simpleRegex, text) }
        } / iterations

        val containsTime = measureTime {
            repeat(iterations) { runContains(words, text) }
        } / iterations

        // Complex regex is significantly slower, run fewer iterations
        val complexIterations = 5
        val complexRegexTime = measureTime {
             repeat(complexIterations) { runRegex(complexRegex, text) }
        } / complexIterations

        println("--------------------------------------------------")
        println("Results (Average per iteration):")
        println("1. Library (Full Feature): ${libraryTime}")
        println("2. Simple Regex (No Variants): ${simpleRegexTime}")
        println("3. Contains Loop (No Variants): ${containsTime}")
        println("4. Complex Regex (With Variants): ${complexRegexTime}")
        println("--------------------------------------------------")
    }
})

private fun runLibrary(validator: ProfanityValidator, text: String) {
    try {
        validator.validate(text)
    } catch (e: ProfanityDetectedException) {
        // Expected
    }
}

private fun runRegex(regex: Regex, text: String) {
    regex.findAll(text).count()
}

private fun runContains(words: List<String>, text: String) {
    words.filter { text.contains(it) }
}

private fun generateTestText(length: Int): String {
    val sb = StringBuilder(length)
    val safeWords = listOf("hello", "world", "kotlin", "java", "programming", "computer", "algorithm", "performance", "test", "benchmark")
    val profanity = ProfanityPolicy.DEFAULT_PROFANITY_LIST.map { it.word }
    
    while (sb.length < length) {
        if (Math.random() < 0.05) { // 5% chance of profanity
            val word = profanity.random()
            // Add some variants occasionally
            if (Math.random() < 0.5) {
                // Insert random number or space between characters
                sb.append(word.toCharArray().joinToString((0..9).random().toString()))
            } else {
                sb.append(word)
            }
        } else {
            sb.append(safeWords.random())
        }
        sb.append(" ")
    }
    return sb.toString()
}
