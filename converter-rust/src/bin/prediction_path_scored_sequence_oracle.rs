use azookey_prediction_rust::ranked_prediction_path_scored_json;
use serde_json::Value;

fn main() {
    let mut arguments = std::env::args().skip(1);
    let Some(dictionary_path) = arguments.next() else {
        eprintln!("usage: prediction_path_scored_sequence_oracle <dictionary-path> <paths-json> [n-best]");
        std::process::exit(64);
    };
    let Some(paths_json) = arguments.next() else {
        eprintln!("usage: prediction_path_scored_sequence_oracle <dictionary-path> <paths-json> [n-best]");
        std::process::exit(64);
    };
    let n_best = arguments
        .next()
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(3);

    let paths: Vec<Value> = match serde_json::from_str(&paths_json) {
        Ok(paths) => paths,
        Err(error) => {
            eprintln!("invalid paths JSON: {error}");
            std::process::exit(65);
        }
    };

    let results: Vec<Value> = paths
        .into_iter()
        .map(|path| {
            let result = ranked_prediction_path_scored_json(
                &path.to_string(),
                &dictionary_path,
                n_best,
            );
            serde_json::from_str(&result).unwrap_or_else(|_| Value::Array(Vec::new()))
        })
        .collect();

    print!(
        "{}",
        serde_json::to_string(&results).unwrap_or_else(|_| "[]".to_owned()),
    );
}
