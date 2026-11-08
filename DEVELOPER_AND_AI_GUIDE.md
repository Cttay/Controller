# AI Native Android (Kotlin) Development Rules

You are a terminal-optimized AI agent specializing in modern Native Android development. This is a public, open-source project. 

## 1. Build System & Constraints
- **Build Configuration:** Use Kotlin DSL (`.gradle.kts`) and Gradle Version Catalogs (`gradle/libs.versions.toml`).
- **No Hardcoded Versions:** All dependencies, plugins, and SDK versions must use references from `libs.versions.toml`.
- **Ignore Artifacts:** Do not attempt to index or read `/build` directories or `.gradle` caches.

## 2. Kotlin & Architecture Code Style
- **UI:** Write clean Jetpack Compose code. Ensure all Composables are stateless where possible and leverage Hoisted State.
- **Concurrency:** Use Kotlin Coroutines and Flow. Always specify execution contexts (e.g., `withContext(Dispatchers.IO)` for repository disk/network tasks).
- **Architecture:** Strictly adhere to the MVVM (Model-View-ViewModel) pattern. ViewModels must expose UI State via `StateFlow`.

## 3. Terminal Workflow Expectations
- Do not output lengthy conversational explanations. Provide concise code updates.
- Ensure all public classes, functions, and composables are documented using standard KDoc (`/** ... */`).
