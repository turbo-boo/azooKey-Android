# azooKey-Android

Android port of [azooKey](https://github.com/azooKey/azooKey).

## Current state

The first installable IME skeleton is implemented on the `feat/initial-ime` branch.

Implemented:

- Japanese flick input
- Android composing text handling
- Backspace with code-point deletion
- Space and enter
- azooKey-compatible `小ﾞﾟ` character transformation
- azooKey default `、/。/？/！` flick key
- Input-method switching key
- Empty candidate area reserved for the conversion engine

Not implemented yet:

- Kana-kanji conversion and prediction
- Rust prediction acceleration
- Number/symbol and Latin keyboard tabs
- User dictionary and learning
- Full azooKey theming/customization

## Development principles

- Android platform integration is written in Kotlin.
- Prediction acceleration in Rust is planned as a later milestone.
- Features are developed test-first: Red -> Green -> Refactor.
- Upstream-derived code and behavior retain clear authorship and source attribution.

## CI

Every branch and pull request runs:

- core unit tests
- app unit tests
- debug APK build
- Android lint

A successful workflow uploads the debug APK as the `azookey-android-debug` artifact.
