# AI Agent Guidelines

This project uses AI agents to help manage and develop the codebase. To ensure consistency and reliability, follow these guidelines.

## 0. Mandatory Document Rules
- **READ Documentation**: Always read `.gemini/docs/STRUCTURE.md` and `.gemini/docs/DOCUMENT.md` before making any significant architectural or logic changes.
- **CONTINUOUS Updates**: Update `.gemini/docs/STRUCTURE.md` whenever the project structure, dependencies, or core conventions change.
- **APPEND-ONLY for DOCUMENT.md**: Never delete existing history in `.gemini/docs/DOCUMENT.md`. If requirements change or logic is deprecated, use strikethrough (`~~text~~`) to mark the old logic and append the new version below with an updated version tag.
- **MAINTAIN History**: Preserve the evolution of the project's core intent and operational mechanisms to avoid regressions.

## 1. Development Lifecycle
- **Research Phase**: Map the codebase and validate assumptions using `grep_search` and `glob`. Use `read_file` to inspect core logic.
- **Strategy Phase**: Formulate a grounded plan and share a concise summary.
- **Execution Phase**: Apply targeted, surgical changes. Ensure changes are idiomatically complete and follow workspace standards.
- **Validation Phase**: Run tests and workspace standards to confirm success and ensure no regressions. **Validation is mandatory.**

## 2. Engineering Standards
- **Contextual Precedence**: Instructions in `GEMINI.md` take absolute precedence over general defaults.
- **Conventions & Style**: Rigorously adhere to existing workspace conventions, architectural patterns, and style (naming, formatting, typing).
- **Security & Integrity**: Protect credentials and sensitive information. Do not stage or commit changes unless requested.
