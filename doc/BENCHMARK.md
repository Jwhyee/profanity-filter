# Performance Benchmark

This document details the performance benchmark comparing the `profanity-filter` library against standard approaches like Regex and String.contains.

## Goal
To demonstrate that the Aho-Corasick based library is significantly faster and more robust than Regex-based solutions, especially when handling variant detection (numbers, whitespace).

## Methodology

The benchmark measures the execution time of four different approaches on a generated text sample.

### Test Configuration
- **Text Length**: 100,000 characters (approx. 15,000 words).
- **Content**: Random safe words mixed with profanity (5% density).
- **Variants**: 50% of profanity instances are obfuscated with numbers or spaces (e.g., "sh 1 t").
- **Iterations**: 20 iterations (after 5 warmup runs).
- **Environment**: JVM (OpenJDK 21+).

### Competitors

1.  **Library (ProfanityValidator)**:
    - Uses Aho-Corasick algorithm (O(n)).
    - **Includes** normalization (removing numbers/spaces).
    - **Includes** whitelist checking logic.
    - **Includes** exception generation overhead (on detection).

2.  **Simple Regex**:
    - Pattern: `(word1|word2|...)`
    - **Misses** variants (e.g., "sh 1 t").
    - Standard Java `Regex` engine.

3.  **Contains Loop**:
    - Naive loop: `words.filter { text.contains(it) }`
    - **Misses** variants.
    - Generally the fastest for exact matching but functionally incomplete.

4.  **Complex Regex**:
    - Pattern: `(w[\d\s]*o[\d\s]*r[\d\s]*d)`
    - **Catches** variants (functionally similar to Library).
    - Extremely complex pattern, expected to be slow.

## Results

*Sample run on Apple M1 Pro (OpenJDK 21):*

```text
Benchmark Configuration:
- Text Length: 100001 chars
- Word Count: 117
- Complex Regex Pattern Length: 2513
--------------------------------------------------
Results (Average per iteration):
1. Library (Full Feature):       3.15 ms
2. Simple Regex (No Variants):  77.64 ms
3. Contains Loop (No Variants):  1.79 ms
4. Complex Regex (With Variants): 85.89 ms
--------------------------------------------------
```

## Analysis

1.  **Speed**: The Library (~3.15ms) is nearly as fast as the naive `contains` loop (~1.79ms) and **~25x faster** than Regex (~77ms).
2.  **Functionality**:
    - The `contains` loop and `Simple Regex` **fail** to detect the 50% of profanity that is obfuscated (variants).
    - The `Complex Regex` detects variants but is **~27x slower** than the library.
3.  **Efficiency**: The Aho-Corasick algorithm maintains O(n) complexity regardless of the number of keywords, whereas Regex performance degrades with pattern complexity and text length.

## Conclusion

The `profanity-filter` library provides **Variant Detection** (like Complex Regex) at **High Speed** (comparable to naive search). It is the superior choice for high-throughput applications requiring robust filtering.

## How to Run

Run the benchmark test using Gradle:

```bash
./gradlew test --tests "io.github.jwhyee.profanity.policy.PerformanceBenchmark" --info
```
