package dev.turboboo.azookey.ime;

final class RustPredictionBridge {
    static {
        System.loadLibrary("azookey_prediction_rust");
    }

    private RustPredictionBridge() {}

    static native String prefixWordsJson(String input, String dictionaryPath);
}
