# Project Knowledge Base

## OVERVIEW
`profanity-filter` is a high-performance Kotlin JVM library for detecting and masking profanity. It utilizes the Aho-Corasick algorithm to ensure $O(N)$ search complexity regardless of the dictionary size. Key features include real-time dictionary updates via Atomic Swap, defense against common bypass techniques (e.g., "ㅅ1발", "시 발"), and intelligent handling of whitelisted words to prevent false positives (e.g., "시발점"). It is inspired by the profanity detection strategy used by Woowahan Brothers (Baedal Minjok).

## STRUCTURE
```text
/
├── .gemini/
│   └── docs/
│       ├── STRUCTURE.md
│       └── DOCUMENT.md
├── GEMINI.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── src/
│   ├── main/
│   │   ├── kotlin/io/github/jwhyee/profanity/
│   │   │   ├── dto/
│   │   │   │   └── Profanity.kt
│   │   │   ├── helper/
│   │   │   │   └── ProfanityTrie.kt
│   │   │   ├── policy/
│   │   │   │   ├── ProfanityFilterRegex.kt
│   │   │   │   └── ProfanityPolicy.kt
│   │   │   └── validator/
│   │   │       ├── ProfanityDetectedException.kt
│   │   │       └── ProfanityValidator.kt
│   │   └── resources/
│   └── test/
│       ├── kotlin/io/github/jwhyee/profanity/
│       │   ├── policy/
│       │   │   └── PerformanceBenchmark.kt
│       │   └── validator/
│       │       ├── MemoryLimitTest.kt
│       │       └── ProfanityValidatorTest.kt
│       └── resources/
└── doc/
    ├── BENCHMARK.md
    ├── QUICK_START.md
    └── TEST.md
```

## WHERE TO LOOK
| Task / Workflow | File Path |
| :--- | :--- |
| **Validate text for profanity** | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityValidator.kt` (validate method) |
| **Mask profanity in text** | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityValidator.kt` (filter method) |
| **Bypass defense (Normalization)** | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityValidator.kt` (applyPoliciesWithMapping) |
| **Update banned words at runtime** | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityValidator.kt` (addBannedWords/removeBannedWords) |
| **Default profanity list** | `src/main/kotlin/io/github/jwhyee/profanity/policy/ProfanityPolicy.kt` |
| **Trie construction logic** | `src/main/kotlin/io/github/jwhyee/profanity/helper/ProfanityTrie.kt` |

## CODE MAP
| Symbol | Type | Location |
| :--- | :--- | :--- |
| `ProfanityValidator` | Class | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityValidator.kt` |
| `ProfanityTrie` | Object | `src/main/kotlin/io/github/jwhyee/profanity/helper/ProfanityTrie.kt` |
| `ProfanityPolicy` | Object | `src/main/kotlin/io/github/jwhyee/profanity/policy/ProfanityPolicy.kt` |
| `ProfanityFilterRegex` | Enum | `src/main/kotlin/io/github/jwhyee/profanity/policy/ProfanityFilterRegex.kt` |
| `Profanity` | Data Class | `src/main/kotlin/io/github/jwhyee/profanity/dto/Profanity.kt` |
| `ProfanityDetectedException`| Class | `src/main/kotlin/io/github/jwhyee/profanity/validator/ProfanityDetectedException.kt` |

## CONVENTIONS (THIS PROJECT)
- **Kotlin-First**: Modern Kotlin (JVM 21) with `AtomicReference`, `ConcurrentHashMap`, and `CopyOnWriteArraySet` for thread-safety.
- **Performance-Oriented**: Aho-Corasick algorithm for $O(N)$ string matching.
- **Normalization Strategy**: Normalize text (removing numbers/whitespaces) before matching, keeping an index map to restore original indices for masking.
- **Immutable/Atomic Updates**: The `ProfanityValidator` uses Atomic Swap for runtime dictionary changes, ensuring no lock contention during reads.
- **Minimal Dependencies**: Relies primarily on `ahocorasick` and Kotlin standard libraries.

## ANTI-PATTERNS / TECH DEBT
- **Synchronized Rebuilds**: `rebuildTrie()` in `ProfanityValidator` is `@Synchronized`. If updates are extremely frequent, this could cause thread contention, though it is mitigated by the use of `AtomicReference`.
- **Memory Overhead**: Whitelist filtering uses `allowTrieCache` (ConcurrentHashMap). Very large combinations of policies could increase memory usage.

## COMMANDS
| Goal | Command |
| :--- | :--- |
| **Build project** | `./gradlew build` |
| **Run tests** | `./gradlew test` |
| **Run Performance Benchmark** | `./gradlew test --tests "io.github.jwhyee.profanity.policy.PerformanceBenchmark" --info` |
| **Publish to Maven Local** | `./gradlew publishToMavenLocal` |

## NOTES
- The library is designed to handle common bypass patterns by normalizing the input text based on configurable policies.
- The `IndexMapping` logic is crucial for accurate masking, as it allows the engine to point back to original characters even after normalization.
