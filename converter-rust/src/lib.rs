//! Rust prediction-core migration for azooKey-Android.
//!
//! The LOUDS behavior in this module is derived from:
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

use std::collections::{BTreeMap, HashMap};
use std::error::Error;
use std::fmt;
use std::fs;
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

impl fmt::Display for DictionaryError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Io(error) => write!(formatter, "dictionary I/O error: {error}"),
            Self::InvalidFormat(message) => write!(formatter, "invalid dictionary format: {message}"),
            Self::UnsupportedCharacter(character) => {
                write!(formatter, "character is not present in charID.chid: {character}")
            }
        }
    }
}

impl Error for DictionaryError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            Self::Io(error) => Some(error),
            Self::InvalidFormat(_) | Self::UnsupportedCharacter(_) => None,
        }
    }
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
pub struct Louds {
    bits: Vec<u64>,
    flat_char_to_node_indices: Vec<u32>,
    flat_char_to_node_indices_index: [u32; 256],
    rank_large: Vec<u32>,
}

impl Louds {
    pub fn from_parts(
        bits: Vec<u64>,
        node_index_to_id: Vec<u8>,
    ) -> Result<Self, DictionaryError> {
        if bits.is_empty() {
            return Err(DictionaryError::InvalidFormat("LOUDS bit array is empty"));
        }
        if node_index_to_id.len() < 2 {
            return Err(DictionaryError::InvalidFormat(
                "loudschars2 is missing the two root entries",
            ));
        }
        if node_index_to_id.len() > u32::MAX as usize {
            return Err(DictionaryError::InvalidFormat(
                "loudschars2 has too many nodes",
            ));
        }

        let mut cumulative_counts = [0usize; 256];
        for &value in &node_index_to_id {
            cumulative_counts[value as usize] += 1;
        }
        for index in 1..cumulative_counts.len() {
            cumulative_counts[index] += cumulative_counts[index - 1];
        }

        let mut counts = [0usize; 256];
        let mut flat_char_to_node_indices = vec![0u32; node_index_to_id.len()];
        for (node_index, &value) in node_index_to_id.iter().enumerate() {
            let value_index = value as usize;
            let destination = if value == 0 {
                counts[value_index]
            } else {
                cumulative_counts[value_index - 1] + counts[value_index]
            };
            flat_char_to_node_indices[destination] = node_index as u32;
            counts[value_index] += 1;
        }

        let mut flat_char_to_node_indices_index = [0u32; 256];
        for (index, &value) in cumulative_counts.iter().enumerate() {
            flat_char_to_node_indices_index[index] = value as u32;
        }

        let mut rank_large: Vec<u32> = Vec::with_capacity(bits.len() + 1);
        rank_large.push(0);
        for &word in &bits {
            let previous = *rank_large
                .last()
                .expect("rank_large always contains the zero prefix");
            let zero_count = 64u32 - word.count_ones();
            rank_large.push(previous.saturating_add(zero_count));
        }

        Ok(Self {
            bits,
            flat_char_to_node_indices,
            flat_char_to_node_indices_index,
            rank_large,
        })
    }

    pub fn from_files(
        louds_path: impl AsRef<Path>,
        chars_path: impl AsRef<Path>,
    ) -> Result<Self, DictionaryError> {
        let louds_bytes = fs::read(louds_path)?;
        if louds_bytes.is_empty() || louds_bytes.len() % 8 != 0 {
            return Err(DictionaryError::InvalidFormat(
                ".louds size must be a non-zero multiple of 8 bytes",
            ));
        }

        let bits = louds_bytes
            .chunks_exact(8)
            .map(|chunk| {
                u64::from_le_bytes(
                    chunk
                        .try_into()
                        .expect("chunks_exact(8) always yields eight bytes"),
                )
            })
            .collect();

        let node_index_to_id = fs::read(chars_path)?;
        Self::from_parts(bits, node_index_to_id)
    }

    pub fn search_node_index(&self, chars: &[u8]) -> Option<usize> {
        let mut index = 1usize;
        for &character in chars {
            index = self.search_char_node_index(index, character)?;
        }
        Some(index)
    }

    pub fn prefix_node_indices(
        &self,
        chars: &[u8],
        max_depth: usize,
        max_count: usize,
    ) -> Vec<usize> {
        let Some(node_index) = self.search_node_index(chars) else {
            return Vec::new();
        };
        self.prefix_node_indices_from_node(node_index, 0, max_depth, max_count)
    }

    fn prefix_node_indices_from_node(
        &self,
        node_index: usize,
        depth: usize,
        max_depth: usize,
        max_count: usize,
    ) -> Vec<usize> {
        let mut child_node_indices: Vec<usize> =
            self.child_node_indices(node_index).collect();

        if depth == max_depth {
            return child_node_indices;
        }

        let direct_children = child_node_indices.clone();
        for child in direct_children {
            if child_node_indices.len() > max_count {
                break;
            }

            let remaining = max_count.saturating_sub(child_node_indices.len());
            child_node_indices.extend(self.prefix_node_indices_from_node(
                child,
                depth.saturating_add(1),
                max_depth,
                remaining,
            ));
        }

        child_node_indices
    }

    fn search_char_node_index(
        &self,
        parent_node_index: usize,
        character: u8,
    ) -> Option<usize> {
        let child_node_indices = self.child_node_indices(parent_node_index);
        if child_node_indices.is_empty() {
            return None;
        }

        let character_index = character as usize;
        let group_start = if character == 0 {
            0usize
        } else {
            self.flat_char_to_node_indices_index[character_index - 1] as usize
        };
        let group_end = self.flat_char_to_node_indices_index[character_index] as usize;
        let node_indices = &self.flat_char_to_node_indices[group_start..group_end];

        let child_start = u32::try_from(child_node_indices.start).ok()?;
        let child_end = u32::try_from(child_node_indices.end).ok()?;

        let mut left = 0usize;
        let mut right = node_indices.len();
        while left < right {
            let middle = (left + right) >> 1;
            if child_start <= node_indices[middle] {
                right = middle;
            } else {
                left = middle + 1;
            }
        }

        if left < node_indices.len()
            && child_start <= node_indices[left]
            && node_indices[left] < child_end
        {
            Some(node_indices[left] as usize)
        } else {
            None
        }
    }

    fn child_node_indices(&self, parent_node_index: usize) -> Range<usize> {
        if parent_node_index == 0 || self.rank_large.len() < 2 {
            return 0..0;
        }

        let mut left = parent_node_index >> 6;
        let mut right = self.rank_large.len() - 1;
        if left > right {
            return 0..0;
        }

        while left <= right {
            let middle = (left + right) / 2;
            if self.rank_large[middle] as usize >= parent_node_index {
                if middle == 0 {
                    break;
                }
                right = middle - 1;
            } else {
                left = middle + 1;
            }
        }

        if left == self.rank_large.len() || left == 0 {
            return 0..0;
        }

        let word_index = left - 1;
        let Some(&word) = self.bits.get(word_index) else {
            return 0..0;
        };

        let zero_count_before_word = self.rank_large[word_index] as usize;
        if zero_count_before_word > parent_node_index {
            return 0..0;
        }

        let mut bit_offset = 0usize;
        for _ in 0..(parent_node_index - zero_count_before_word) {
            if bit_offset >= 64 {
                return 0..0;
            }
            let zeros = (!(word << bit_offset)).leading_zeros() as usize;
            bit_offset = bit_offset.saturating_add(zeros).saturating_add(1);
        }

        let Some(start) = (word_index << 6)
            .checked_add(bit_offset)
            .and_then(|value| value.checked_sub(parent_node_index))
            .and_then(|value| value.checked_add(1))
        else {
            return 0..0;
        };

        let end = if self.rank_large[word_index + 1] as usize == parent_node_index {
            let mut next_word_index = word_index + 1;
            while self.bits.get(next_word_index) == Some(&u64::MAX) {
                next_word_index += 1;
            }

            let Some(&next_word) = self.bits.get(next_word_index) else {
                return start..start;
            };
            let distance_to_zero = (!next_word).leading_zeros() as usize % 64;
            let Some(end) = (next_word_index << 6)
                .checked_add(distance_to_zero)
                .and_then(|value| value.checked_sub(parent_node_index))
                .and_then(|value| value.checked_add(1))
            else {
                return start..start;
            };
            end
        } else {
            if bit_offset >= 64 {
                return start..start;
            }
            let distance_to_zero =
                ((!(word << bit_offset)).leading_zeros() as usize + bit_offset) % 64;
            let Some(end) = (word_index << 6)
                .checked_add(distance_to_zero)
                .and_then(|value| value.checked_sub(parent_node_index))
                .and_then(|value| value.checked_add(1))
            else {
                return start..start;
            };
            end
        };

        if end < start {
            start..start
        } else {
            start..end
        }
    }
}

#[derive(Debug)]
pub struct PredictionDictionary {
    root: PathBuf,
    char_ids: HashMap<char, u8>,
}

impl PredictionDictionary {
    pub fn open(root: impl AsRef<Path>) -> Result<Self, DictionaryError> {
        let root = root.as_ref().to_path_buf();
        let char_id_path = root.join("louds").join("charID.chid");
        let char_id_text = fs::read_to_string(char_id_path)?;

        let mut char_ids = HashMap::new();
        for (index, character) in char_id_text.chars().enumerate() {
            let id = u8::try_from(index).map_err(|_| {
                DictionaryError::InvalidFormat("charID.chid contains more than 256 entries")
            })?;
            if char_ids.insert(character, id).is_some() {
                return Err(DictionaryError::InvalidFormat(
                    "charID.chid contains duplicate characters",
                ));
            }
        }

        Ok(Self { root, char_ids })
    }

    pub fn raw_prefix_entries(
        &self,
        key: &str,
        include_exact_match: bool,
    ) -> Result<Vec<DicdataElement>, DictionaryError> {
        if key.is_empty() {
            return Ok(Vec::new());
        }

        let mut key_characters = key.chars();
        let Some(first_character) = key_characters.next() else {
            return Ok(Vec::new());
        };

        let char_ids: Vec<u8> = key
            .chars()
            .map(|character| self.char_ids.get(&character).copied().unwrap_or(u8::MAX))
            .collect();

        let identifier = escaped_identifier(&first_character.to_string());
        let louds_root = self.root.join("louds");
        let louds_path = louds_root.join(format!("{identifier}.louds"));
        let chars_path = louds_root.join(format!("{identifier}.loudschars2"));

        let louds = match Louds::from_files(&louds_path, &chars_path) {
            Ok(louds) => louds,
            Err(DictionaryError::Io(error)) if error.kind() == io::ErrorKind::NotFound => {
                return Ok(Vec::new());
            }
            Err(error) => return Err(error),
        };

        let character_count = key.chars().count();
        let max_depth = match character_count {
            1 => 3,
            2 => 5,
            _ => usize::MAX,
        };

        let mut indices =
            louds.prefix_node_indices(&char_ids, max_depth, PREDICTION_MAX_COUNT);
        if include_exact_match && indices.len() < PREDICTION_MAX_COUNT {
            if let Some(exact_index) = louds.search_node_index(&char_ids) {
                indices.push(exact_index);
            }
        }

        indices.sort_unstable();
        indices.dedup();
        self.read_loudstxt3_entries(&identifier, &indices)
    }

    fn read_loudstxt3_entries(
        &self,
        identifier: &str,
        indices: &[usize],
    ) -> Result<Vec<DicdataElement>, DictionaryError> {
        let mut grouped_indices: BTreeMap<usize, Vec<usize>> = BTreeMap::new();
        for &index in indices {
            grouped_indices
                .entry(index >> SHARD_SHIFT)
                .or_default()
                .push(index & LOCAL_MASK);
        }

        let mut result = Vec::new();
        for (shard, local_indices) in grouped_indices {
            let path = self
                .root
                .join("louds")
                .join(format!("{identifier}{shard}.loudstxt3"));
            let bytes = match fs::read(path) {
                Ok(bytes) => bytes,
                Err(error) if error.kind() == io::ErrorKind::NotFound => continue,
                Err(error) => return Err(error.into()),
            };
            result.extend(parse_loudstxt3(&bytes, &local_indices)?);
        }
        Ok(result)
    }
}

pub fn escaped_identifier(input: &str) -> String {
    match input {
        "user" | "memory" | "user_shortcuts" => return input.to_owned(),
        _ => {}
    }

    let chunks: Vec<String> = input
        .encode_utf16()
        .map(|unit| format!("{unit:04X}"))
        .collect();
    format!("[{}]", chunks.join("_"))
}

fn parse_loudstxt3(
    bytes: &[u8],
    indices: &[usize],
) -> Result<Vec<DicdataElement>, DictionaryError> {
    let slot_count = read_u16_le(bytes, 0)? as usize;
    let header_size = 2usize
        .checked_add(
            slot_count
                .checked_mul(4)
                .ok_or(DictionaryError::InvalidFormat("loudstxt3 header overflows"))?,
        )
        .ok_or(DictionaryError::InvalidFormat("loudstxt3 header overflows"))?;
    if bytes.len() < header_size {
        return Err(DictionaryError::InvalidFormat(
            "loudstxt3 header is truncated",
        ));
    }

    let mut result = Vec::new();
    for &index in indices {
        if index >= slot_count {
            continue;
        }

        let start = read_u32_le(bytes, 2 + index * 4)? as usize;
        let end = if index + 1 == slot_count {
            bytes.len()
        } else {
            read_u32_le(bytes, 2 + (index + 1) * 4)? as usize
        };

        if start > end || end > bytes.len() || start < header_size {
            return Err(DictionaryError::InvalidFormat(
                "loudstxt3 payload offset is invalid",
            ));
        }
        result.extend(parse_loudstxt3_payload(&bytes[start..end])?);
    }

    Ok(result)
}

fn parse_loudstxt3_payload(
    payload: &[u8],
) -> Result<Vec<DicdataElement>, DictionaryError> {
    let count = read_u16_le(payload, 0)? as usize;
    if count == 0 {
        return Ok(Vec::new());
    }

    let numeric_size = count
        .checked_mul(10)
        .ok_or(DictionaryError::InvalidFormat("loudstxt3 row count overflows"))?;
    let text_offset = 2usize
        .checked_add(numeric_size)
        .ok_or(DictionaryError::InvalidFormat("loudstxt3 payload overflows"))?;
    if payload.len() < text_offset {
        return Err(DictionaryError::InvalidFormat(
            "loudstxt3 numeric rows are truncated",
        ));
    }

    let text = std::str::from_utf8(&payload[text_offset..])
        .map_err(|_| DictionaryError::InvalidFormat("loudstxt3 text is not UTF-8"))?;
    let fields: Vec<&str> = text.split('\t').collect();
    if fields.len() < count + 1 {
        return Err(DictionaryError::InvalidFormat(
            "loudstxt3 text field count does not match row count",
        ));
    }
    let ruby = fields[0].to_owned();

    let mut result = Vec::with_capacity(count);
    for row in 0..count {
        let offset = 2 + row * 10;
        let lcid = read_u16_le(payload, offset)?;
        let rcid = read_u16_le(payload, offset + 2)?;
        let mid = read_u16_le(payload, offset + 4)?;
        let value = f32::from_bits(read_u32_le(payload, offset + 6)?);
        let word_field = fields[row + 1];

        result.push(DicdataElement {
            word: if word_field.is_empty() {
                ruby.clone()
            } else {
                word_field.to_owned()
            },
            ruby: ruby.clone(),
            lcid,
            rcid,
            mid,
            value,
        });
    }

    Ok(result)
}

fn read_u16_le(bytes: &[u8], offset: usize) -> Result<u16, DictionaryError> {
    let slice = bytes
        .get(offset..offset.saturating_add(2))
        .ok_or(DictionaryError::InvalidFormat("unexpected end of u16 field"))?;
    let array: [u8; 2] = slice
        .try_into()
        .map_err(|_| DictionaryError::InvalidFormat("invalid u16 field"))?;
    Ok(u16::from_le_bytes(array))
}

fn read_u32_le(bytes: &[u8], offset: usize) -> Result<u32, DictionaryError> {
    let slice = bytes
        .get(offset..offset.saturating_add(4))
        .ok_or(DictionaryError::InvalidFormat("unexpected end of u32 field"))?;
    let array: [u8; 4] = slice
        .try_into()
        .map_err(|_| DictionaryError::InvalidFormat("invalid u32 field"))?;
    Ok(u32::from_le_bytes(array))
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
    fn loudstxt3_payload_matches_upstream_layout() {
        let mut payload = Vec::new();
        payload.extend_from_slice(&2u16.to_le_bytes());

        payload.extend_from_slice(&10u16.to_le_bytes());
        payload.extend_from_slice(&20u16.to_le_bytes());
        payload.extend_from_slice(&30u16.to_le_bytes());
        payload.extend_from_slice(&(-3.5f32).to_bits().to_le_bytes());

        payload.extend_from_slice(&11u16.to_le_bytes());
        payload.extend_from_slice(&21u16.to_le_bytes());
        payload.extend_from_slice(&31u16.to_le_bytes());
        payload.extend_from_slice(&(-4.5f32).to_bits().to_le_bytes());

        payload.extend_from_slice("カナ	仮名	".as_bytes());

        assert_eq!(
            vec![
                DicdataElement {
                    word: "仮名".to_owned(),
                    ruby: "カナ".to_owned(),
                    lcid: 10,
                    rcid: 20,
                    mid: 30,
                    value: -3.5,
                },
                DicdataElement {
                    word: "カナ".to_owned(),
                    ruby: "カナ".to_owned(),
                    lcid: 11,
                    rcid: 21,
                    mid: 31,
                    value: -4.5,
                },
            ],
            parse_loudstxt3_payload(&payload).unwrap(),
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

        bits.extend(vec![true; current.len()]);
        bits.push(false);

        while !current.is_empty() {
            let mut next = Vec::new();
            for (character, node) in current {
                node_ids.push(character);
                let children: Vec<(u8, Node)> = node.children.into_iter().collect();
                bits.extend(vec![true; children.len()]);
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
