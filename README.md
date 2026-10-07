# azooKey-Android

Android port of [azooKey](https://github.com/azooKey/azooKey).

## Current state

The first installable IME is implemented on the `feat/initial-ime` branch.

Implemented:

- Japanese flick input
- Latin flick input with upper/lower case switching
- Number/symbol flick input
- Android composing text handling
- Backspace with code-point deletion
- Space and enter
- azooKey-compatible `小ﾞﾟ` character transformation
- azooKey default `、/。/？/！` flick key
- Input-method switching key
- Kana-kanji conversion candidates backed by the pinned AzooKeyKanaKanjiConverter
- Asynchronous candidate refresh with stale-result suppression
- Candidate selection and composition replacement
- Android packaging for the Swift JNI converter and default dictionary
- Rust-backed Japanese prediction hot path with LOUDS lookup, CC/MM scoring, path-aware context reconstruction, and stable direct-input predictions
- Production candidate merging with Swift regular conversion and Rust prediction, checked against the pinned Swift converter in CI

Not implemented yet:

- User dictionary and learning
- Full azooKey theming/customization

## Development principles

- Android platform integration is written in Kotlin.
- The upstream Swift converter remains the regular kana-kanji conversion baseline and parity oracle; Japanese prediction lookup and ranking run through Rust without moving Android UI/platform code out of Kotlin.
- Features are developed test-first: Red -> Green -> Refactor.
- Upstream-derived code and behavior retain clear authorship and source attribution.

## Android requirements

The converter bridge uses Swift for Android and currently requires Android 9 / API 28 or newer.

## CI

Every branch and pull request runs:

- core unit tests and lint without a Swift Android toolchain
- host-side Swift converter tests
- Rust prediction unit tests plus single-input, multi-clause, production-merge, and sequential stable-prediction parity checks against the pinned Swift converter
- Android integration tests with the Swift JNI bridge and Rust JNI library
- app unit tests and lint
- debug APK assembly
- AAR/APK checks for JNI libraries and the bundled azooKey dictionary

A successful Android integration workflow uploads the debug APK as the `azookey-android-debug` artifact.
