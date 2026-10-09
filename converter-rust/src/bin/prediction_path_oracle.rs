use azookey_prediction_rust::ranked_prediction_path_json;

fn main() {
    let mut arguments = std::env::args().skip(1);
    let Some(dictionary_path) = arguments.next() else {
        eprintln!("usage: prediction_path_oracle <dictionary-path> <path-json> [n-best]");
        std::process::exit(64);
    };
    let Some(path_json) = arguments.next() else {
        eprintln!("usage: prediction_path_oracle <dictionary-path> <path-json> [n-best]");
        std::process::exit(64);
    };
    let n_best = arguments
        .next()
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(3);

    print!(
        "{}",
        ranked_prediction_path_json(&path_json, dictionary_path, n_best),
    );
}
