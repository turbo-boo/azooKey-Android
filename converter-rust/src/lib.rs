//! Rust prediction-core migration for azooKey-Android.
//!
//! The LOUDS behavior exercised by the tests is derived from:
//! azooKey/AzooKeyKanaKanjiConverter
//! revision d59a28e4c7ca049aef04f29a91eae9677a7753f2
//!
//! Relevant upstream files:
//! - Sources/KanaKanjiConverterModule/DictionaryManagement/DataStructure/LOUDS.swift
//! - Sources/KanaKanjiConverterModule/DictionaryManagement/DataStructure/extension LOUDS.swift
//! - Sources/KanaKanjiConverterModule/DictionaryManagement/DataStructure/DictionaryBuilder.swift
//! - Sources/KanaKanjiConverterModule/DictionaryManagement/DicdataStore.swift
//!
//! Original implementation author: Miwa / Ensan
//! License: MIT

use std::collections::HashMap;
use std::io;
use std::ops::Range;
use std::path::{Path, PathBuf};

const SHARD_SHIFT: usize = 11;
const LOCAL_MASK: usize = (1 << SHARD_SHIFT) - 1;
const PREDICTION_MAX_COUNT: usize = 700;

#[derive(Debug)]
pub enum DictionaryError {
    Io(io::Error),
    InvalidFormat(&'static str),
    UnsupportedCharacter(char),
}

impl From<io::Error> for DictionaryError {
    fn from(error: io::Error) -> Self {
        Self::Io(error)
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct DicdataElement {
    pub word: String,
    pub ruby: String,
    pub lcid: u16,
    pub rcid: u16,
    pub mid: u16,
    pub value: f32,
}

#[derive(Debug)]
pub struct Louds;

impl Louds {
    pub fn from_parts(
        _bits: Vec<u64>,
        _node_index_to_id: Vec<u8>,
    ) -> Result<Self, DictionaryError> {
        todo!("implemented by the following Green commit")
    }

    pub fn search_node_index(&self, _chars: &[u8]) -> Option<usize> {
        todo!("implemented by the following Green commit")
    }

    pub fn prefix_node_indices(
        &self,
        _chars: &[u8],
        _max_depth: usize,
        _max_count: usize,
    ) -> Vec<usize> {
        todo!("implemented by the following Green commit")
    }

    #[allow(dead_code)]
    fn child_node_indices(&self, _parent_node_index: usize) -> Range<usize> {
        todo!("implemented by the following Green commit")
    }
}

#[derive(Debug)]
pub struct PredictionDictionary {
    root: PathBuf,
    char_ids: HashMap<char, u8>,
}

impl PredictionDictionary {
    pub fn open(_root: impl AsRef<Path>) -> Result<Self, DictionaryError> {
        todo!("implemented by the following Green commit")
    }

    pub fn raw_prefix_entries(
        &self,
        _key: &str,
        _include_exact_match: bool,
    ) -> Result<Vec<DicdataElement>, DictionaryError> {
        let _ = (&self.root, &self.char_ids);
        todo!("implemented by the following Green commit")
    }
}

pub fn escaped_identifier(_input: &str) -> String {
    todo!("implemented by the following Green commit")
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::BTreeMap;

    #[test]
    fn escaped_identifier_matches_upstream_utf16_encoding() {
        assert_eq!("[3042]", escaped_identifier("あ"));
        assert_eq!("[0041_0042]", escaped_identifier("AB"));
        assert_eq!("[D83C_DDEF_D83C_DDF5]", escaped_identifier("🇯🇵"));
        assert_eq!("user", escaped_identifier("user"));
    }

    #[test]
    fn louds_searches_exact_and_prefix_nodes() {
        let louds = fixture_louds(&[
            &[1],
            &[1, 2],
            &[1, 3],
            &[1, 2, 5],
            &[4],
        ]);

        let one = louds.search_node_index(&[1]).expect("node 1");
        let one_two = louds.search_node_index(&[1, 2]).expect("node 1,2");
        let one_three = louds.search_node_index(&[1, 3]).expect("node 1,3");
        let one_two_five = louds
            .search_node_index(&[1, 2, 5])
            .expect("node 1,2,5");

        assert_ne!(one, one_two);
        assert_eq!(None, louds.search_node_index(&[2]));

        let mut descendants = louds.prefix_node_indices(&[1], usize::MAX, 32);
        descendants.sort_unstable();

        let mut expected = vec![one_two, one_three, one_two_five];
        expected.sort_unstable();
        assert_eq!(expected, descendants);
    }

    #[test]
    fn louds_prefix_depth_limits_descendants() {
        let louds = fixture_louds(&[
            &[1],
            &[1, 2],
            &[1, 2, 3],
            &[1, 2, 3, 4],
        ]);

        let child = louds.search_node_index(&[1, 2]).unwrap();
        assert_eq!(
            vec![child],
            louds.prefix_node_indices(&[1], 0, 32),
        );
    }

    #[test]
    fn pinned_dictionary_exposes_azookey_entry_when_available() {
        let Ok(path) = std::env::var("AZOOKEY_DICTIONARY_PATH") else {
            return;
        };

        let dictionary = PredictionDictionary::open(path).expect("open pinned dictionary");
        let entries = dictionary
            .raw_prefix_entries("アズーキー", true)
            .expect("read prefix entries");

        assert!(
            entries.iter().any(|entry| entry.word == "azooKey"),
            "expected the pinned dictionary to contain azooKey; got {entries:?}",
        );
    }

    #[derive(Default, Clone)]
    struct Node {
        children: BTreeMap<u8, Node>,
    }

    fn fixture_louds(paths: &[&[u8]]) -> Louds {
        let mut root = Node::default();
        for path in paths {
            insert(&mut root, path);
        }

        let mut node_ids = vec![0, 0];
        let mut bits = vec![true, false];
        let mut current: Vec<(u8, Node)> = root.children.into_iter().collect();

        bits.extend(std::iter::repeat(true).take(current.len()));
        bits.push(false);

        while !current.is_empty() {
            let mut next = Vec::new();
            for (character, node) in current {
                node_ids.push(character);
                let children: Vec<(u8, Node)> = node.children.into_iter().collect();
                bits.extend(std::iter::repeat(true).take(children.len()));
                bits.push(false);
                next.extend(children);
            }
            current = next;
        }

        Louds::from_parts(pack_bits(&bits), node_ids).unwrap()
    }

    fn insert(node: &mut Node, path: &[u8]) {
        if let Some((&head, tail)) = path.split_first() {
            insert(node.children.entry(head).or_default(), tail);
        }
    }

    fn pack_bits(bits: &[bool]) -> Vec<u64> {
        let mut words = Vec::new();
        for chunk in bits.chunks(64) {
            let mut value = 0u64;
            for (index, bit) in chunk.iter().enumerate() {
                if *bit {
                    value |= 1u64 << (63 - index);
                }
            }
            for index in chunk.len()..64 {
                value |= 1u64 << (63 - index);
            }
            words.push(value);
        }
        words
    }
}
