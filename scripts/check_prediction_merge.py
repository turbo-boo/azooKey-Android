#!/usr/bin/env python3
import json
import sys


def merge_candidates(base, predictions):
    combined = []
    positions = {}

    for item in base[:5] + [
        {"text": prediction["word"], "value": prediction["score"], "exactRuby": False}
        for prediction in predictions
    ]:
        text = item["text"]
        if not text:
            continue

        if text in positions:
            index = positions[text]
            if combined[index]["value"] < item["value"]:
                combined[index] = item
        else:
            positions[text] = len(combined)
            combined.append(item)

    combined = sorted(combined, key=lambda item: item["value"], reverse=True)[:5]

    if not any(item.get("exactRuby", False) for item in combined[:3]):
        exact = next((item for item in base if item.get("exactRuby", False)), None)
        if exact is not None:
            combined = [item for item in combined if item["text"] != exact["text"]]
            combined.insert(min(2, len(combined)), exact)
            combined = combined[:5]

    return [item["text"] for item in combined]


def main():
    if len(sys.argv) != 5:
        raise SystemExit(
            "usage: check_prediction_merge.py <input> <auto-json> <bridge-json> <rust-json>"
        )

    input_text, auto_raw, bridge_raw, rust_raw = sys.argv[1:5]
    expected = json.loads(auto_raw)[:5]
    bridge = json.loads(bridge_raw)
    predictions = json.loads(rust_raw)
    actual = merge_candidates(bridge["candidates"], predictions)

    if actual != expected:
        raise AssertionError(
            f"production merge parity mismatch for {input_text!r}: "
            f"swift={expected!r} rust-mix={actual!r}"
        )


if __name__ == "__main__":
    main()
