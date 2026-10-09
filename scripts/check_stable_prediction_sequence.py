#!/usr/bin/env python3
import json
import sys


def dictionary_reading(text):
    return "".join(
        chr(ord(ch) + 0x60) if 0x3041 <= ord(ch) <= 0x3096 else ch
        for ch in text
    )


def unique_scored(candidates):
    result = []
    positions = {}
    for candidate in candidates:
        text = candidate.get("text", "")
        if not text:
            continue
        if text in positions:
            index = positions[text]
            if result[index]["value"] < candidate["value"]:
                result[index] = candidate
        else:
            positions[text] = len(result)
            result.append(candidate)
    return result


def merge_stable(stable, fresh, limit=3):
    stable = unique_scored(stable)
    if len(stable) >= limit:
        return stable[:limit]
    seen = {candidate["text"] for candidate in stable}
    additional = [
        candidate for candidate in unique_scored(fresh)
        if candidate["text"] not in seen
    ]
    additional.sort(key=lambda item: item["value"], reverse=True)
    return stable + additional[: limit - len(stable)]


def merge_hybrid(swift, predictions, stable, limit=10):
    top = {}
    order = []
    for candidate in swift[:5] + predictions:
        text = candidate.get("text", "")
        if not text:
            continue
        if text not in top:
            order.append(text)
            top[text] = candidate
        elif top[text]["value"] < candidate["value"]:
            top[text] = candidate

    score_merged = [top[text] for text in order]
    score_merged.sort(key=lambda item: item["value"], reverse=True)

    stable_unique = unique_scored(stable)
    stable_texts = {candidate["text"] for candidate in stable_unique}
    mixed = (
        stable_unique +
        [candidate for candidate in score_merged if candidate["text"] not in stable_texts]
    )[:5]

    if not any(candidate.get("exactRuby", False) for candidate in mixed[:3]):
        exact = next((candidate for candidate in swift if candidate.get("exactRuby", False)), None)
        if exact is not None:
            mixed = [candidate for candidate in mixed if candidate["text"] != exact["text"]]
            mixed.insert(min(2, len(mixed)), exact)
            mixed = mixed[:5]

    result = []
    seen = set()
    for candidate in mixed + swift[5:]:
        text = candidate["text"]
        if text and text not in seen and len(result) < limit:
            seen.add(text)
            result.append(text)
    return result


def main():
    if len(sys.argv) != 3:
        raise SystemExit(
            "usage: check_stable_prediction_sequence.py <swift-sequence-json> <rust-fresh-json>"
        )

    rows = json.loads(sys.argv[1])
    fresh_sequence = json.loads(sys.argv[2])
    if len(rows) != len(fresh_sequence):
        raise AssertionError("Swift and Rust sequence lengths differ")

    cache_reading = None
    cache_candidates = []

    for row, fresh_raw in zip(rows, fresh_sequence):
        reading = dictionary_reading(row["input"])
        stable = []
        if cache_reading is not None:
            if reading.startswith(cache_reading):
                stable = [
                    candidate for candidate in cache_candidates
                    if candidate.get("ruby")
                    and candidate["ruby"] != reading
                    and candidate["ruby"].startswith(reading)
                ]
            else:
                cache_reading = None
                cache_candidates = []

        fresh = [
            {
                "text": candidate["word"],
                "value": candidate["score"],
                "ruby": candidate.get("ruby", ""),
                "exactRuby": False,
            }
            for candidate in fresh_raw
        ]
        predictions = merge_stable(stable, fresh, limit=3)
        cache_reading = reading if predictions else None
        cache_candidates = predictions

        actual = merge_hybrid(
            row["bridge"]["candidates"],
            predictions,
            stable,
        )[:5]
        expected = row["autoMix"][:5]
        if actual != expected:
            raise AssertionError(
                f"stable prediction parity mismatch for {row['input']!r}: "
                f"swift={expected!r} rust-mix={actual!r} stable={stable!r}"
            )


if __name__ == "__main__":
    main()
