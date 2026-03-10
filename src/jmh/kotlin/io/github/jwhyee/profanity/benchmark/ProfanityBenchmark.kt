package io.github.jwhyee.profanity.benchmark

import io.github.jwhyee.profanity.helper.ProfanityTrie
import io.github.jwhyee.profanity.policy.ProfanityPolicy
import io.github.jwhyee.profanity.validator.ProfanityDetectedException
import io.github.jwhyee.profanity.validator.ProfanityValidator
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class ProfanityBenchmark {

    private lateinit var words: List<String>
    private lateinit var text: String
    private lateinit var validator: ProfanityValidator
    private lateinit var simpleRegex: Regex
    private lateinit var complexRegex: Regex

    @Setup
    fun setup() {
        words = ProfanityPolicy.DEFAULT_PROFANITY_LIST.map { it.word }
        text = generateTestText(100_000)

        // 1. Library Setup
        val trie = ProfanityTrie.create()
        validator = ProfanityValidator(trie)

        // 2. Simple Regex Setup
        simpleRegex = Regex(words.joinToString("|") { Regex.escape(it) })

        // 3. Complex Regex Setup
        val complexPattern = words.joinToString("|") { word ->
            word.toCharArray().joinToString("[\\d\\s]*") { Regex.escape(it.toString()) }
        }
        complexRegex = Regex(complexPattern)
    }

    @Benchmark
    fun benchmarkLibrary() {
        try {
            validator.validate(text)
        } catch (e: ProfanityDetectedException) {
            // Expected
        }
    }

    @Benchmark
    fun benchmarkSimpleRegex() {
        simpleRegex.findAll(text).count()
    }

    @Benchmark
    fun benchmarkContainsLoop() {
        words.filter { text.contains(it) }
    }

    @Benchmark
    fun benchmarkComplexRegex() {
        complexRegex.findAll(text).count()
    }

    private fun generateTestText(length: Int): String {
        val sb = StringBuilder(length)
        val safeWords = listOf("hello", "world", "kotlin", "java", "programming", "computer", "algorithm", "performance", "test", "benchmark")
        val profanity = ProfanityPolicy.DEFAULT_PROFANITY_LIST.map { it.word }
        
        val random = Random(42) // Fixed seed for reproducibility
        while (sb.length < length) {
            if (random.nextDouble() < 0.05) { // 5% chance of profanity
                val word = profanity.random(random)
                if (random.nextDouble() < 0.5) {
                    sb.append(word.toCharArray().joinToString(random.nextInt(10).toString()))
                } else {
                    sb.append(word)
                }
            } else {
                sb.append(safeWords.random(random))
            }
            sb.append(" ")
        }
        return sb.toString()
    }
}
