import AzooKeyAndroidConverter

let input = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : ""
let json = AzooKeyAndroidConverter.predictionCandidatesJSON(input)
let diagnostics = AzooKeyAndroidConverter.predictionDiagnosticsJSON(input)
print("AZOOKEY_PREDICTION_JSON=\(json)")
print("AZOOKEY_PREDICTION_DIAGNOSTICS_JSON=\(diagnostics)")
