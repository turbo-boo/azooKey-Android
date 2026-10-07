use azookey_prediction_rust::ranked_prefix_words_json;

fn main() {
    let mut arguments = std::env::args().skip(1);
    let Some(dictionary_path) = arguments.next() else {
        eprintln!("usage: prediction_oracle <dictionary-path> <input> [n-best]");
        std::process::exit(64);
    };
    let Some(input) = arguments.next() else {
        eprintln!("usage: prediction_oracle <dictionary-path> <input> [n-best]");
        std::process::exit(64);
    };
    let n_best = arguments
        .next()
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(5);

    print!(
        "{}",
        ranked_prefix_words_json(&input, dictionary_path, n_best),
    );
}
