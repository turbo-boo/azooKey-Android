package dev.turboboo.azookey.ime;

final class RustPredictionBridge {
    static {
        System.loadLibrary("azookey_prediction_rust");
    }

    private RustPredictionBridge() {}

    static native String prefixWordsJson(String input, String dictionaryPath);

    static native String prefixWordsFromPathJson(
        String pathJson,
        String dictionaryPath,
        int nBest
    );

    static native String prefixScoredFromPathJson(
        String pathJson,
        String dictionaryPath,
        int nBest
    );

    static native String prefixWordsWithContextJson(
        String input,
        String dictionaryPath,
        int lastRcid,
        int nextLcid,
        int lastMid,
        float lastValue,
        int nBest
    );
}
