# Project Planning

## Core Intent
The primary intent of the `profanity-filter` project is to provide a high-performance, resilient profanity filtering library for Kotlin/Java applications. It aims to solve the performance bottlenecks of regex-based filters and the functional limitations of simple loop-based checks.

## Core Logic & Mechanisms
The project operates through a multi-stage pipeline:
1.  **Normalization**: The input sentence is processed to remove characters defined in `ProfanityFilterRegex` (currently NUMBERS and WHITESPACES). Crucially, an `indexMap` (IntArray) is maintained to link each character in the normalized string back to its original position in the input string.
2.  **Aho-Corasick Trie Search**: The normalized string is searched against a `PayloadTrie` containing banned words. This ensures that the search complexity remains $O(N)$ where $N$ is the length of the string, regardless of the number of banned words.
3.  **Whitelist (Allowed Words) Processing**: A separate trie of allowed words is searched against the normalized string. If a detected banned word overlaps with an allowed word, it is marked for exclusion.
4.  **Overlap Resolution**: The engine checks if detected banned word ranges are covered by any allowed word ranges. If so, they are not treated as profanity.
5.  **Index Restoration and Action**:
    - For **Validation**: If any non-excluded banned words remain, a `ProfanityDetectedException` is thrown.
    - For **Filtering**: The `indexMap` is used to identify the exact character ranges in the original string that correspond to the detected banned words, and those characters are replaced with a mask character (e.g., `*`).
6.  **Concurrency Support**: The `ProfanityValidator` uses an `AtomicReference` to store its main trie. When banned words are added or removed, a new trie is built in the background and swapped atomically, allowing for thread-safe runtime updates without interrupting read operations.

## Versioning
- `v1.0.0 - 2026-03-10`: Initial project analysis and documentation.
