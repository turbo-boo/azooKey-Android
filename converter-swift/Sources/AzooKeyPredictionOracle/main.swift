import AzooKeyAndroidConverter

let input = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : ""
let json = AzooKeyAndroidConverter.predictionCandidatesJSON(input)
let diagnostics = AzooKeyAndroidConverter.predictionDiagnosticsJSON(input)
print("AZOOKEY_PREDICTION_JSON=\(json)")
print("AZOOKEY_PREDICTION_DIAGNOSTICS_JSON=\(diagnostics)")

let autoMix = AzooKeyAndroidConverter.candidatesJSON(input)
let conversionBridge = AzooKeyAndroidConverter.conversionBridgeJSON(input)
print("AZOOKEY_AUTO_MIX_JSON=\(autoMix)")
print("AZOOKEY_CONVERSION_BRIDGE_JSON=\(conversionBridge)")

let sequenceInputs = Array(CommandLine.arguments.dropFirst())
if sequenceInputs.count > 1 {
    let sequence = AzooKeyAndroidConverter.sequentialPredictionParityJSON(sequenceInputs)
    print("AZOOKEY_SEQUENTIAL_PARITY_JSON=\(sequence)")
}
