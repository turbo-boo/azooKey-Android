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

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jint};
use jni::EnvUnowned;
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap, HashSet};
use std::error::Error;
use std::fmt::{self, Write};
use std::fs;
use std::io;
use std::ops::Range;
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex, OnceLock};

const SHARD_SHIFT: usize = 11;
const LOCAL_MASK: usize = (1 << SHARD_SHIFT) - 1;
const PREDICTION_MAX_COUNT: usize = 700;
const MID_COUNT: usize = 502;
const PREDICTION_UNUSABLE_RCIDS: &[u16] = &[
    13, 14, 15, 16, 17, 18, 25, 26, 27, 28, 33, 34, 40, 41, 42, 46, 47, 50,
    56, 57, 58, 59, 60, 61, 62, 63, 64, 74, 75, 76, 77, 78, 79, 86, 87, 88,
    93, 94, 95, 99, 100, 103, 107, 108, 109, 110, 111, 112, 119, 120, 121, 122, 127, 128,
    134, 135, 136, 140, 141, 144, 369, 372, 373, 377, 378, 379, 380, 381, 382, 389, 390, 391,
    392, 397, 398, 401, 402, 404, 405, 406, 408, 410, 411, 412, 413, 416, 417, 418, 419, 420,
    421, 426, 427, 431, 433, 434, 437, 438, 441, 442, 443, 447, 448, 450, 452, 455, 457, 462,
    463, 464, 470, 471, 472, 476, 477, 480, 483, 489, 490, 493, 494, 495, 496, 504, 527, 528,
    533, 534, 537, 540, 542, 548, 551, 553, 561, 562, 564, 565, 566, 567, 569, 571, 572, 574,
    575, 576, 577, 579, 581, 582, 585, 587, 589, 590, 591, 594, 595, 596, 597, 598, 600, 601,
    603, 604, 606, 609, 611, 614, 617, 618, 620, 621, 622, 624, 626, 627, 629, 630, 631, 634,
    636, 638, 641, 642, 644, 647, 648, 650, 653, 654, 656, 659, 660, 662, 665, 666, 668, 671,
    672, 673, 675, 676, 677, 678, 681, 682, 683, 684, 687, 688, 691, 692, 693, 694, 697, 698,
    699, 700, 703, 704, 707, 708, 709, 710, 713, 714, 715, 716, 721, 722, 724, 725, 727, 729,
    730, 732, 733, 736, 737, 739, 740, 742, 744, 745, 747, 748, 750, 752, 753, 755, 756, 758,
    760, 761, 763, 764, 766, 768, 769, 770, 771, 774, 775, 776, 777, 778, 779, 780, 781, 782,
    783, 786, 787, 790, 791, 793, 794, 795, 798, 800, 801, 804, 805, 806, 807, 810, 811, 814,
    815, 816, 820, 821, 822, 823, 824, 825, 829, 830, 831, 835, 837, 840, 842, 845, 847, 850,
    852, 855, 859, 860, 862, 865, 866, 868, 869, 871, 872, 873, 875, 877, 878, 880, 881, 884,
    885, 887, 888, 889, 890, 891, 893, 895, 896, 898, 899, 900, 901, 903, 905, 906, 908, 909,
    910, 911, 913, 915, 916, 917, 918, 921, 922, 923, 924, 925, 928, 929, 931, 932, 934, 935,
    936, 939, 941, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 958, 959, 960, 961, 962,
    963, 964, 965, 966, 967, 973, 974, 975, 976, 977, 983, 984, 985, 986, 987, 988, 989, 990,
    995, 996, 997, 998, 999, 1000, 1001, 1002, 1007, 1008, 1009, 1010, 1015, 1016, 1017, 1018, 1021, 1022,
    1023, 1024, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048,
    1057, 1058, 1060, 1061, 1063, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077,
    1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1104, 1105, 1106, 1107, 1108,
    1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126,
    1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144,
    1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156, 1157, 1158, 1159, 1160, 1161, 1162,
    1163, 1164, 1165, 1166, 1167, 1168, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193,
    1194, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228,
    1229, 1230, 1231, 1240, 1241, 1242, 1243, 1248, 1249, 1250, 1251, 1256, 1257, 1258, 1259, 1260, 1261, 1262,
    1263, 1268, 1269, 1270, 1271, 1276, 1278,
];


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

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct PredictionContext {
    pub last_rcid: u16,
    pub next_lcid: u16,
    pub last_mid: u16,
    pub last_value: f32,
}

impl Default for PredictionContext {
    fn default() -> Self {
        Self {
            last_rcid: 0,
            next_lcid: 0,
            last_mid: 500,
            last_value: 0.0,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize)]
pub struct RankedPrediction {
    pub word: String,
    pub ruby: String,
    pub score: f32,
}

#[derive(Debug, Deserialize)]
struct UserDictionaryEntryWire {
    reading: String,
    word: String,
}

#[derive(Debug, Deserialize)]
struct PredictionPathElementWire {
    word: String,
    ruby: String,
    lcid: u16,
    rcid: u16,
    mid: u16,
    value: f32,
}

fn prediction_path_from_json(
    json: &str,
) -> Result<Vec<DicdataElement>, DictionaryError> {
    let elements: Vec<PredictionPathElementWire> = serde_json::from_str(json)
        .map_err(|_| DictionaryError::InvalidFormat("prediction path JSON is invalid"))?;

    Ok(elements
        .into_iter()
        .map(|element| DicdataElement {
            word: element.word,
            ruby: element.ruby,
            lcid: element.lcid,
            rcid: element.rcid,
            mid: element.mid,
            value: element.value,
        })
        .collect())
}

#[derive(Debug, Clone, PartialEq)]
pub struct PredictionSeed {
    pub ruby: String,
    pub prefix_text: String,
    pub prefix_ruby: String,
    pub context: PredictionContext,
}

#[derive(Debug, Clone, PartialEq)]
struct ReconstructedClause {
    start: usize,
    end: usize,
    mid: u16,
    next_lcid: u16,
    value: f32,
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
struct CcLine {
    default_value: f32,
    overrides: HashMap<u16, f32>,
}

pub struct PredictionDictionary {
    root: PathBuf,
    char_ids: HashMap<char, u8>,
    louds_cache: Mutex<HashMap<String, Arc<Louds>>>,
    loudstxt3_cache: Mutex<HashMap<String, Arc<Vec<u8>>>>,
    cc_cache: Mutex<HashMap<u16, Option<Arc<CcLine>>>>,
    mm_cache: Mutex<Option<Arc<Vec<f32>>>>,
    dynamic_user_dictionary: Mutex<Vec<DicdataElement>>,
}

static PREDICTION_DICTIONARY_CACHE: OnceLock<
    Mutex<HashMap<PathBuf, Arc<PredictionDictionary>>>,
> = OnceLock::new();

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

        Ok(Self {
            root,
            char_ids,
            louds_cache: Mutex::new(HashMap::new()),
            loudstxt3_cache: Mutex::new(HashMap::new()),
            cc_cache: Mutex::new(HashMap::new()),
            mm_cache: Mutex::new(None),
            dynamic_user_dictionary: Mutex::new(Vec::new()),
        })
    }

    pub fn prediction_seed_from_path(
        &self,
        data: &[DicdataElement],
    ) -> Result<Option<PredictionSeed>, DictionaryError> {
        let clauses = self.reconstruct_clauses(data)?;
        let Some(last_clause) = clauses.last() else {
            return Ok(None);
        };

        let prefix_end = last_clause.start;
        let prefix_text = data[..prefix_end]
            .iter()
            .map(|item| item.word.as_str())
            .collect::<String>();
        let prefix_ruby = data[..prefix_end]
            .iter()
            .map(|item| item.ruby.as_str())
            .collect::<String>();
        let ruby = data[last_clause.start..last_clause.end]
            .iter()
            .map(|item| item.ruby.as_str())
            .collect::<String>();

        let prepart = &clauses[..clauses.len() - 1];
        let last_rcid = if prefix_end == 0 {
            0
        } else {
            data[prefix_end - 1].rcid
        };
        let next_lcid = prepart
            .last()
            .map(|clause| clause.next_lcid)
            .unwrap_or(0);
        let last_mid = prepart.last().map(|clause| clause.mid).unwrap_or(500);

        let mut mm_value = 0.0;
        let mut previous_mid = 500;
        for clause in prepart {
            mm_value += self.mm_value(previous_mid, clause.mid)?;
            previous_mid = clause.mid;
        }
        let last_value = prepart
            .last()
            .map(|clause| clause.value + mm_value)
            .unwrap_or(0.0);

        Ok(Some(PredictionSeed {
            ruby,
            prefix_text,
            prefix_ruby,
            context: PredictionContext {
                last_rcid,
                next_lcid,
                last_mid,
                last_value,
            },
        }))
    }

    fn reconstruct_clauses(
        &self,
        data: &[DicdataElement],
    ) -> Result<Vec<ReconstructedClause>, DictionaryError> {
        if data.is_empty() {
            return Ok(Vec::new());
        }

        let mut clauses: Vec<ReconstructedClause> = Vec::new();
        let mut previous_rcid = 0u16;
        let mut total_value = 0.0f32;

        for (index, item) in data.iter().enumerate() {
            total_value += self.cc_value(previous_rcid, item.lcid)? + item.value;

            let starts_new_clause = !clauses.is_empty() && is_clause(previous_rcid, item.lcid);
            if starts_new_clause {
                if let Some(previous_clause) = clauses.last_mut() {
                    previous_clause.next_lcid = item.lcid;
                }
                clauses.push(ReconstructedClause {
                    start: index,
                    end: index + 1,
                    mid: if include_mm_value_calculation(item) {
                        item.mid
                    } else {
                        500
                    },
                    next_lcid: 1316,
                    value: total_value,
                });
            } else if let Some(clause) = clauses.last_mut() {
                clause.end = index + 1;
                if (clause.mid == 500 && item.mid != 500)
                    || include_mm_value_calculation(item)
                {
                    clause.mid = item.mid;
                }
                clause.value = total_value;
            } else {
                clauses.push(ReconstructedClause {
                    start: index,
                    end: index + 1,
                    mid: if item.mid != 500 || include_mm_value_calculation(item) {
                        item.mid
                    } else {
                        500
                    },
                    next_lcid: 1316,
                    value: total_value,
                });
            }

            previous_rcid = item.rcid;
        }

        Ok(clauses)
    }

    pub fn rank_prediction_entries_for_seed(
        &self,
        seed: &PredictionSeed,
        entries: impl IntoIterator<Item = DicdataElement>,
        n_best: usize,
    ) -> Result<Vec<RankedPrediction>, DictionaryError> {
        let mut ranked = self.rank_prediction_entries(
            entries,
            seed.ruby.chars().count(),
            seed.context,
            n_best,
        )?;

        for item in &mut ranked {
            item.word = format!("{}{}", seed.prefix_text, item.word);
            item.ruby = format!("{}{}", seed.prefix_ruby, item.ruby);
        }
        Ok(ranked)
    }

    pub fn ranked_path_prediction_words(
        &self,
        data: &[DicdataElement],
        n_best: usize,
    ) -> Result<Vec<String>, DictionaryError> {
        let Some(seed) = self.prediction_seed_from_path(data)? else {
            return Ok(Vec::new());
        };
        let entries = self.raw_prefix_entries(&seed.ruby, false)?;
        let ranked = self.rank_prediction_entries_for_seed(
            &seed,
            entries,
            n_best,
        )?;

        let mut seen = HashSet::new();
        Ok(ranked
            .into_iter()
            .filter_map(|item| {
                if item.word.is_empty() || !seen.insert(item.word.clone()) {
                    None
                } else {
                    Some(item.word)
                }
            })
            .collect())
    }

    pub fn rank_prediction_entries(
        &self,
        entries: impl IntoIterator<Item = DicdataElement>,
        last_ruby_count: usize,
        context: PredictionContext,
        n_best: usize,
    ) -> Result<Vec<RankedPrediction>, DictionaryError> {
        if n_best == 0 {
            return Ok(Vec::new());
        }

        let ignore_cc_value = self.cc_value(context.last_rcid, context.next_lcid)?;
        let mut result: Vec<RankedPrediction> = Vec::with_capacity(n_best);

        for data in entries.into_iter().filter(|data| prediction_usable(data.rcid)) {
            let mm_value = if include_mm_value_calculation(&data) {
                self.mm_value(context.last_mid, data.mid)?
            } else {
                0.0
            };
            let cc_value = self.cc_value(context.last_rcid, data.lcid)?;
            let ruby_extension = data
                .ruby
                .chars()
                .count()
                .saturating_sub(last_ruby_count) as f32;
            let score = context.last_value
                + mm_value
                + cc_value
                + data.value
                - ruby_extension
                - ignore_cc_value;

            let insertion_index = result
                .iter()
                .rposition(|item| item.score >= score)
                .map_or(0, |index| index + 1);
            if insertion_index >= n_best {
                continue;
            }

            if result.len() >= n_best {
                result.pop();
            }
            result.insert(
                insertion_index,
                RankedPrediction {
                    word: data.word,
                    ruby: data.ruby,
                    score,
                },
            );
        }

        Ok(result)
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
        let mut result = if let Some(louds) = self.load_louds(&identifier)? {
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
            self.read_loudstxt3_entries(&identifier, &indices)?
        } else {
            Vec::new()
        };

        let dynamic_entries = self
            .dynamic_user_dictionary
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
            .iter()
            .filter(|entry| entry.ruby.starts_with(key))
            .cloned()
            .collect::<Vec<_>>();
        result.extend(dynamic_entries);
        Ok(result)
    }

    fn load_louds(
        &self,
        identifier: &str,
    ) -> Result<Option<Arc<Louds>>, DictionaryError> {
        if let Some(cached) = self
            .louds_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
            .get(identifier)
            .cloned()
        {
            return Ok(Some(cached));
        }

        let louds_root = self.root.join("louds");
        let louds_path = louds_root.join(format!("{identifier}.louds"));
        let chars_path = louds_root.join(format!("{identifier}.loudschars2"));
        let loaded = match Louds::from_files(louds_path, chars_path) {
            Ok(louds) => Arc::new(louds),
            Err(DictionaryError::Io(error)) if error.kind() == io::ErrorKind::NotFound => {
                return Ok(None);
            }
            Err(error) => return Err(error),
        };

        let mut cache = self
            .louds_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());
        Ok(Some(
            cache
                .entry(identifier.to_owned())
                .or_insert_with(|| loaded.clone())
                .clone(),
        ))
    }

    pub fn cc_value(
        &self,
        former: u16,
        latter: u16,
    ) -> Result<f32, DictionaryError> {
        let Some(line) = self.load_cc_line(former)? else {
            return Ok(-25.0);
        };
        Ok(line
            .overrides
            .get(&latter)
            .copied()
            .unwrap_or(line.default_value))
    }

    fn load_cc_line(
        &self,
        former: u16,
    ) -> Result<Option<Arc<CcLine>>, DictionaryError> {
        if let Some(cached) = self
            .cc_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
            .get(&former)
            .cloned()
        {
            return Ok(cached);
        }

        let path = self.root.join("cb").join(format!("{former}.binary"));
        let bytes = match fs::read(path) {
            Ok(bytes) => bytes,
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                self.cc_cache
                    .lock()
                    .unwrap_or_else(|poisoned| poisoned.into_inner())
                    .insert(former, None);
                return Ok(None);
            }
            Err(error) => return Err(error.into()),
        };

        if bytes.is_empty() {
            self.cc_cache
                .lock()
                .unwrap_or_else(|poisoned| poisoned.into_inner())
                .insert(former, None);
            return Ok(None);
        }
        if bytes.len() % 8 != 0 {
            return Err(DictionaryError::InvalidFormat(
                "CC binary size must be a multiple of 8 bytes",
            ));
        }

        let first_key = read_i32_le(&bytes, 0)?;
        if first_key != -1 {
            return Err(DictionaryError::InvalidFormat(
                "CC binary must begin with the -1 default entry",
            ));
        }
        let default_value = f32::from_bits(read_u32_le(&bytes, 4)?);
        let mut overrides = HashMap::new();

        for offset in (8..bytes.len()).step_by(8) {
            let key = read_i32_le(&bytes, offset)?;
            if !(0..=1318).contains(&key) {
                return Err(DictionaryError::InvalidFormat(
                    "CC binary contains an out-of-range class id",
                ));
            }
            let value = f32::from_bits(read_u32_le(&bytes, offset + 4)?);
            overrides.insert(key as u16, value);
        }

        let loaded = Arc::new(CcLine {
            default_value,
            overrides,
        });
        let mut cache = self
            .cc_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());
        Ok(Some(
            cache
                .entry(former)
                .or_insert_with(|| Some(loaded.clone()))
                .as_ref()
                .expect("new CC cache entry contains a line")
                .clone(),
        ))
    }

    pub fn mm_value(
        &self,
        former: u16,
        latter: u16,
    ) -> Result<f32, DictionaryError> {
        if former == 500 || latter == 500 {
            return Ok(0.0);
        }
        let former = usize::from(former);
        let latter = usize::from(latter);
        if former >= MID_COUNT || latter >= MID_COUNT {
            return Err(DictionaryError::InvalidFormat(
                "MM lookup contains an out-of-range meaning id",
            ));
        }

        let values = self.load_mm_values()?;
        Ok(values[former * MID_COUNT + latter])
    }

    fn load_mm_values(&self) -> Result<Arc<Vec<f32>>, DictionaryError> {
        if let Some(cached) = self
            .mm_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
            .clone()
        {
            return Ok(cached);
        }

        let expected_count = MID_COUNT * MID_COUNT;
        let path = self.root.join("mm.binary");
        let loaded = match fs::read(path) {
            Ok(bytes) => {
                if bytes.len() != expected_count * 4 {
                    return Err(DictionaryError::InvalidFormat(
                        "mm.binary must contain a 502 by 502 Float32 matrix",
                    ));
                }

                let mut values = Vec::with_capacity(expected_count);
                for chunk in bytes.chunks_exact(4) {
                    let bits = u32::from_le_bytes(
                        chunk
                            .try_into()
                            .expect("chunks_exact(4) always yields four bytes"),
                    );
                    values.push(f32::from_bits(bits));
                }
                Arc::new(values)
            }
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                Arc::new(vec![0.0; expected_count])
            }
            Err(error) => return Err(error.into()),
        };

        let mut cache = self
            .mm_cache
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());
        Ok(cache.get_or_insert_with(|| loaded.clone()).clone())
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
            let file_id = format!("{identifier}{shard}");
            let cached = {
                self.loudstxt3_cache
                    .lock()
                    .unwrap_or_else(|poisoned| poisoned.into_inner())
                    .get(&file_id)
                    .cloned()
            };
            let bytes = if let Some(cached) = cached {
                cached
            } else {
                let path = self
                    .root
                    .join("louds")
                    .join(format!("{file_id}.loudstxt3"));
                let loaded = match fs::read(path) {
                    Ok(bytes) => Arc::new(bytes),
                    Err(error) if error.kind() == io::ErrorKind::NotFound => continue,
                    Err(error) => return Err(error.into()),
                };
                let mut cache = self
                    .loudstxt3_cache
                    .lock()
                    .unwrap_or_else(|poisoned| poisoned.into_inner());
                cache
                    .entry(file_id)
                    .or_insert_with(|| loaded.clone())
                    .clone()
            };
            result.extend(parse_loudstxt3(bytes.as_slice(), &local_indices)?);
        }
        Ok(result)
    }
}

fn word_type(cid: u16) -> u8 {
    if cid == 0 || cid == 1316 {
        return 3;
    }
    if matches!(cid, 1315 | 6 | 557..=560) {
        return 0;
    }
    if matches!(
        cid,
        561..=867
            | 1283..=1296
            | 1306..=1309
            | 11..=52
            | 555..=556
            | 1281..=1282
            | 1314
            | 3
            | 2
            | 4
            | 5
            | 1
            | 9
    ) {
        return 1;
    }
    2
}

fn is_clause(former: u16, latter: u16) -> bool {
    let latter_word_type = word_type(latter);
    if latter_word_type == 3 {
        return false;
    }

    let former_word_type = word_type(former);
    if former_word_type == 3 {
        return false;
    }

    match latter_word_type {
        0 | 1 => former_word_type != 0,
        _ => false,
    }
}

fn include_mm_value_calculation(data: &DicdataElement) -> bool {
    if (895..=1280).contains(&data.lcid) || (895..=1280).contains(&data.rcid) {
        return true;
    }
    if (1297..=1305).contains(&data.lcid) || (1297..=1305).contains(&data.rcid) {
        return true;
    }
    word_type(data.lcid) == 1 || word_type(data.rcid) == 1
}

fn prediction_usable(rcid: u16) -> bool {
    PREDICTION_UNUSABLE_RCIDS.binary_search(&rcid).is_err()
}

fn cached_prediction_dictionary(
    root: impl AsRef<Path>,
) -> Result<Arc<PredictionDictionary>, DictionaryError> {
    let root = root.as_ref().to_path_buf();
    let cache = PREDICTION_DICTIONARY_CACHE.get_or_init(|| Mutex::new(HashMap::new()));

    if let Some(dictionary) = cache
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
        .get(&root)
        .cloned()
    {
        return Ok(dictionary);
    }

    let opened = Arc::new(PredictionDictionary::open(&root)?);
    let mut guard = cache
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    Ok(guard
        .entry(root)
        .or_insert_with(|| opened.clone())
        .clone())
}

fn user_dictionary_from_json(
    json: &str,
) -> Result<Vec<DicdataElement>, DictionaryError> {
    let entries: Vec<UserDictionaryEntryWire> = serde_json::from_str(json)
        .map_err(|_| DictionaryError::InvalidFormat("user dictionary JSON is invalid"))?;

    Ok(entries
        .into_iter()
        .filter_map(|entry| {
            let reading = dictionary_reading(entry.reading.trim());
            let word = entry.word.trim().to_owned();
            if reading.is_empty() || word.is_empty() {
                return None;
            }
            Some(DicdataElement {
                word,
                ruby: reading,
                lcid: 1288,
                rcid: 1288,
                mid: 501,
                value: -10.0,
            })
        })
        .collect())
}

pub fn replace_user_dictionary_json(
    dictionary_path: impl AsRef<Path>,
    json: &str,
) -> Result<(), DictionaryError> {
    let entries = user_dictionary_from_json(json)?;
    let dictionary = cached_prediction_dictionary(dictionary_path)?;
    let mut dynamic_user_dictionary = dictionary
        .dynamic_user_dictionary
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    *dynamic_user_dictionary = entries;
    Ok(())
}

pub fn dictionary_reading(input: &str) -> String {
    input
        .chars()
        .map(|character| {
            let codepoint = character as u32;
            if (0x3041..=0x3096).contains(&codepoint) {
                char::from_u32(codepoint + 0x60).unwrap_or(character)
            } else {
                character
            }
        })
        .collect()
}

pub fn raw_prefix_words(
    input: &str,
    dictionary_path: impl AsRef<Path>,
) -> Result<Vec<String>, DictionaryError> {
    if input.is_empty() {
        return Ok(Vec::new());
    }

    let dictionary = cached_prediction_dictionary(dictionary_path)?;
    let reading = dictionary_reading(input);
    let entries = dictionary.raw_prefix_entries(&reading, true)?;

    let mut seen = HashSet::new();
    Ok(entries
        .into_iter()
        .filter(|entry| prediction_usable(entry.rcid))
        .filter_map(|entry| {
            if entry.word.is_empty() || !seen.insert(entry.word.clone()) {
                None
            } else {
                Some(entry.word)
            }
        })
        .collect())
}

pub fn ranked_prefix_words(
    input: &str,
    dictionary_path: impl AsRef<Path>,
    n_best: usize,
) -> Result<Vec<String>, DictionaryError> {
    ranked_prefix_words_with_context(
        input,
        dictionary_path,
        n_best,
        PredictionContext::default(),
    )
}

pub fn ranked_prefix_words_with_context(
    input: &str,
    dictionary_path: impl AsRef<Path>,
    n_best: usize,
    context: PredictionContext,
) -> Result<Vec<String>, DictionaryError> {
    if input.is_empty() || n_best == 0 {
        return Ok(Vec::new());
    }

    let dictionary = cached_prediction_dictionary(dictionary_path)?;
    let reading = dictionary_reading(input);
    let entries = dictionary.raw_prefix_entries(&reading, false)?;
    let ranked = dictionary.rank_prediction_entries(
        entries,
        reading.chars().count(),
        context,
        n_best,
    )?;

    let mut seen = HashSet::new();
    Ok(ranked
        .into_iter()
        .filter_map(|item| {
            if item.word.is_empty() || !seen.insert(item.word.clone()) {
                None
            } else {
                Some(item.word)
            }
        })
        .collect())
}

fn words_json(words: &[String]) -> String {
    let mut output = String::from("[");
    for (index, word) in words.iter().enumerate() {
        if index != 0 {
            output.push(',');
        }
        output.push('"');
        for character in word.chars() {
            match character {
                '"' => output.push_str("\\\""),
                '\\' => output.push_str("\\\\"),
                '\n' => output.push_str("\\n"),
                '\r' => output.push_str("\\r"),
                '\t' => output.push_str("\\t"),
                '\u{0008}' => output.push_str("\\b"),
                '\u{000C}' => output.push_str("\\f"),
                control if control <= '\u{001F}' => {
                    let _ = write!(output, "\\u{:04X}", control as u32);
                }
                other => output.push(other),
            }
        }
        output.push('"');
    }
    output.push(']');
    output
}

pub fn ranked_prefix_words_json(
    input: &str,
    dictionary_path: impl AsRef<Path>,
    n_best: usize,
) -> String {
    match ranked_prefix_words(input, dictionary_path, n_best) {
        Ok(words) => words_json(&words),
        Err(_) => "[]".to_owned(),
    }
}

pub fn ranked_prediction_path_scored_json(
    path_json: &str,
    dictionary_path: impl AsRef<Path>,
    n_best: usize,
) -> String {
    let result = prediction_path_from_json(path_json).and_then(|path| {
        let dictionary = cached_prediction_dictionary(dictionary_path)?;
        let Some(seed) = dictionary.prediction_seed_from_path(&path)? else {
            return Ok(Vec::new());
        };
        let entries = dictionary.raw_prefix_entries(&seed.ruby, false)?;
        dictionary.rank_prediction_entries_for_seed(&seed, entries, n_best)
    });

    match result {
        Ok(items) => serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_owned()),
        Err(_) => "[]".to_owned(),
    }
}

pub fn ranked_prediction_path_json(
    path_json: &str,
    dictionary_path: impl AsRef<Path>,
    n_best: usize,
) -> String {
    let result = prediction_path_from_json(path_json).and_then(|path| {
        let dictionary = cached_prediction_dictionary(dictionary_path)?;
        dictionary.ranked_path_prediction_words(&path, n_best)
    });

    match result {
        Ok(words) => words_json(&words),
        Err(_) => "[]".to_owned(),
    }
}

fn raw_prefix_words_json(input: &str, dictionary_path: &str) -> String {
    ranked_prefix_words_json(input, dictionary_path, 10)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_turboboo_azookey_ime_RustPredictionBridge_prefixWordsJson<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    input: JString<'caller>,
    dictionary_path: JString<'caller>,
) -> JString<'caller> {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let input: String = input.to_string();
        let dictionary_path: String = dictionary_path.to_string();
        JString::from_str(
            env,
            raw_prefix_words_json(&input, &dictionary_path),
        )
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_turboboo_azookey_ime_RustPredictionBridge_prefixWordsWithContextJson<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    input: JString<'caller>,
    dictionary_path: JString<'caller>,
    last_rcid: jint,
    next_lcid: jint,
    last_mid: jint,
    last_value: jfloat,
    n_best: jint,
) -> JString<'caller> {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let input: String = input.to_string();
        let dictionary_path: String = dictionary_path.to_string();

        let json = match (
            u16::try_from(last_rcid),
            u16::try_from(next_lcid),
            u16::try_from(last_mid),
            usize::try_from(n_best),
        ) {
            (Ok(last_rcid), Ok(next_lcid), Ok(last_mid), Ok(n_best)) => {
                match ranked_prefix_words_with_context(
                    &input,
                    &dictionary_path,
                    n_best,
                    PredictionContext {
                        last_rcid,
                        next_lcid,
                        last_mid,
                        last_value,
                    },
                ) {
                    Ok(words) => words_json(&words),
                    Err(_) => "[]".to_owned(),
                }
            }
            _ => "[]".to_owned(),
        };

        JString::from_str(env, json)
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_turboboo_azookey_ime_RustPredictionBridge_prefixWordsFromPathJson<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    path_json: JString<'caller>,
    dictionary_path: JString<'caller>,
    n_best: jint,
) -> JString<'caller> {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let path_json: String = path_json.to_string();
        let dictionary_path: String = dictionary_path.to_string();
        let json = match usize::try_from(n_best) {
            Ok(n_best) => ranked_prediction_path_json(
                &path_json,
                &dictionary_path,
                n_best,
            ),
            Err(_) => "[]".to_owned(),
        };
        JString::from_str(env, json)
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_turboboo_azookey_ime_RustPredictionBridge_prefixScoredFromPathJson<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    path_json: JString<'caller>,
    dictionary_path: JString<'caller>,
    n_best: jint,
) -> JString<'caller> {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let path_json: String = path_json.to_string();
        let dictionary_path: String = dictionary_path.to_string();
        let json = match usize::try_from(n_best) {
            Ok(n_best) => ranked_prediction_path_scored_json(
                &path_json,
                &dictionary_path,
                n_best,
            ),
            Err(_) => "[]".to_owned(),
        };
        JString::from_str(env, json)
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_turboboo_azookey_ime_RustPredictionBridge_replaceUserDictionaryJson<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    dictionary_path: JString<'caller>,
    json: JString<'caller>,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<jboolean, jni::errors::Error> {
        let dictionary_path: String = dictionary_path.to_string();
        let json: String = json.to_string();
        Ok(replace_user_dictionary_json(&dictionary_path, &json).is_ok())
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
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

fn read_i32_le(bytes: &[u8], offset: usize) -> Result<i32, DictionaryError> {
    let slice = bytes
        .get(offset..offset.saturating_add(4))
        .ok_or(DictionaryError::InvalidFormat("unexpected end of i32 field"))?;
    let array: [u8; 4] = slice
        .try_into()
        .map_err(|_| DictionaryError::InvalidFormat("invalid i32 field"))?;
    Ok(i32::from_le_bytes(array))
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
    use std::sync::atomic::{AtomicU64, Ordering};

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
    fn pinned_dictionary_matches_upstream_must_word_when_available() {
        let Ok(path) = std::env::var("AZOOKEY_DICTIONARY_PATH") else {
            return;
        };

        let dictionary = PredictionDictionary::open(path).expect("open pinned dictionary");
        let entries = dictionary
            .raw_prefix_entries("アサッテ", true)
            .expect("read prefix entries");

        assert!(
            entries.iter().any(|entry| entry.word == "明後日"),
            "expected the pinned dictionary to contain アサッテ -> 明後日; got {entries:?}",
        );
    }

    #[test]
    fn dictionary_reading_normalizes_hiragana_to_katakana() {
        assert_eq!("アサッテ", dictionary_reading("あさって"));
        assert_eq!("ヴァ", dictionary_reading("ゔぁ"));
        assert_eq!("azooKeyー", dictionary_reading("azooKeyー"));
    }

    #[test]
    fn words_json_uses_json_string_escaping() {
        assert_eq!(
            r#"["仮名","quote\\\"","line\\nbreak"]"#,
            words_json(&[
                "仮名".to_owned(),
                "quote\\\"".to_owned(),
                "line\\nbreak".to_owned(),
            ]),
        );
    }

    #[test]
    fn prediction_usable_matches_upstream_terminal_filter_examples() {
        for blocked in [33u16, 15, 372, 420, 25, 27, 404, 13, 373] {
            assert!(!prediction_usable(blocked), "rcid {blocked} must be filtered");
        }
        for allowed in [0u16, 1, 32, 1319] {
            assert!(prediction_usable(allowed), "rcid {allowed} must remain usable");
        }
    }

    #[test]
    fn cc_binary_uses_default_and_sparse_overrides() {
        let root = temporary_dictionary_root("cc");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::create_dir_all(root.join("cb")).unwrap();
        fs::write(root.join("louds/charID.chid"), "ア").unwrap();

        let mut bytes = Vec::new();
        bytes.extend_from_slice(&(-1i32).to_le_bytes());
        bytes.extend_from_slice(&(-5.0f32).to_le_bytes());
        bytes.extend_from_slice(&(7i32).to_le_bytes());
        bytes.extend_from_slice(&(-1.5f32).to_le_bytes());
        fs::write(root.join("cb/0.binary"), bytes).unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        assert_eq!(-1.5, dictionary.cc_value(0, 7).unwrap());
        assert_eq!(-5.0, dictionary.cc_value(0, 8).unwrap());
        assert_eq!(-25.0, dictionary.cc_value(1, 8).unwrap());

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn mm_binary_reads_dense_values_and_mid_500_is_neutral() {
        let root = temporary_dictionary_root("mm");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "ア").unwrap();

        let mut values = vec![0.0f32; MID_COUNT * MID_COUNT];
        values[2 * MID_COUNT + 3] = -2.75;
        let mut bytes = Vec::with_capacity(values.len() * 4);
        for value in values {
            bytes.extend_from_slice(&value.to_le_bytes());
        }
        fs::write(root.join("mm.binary"), bytes).unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        assert_eq!(-2.75, dictionary.mm_value(2, 3).unwrap());
        assert_eq!(0.0, dictionary.mm_value(500, 3).unwrap());
        assert_eq!(0.0, dictionary.mm_value(2, 500).unwrap());

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn missing_mm_binary_matches_upstream_zero_fallback() {
        let root = temporary_dictionary_root("mm-missing");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "ア").unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        assert_eq!(0.0, dictionary.mm_value(12, 34).unwrap());

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn include_mm_matches_upstream_word_type_rules() {
        let content = DicdataElement {
            word: "内容".to_owned(),
            ruby: "ナイヨウ".to_owned(),
            lcid: 1285,
            rcid: 1285,
            mid: 1,
            value: 0.0,
        };
        let function_word = DicdataElement {
            lcid: 147,
            rcid: 147,
            ..content.clone()
        };
        let dependent_verb = DicdataElement {
            lcid: 900,
            rcid: 900,
            ..content.clone()
        };

        assert!(include_mm_value_calculation(&content));
        assert!(!include_mm_value_calculation(&function_word));
        assert!(include_mm_value_calculation(&dependent_verb));
    }

    #[test]
    fn clause_boundary_matches_upstream_word_type_rules() {
        assert!(!is_clause(0, 1285));
        assert!(!is_clause(1285, 1316));
        assert!(!is_clause(1315, 1285));
        assert!(!is_clause(1285, 7));
        assert!(is_clause(7, 1285));
        assert!(is_clause(1285, 1285));
        assert!(is_clause(1285, 1315));
    }

    #[test]
    fn prediction_path_json_decodes_swift_wire_format() {
        let path = prediction_path_from_json(
            r#"[
                {"word":"私","ruby":"ワタシ","lcid":1285,"rcid":1285,"mid":1,"value":-2.0},
                {"word":"は","ruby":"ハ","lcid":261,"rcid":261,"mid":500,"value":-1.0}
            ]"#,
        )
        .unwrap();

        assert_eq!(
            vec![
                DicdataElement {
                    word: "私".to_owned(),
                    ruby: "ワタシ".to_owned(),
                    lcid: 1285,
                    rcid: 1285,
                    mid: 1,
                    value: -2.0,
                },
                DicdataElement {
                    word: "は".to_owned(),
                    ruby: "ハ".to_owned(),
                    lcid: 261,
                    rcid: 261,
                    mid: 500,
                    value: -1.0,
                },
            ],
            path,
        );
    }

    #[test]
    fn prediction_path_json_rejects_out_of_range_ids() {
        assert!(prediction_path_from_json(
            r#"[{"word":"x","ruby":"エックス","lcid":70000,"rcid":1,"mid":1,"value":-1.0}]"#,
        )
        .is_err());
    }

    #[test]
    fn prediction_seed_reconstructs_last_clause_context() {
        let root = temporary_dictionary_root("prediction-seed");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "ワタシハガッコウ").unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        let path = vec![
            DicdataElement {
                word: "私".to_owned(),
                ruby: "ワタシ".to_owned(),
                lcid: 1285,
                rcid: 1285,
                mid: 1,
                value: -2.0,
            },
            DicdataElement {
                word: "は".to_owned(),
                ruby: "ハ".to_owned(),
                lcid: 261,
                rcid: 261,
                mid: 500,
                value: -1.0,
            },
            DicdataElement {
                word: "学校".to_owned(),
                ruby: "ガッコウ".to_owned(),
                lcid: 1285,
                rcid: 1285,
                mid: 2,
                value: -3.0,
            },
        ];

        let seed = dictionary
            .prediction_seed_from_path(&path)
            .unwrap()
            .expect("prediction seed");

        assert_eq!("ガッコウ", seed.ruby);
        assert_eq!("私は", seed.prefix_text);
        assert_eq!("ワタシハ", seed.prefix_ruby);
        assert_eq!(
            PredictionContext {
                last_rcid: 261,
                next_lcid: 1285,
                last_mid: 1,
                last_value: -53.0,
            },
            seed.context,
        );

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn prediction_seed_ranking_preserves_prefix_text() {
        let root = temporary_dictionary_root("prediction-seed-ranking");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "ガッコウヘ").unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        let seed = PredictionSeed {
            ruby: "ガッコウ".to_owned(),
            prefix_text: "私は".to_owned(),
            prefix_ruby: "ワタシハ".to_owned(),
            context: PredictionContext {
                last_rcid: 261,
                next_lcid: 1285,
                last_mid: 1,
                last_value: -53.0,
            },
        };
        let entries = vec![
            DicdataElement {
                word: "学校".to_owned(),
                ruby: "ガッコウ".to_owned(),
                lcid: 1285,
                rcid: 1285,
                mid: 2,
                value: -3.0,
            },
            DicdataElement {
                word: "学校へ".to_owned(),
                ruby: "ガッコウヘ".to_owned(),
                lcid: 1285,
                rcid: 1285,
                mid: 2,
                value: -2.0,
            },
        ];

        let ranked = dictionary
            .rank_prediction_entries_for_seed(&seed, entries, 5)
            .unwrap();

        assert_eq!(
            vec!["私は学校".to_owned(), "私は学校へ".to_owned()],
            ranked.iter().map(|item| item.word.clone()).collect::<Vec<_>>(),
        );
        assert_eq!("ワタシハガッコウ", ranked[0].ruby);
        assert_eq!("ワタシハガッコウヘ", ranked[1].ruby);
        assert_eq!(-56.0, ranked[0].score);
        assert_eq!(-56.0, ranked[1].score);

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn prediction_ranking_matches_upstream_formula_and_stable_ties() {
        let root = temporary_dictionary_root("ranking");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::create_dir_all(root.join("cb")).unwrap();
        fs::write(root.join("louds/charID.chid"), "アイウエ").unwrap();

        let mut cc = Vec::new();
        cc.extend_from_slice(&(-1i32).to_le_bytes());
        cc.extend_from_slice(&(-5.0f32).to_le_bytes());
        cc.extend_from_slice(&(7i32).to_le_bytes());
        cc.extend_from_slice(&(-1.0f32).to_le_bytes());
        cc.extend_from_slice(&(8i32).to_le_bytes());
        cc.extend_from_slice(&(-3.0f32).to_le_bytes());
        fs::write(root.join("cb/0.binary"), cc).unwrap();

        let dictionary = PredictionDictionary::open(&root).unwrap();
        let entries = vec![
            DicdataElement {
                word: "first".to_owned(),
                ruby: "アイウ".to_owned(),
                lcid: 7,
                rcid: 1,
                mid: 1,
                value: -4.0,
            },
            DicdataElement {
                word: "second".to_owned(),
                ruby: "アイウエ".to_owned(),
                lcid: 8,
                rcid: 1,
                mid: 1,
                value: -1.0,
            },
            DicdataElement {
                word: "blocked".to_owned(),
                ruby: "アイウ".to_owned(),
                lcid: 7,
                rcid: 33,
                mid: 1,
                value: 100.0,
            },
        ];

        let ranked = dictionary
            .rank_prediction_entries(
                entries,
                2,
                PredictionContext::default(),
                10,
            )
            .unwrap();

        assert_eq!(
            vec!["first".to_owned(), "second".to_owned()],
            ranked.iter().map(|item| item.word.clone()).collect::<Vec<_>>(),
        );
        assert_eq!(-1.0, ranked[0].score);
        assert_eq!(-1.0, ranked[1].score);

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn dictionary_cache_reuses_open_dictionary() {
        let root = temporary_dictionary_root("cache");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "アサッテ").unwrap();

        let first = cached_prediction_dictionary(&root).unwrap();
        let second = cached_prediction_dictionary(&root).unwrap();

        assert!(Arc::ptr_eq(&first, &second));
        fs::remove_dir_all(root).unwrap();
    }

    fn temporary_dictionary_root(label: &str) -> PathBuf {
        static NEXT_ID: AtomicU64 = AtomicU64::new(0);
        let id = NEXT_ID.fetch_add(1, Ordering::Relaxed);
        std::env::temp_dir().join(format!(
            "azookey-rust-{label}-{}-{id}",
            std::process::id(),
        ))
    }

    #[test]
    fn dynamic_user_dictionary_prefix_entries_match_upstream_behavior() {
        let root = temporary_dictionary_root("dynamic-user-dictionary");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "カスタムヘン").unwrap();

        replace_user_dictionary_json(
            &root,
            r#"[{"reading":"かすたむへんかん","word":"カスタム変換"}]"#,
        )
        .unwrap();

        let dictionary = cached_prediction_dictionary(&root).unwrap();
        let entries = dictionary
            .raw_prefix_entries("カスタム", false)
            .unwrap();

        assert!(entries.iter().any(|entry| {
            entry.word == "カスタム変換"
                && entry.ruby == "カスタムヘンカン"
                && entry.lcid == 1288
                && entry.rcid == 1288
                && entry.mid == 501
                && entry.value == -10.0
        }));

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn replacing_dynamic_user_dictionary_removes_old_entries() {
        let root = temporary_dictionary_root("replace-user-dictionary");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::write(root.join("louds/charID.chid"), "アイウエ").unwrap();

        replace_user_dictionary_json(
            &root,
            r#"[{"reading":"あい","word":"旧候補"}]"#,
        )
        .unwrap();
        replace_user_dictionary_json(
            &root,
            r#"[{"reading":"あい","word":"新候補"}]"#,
        )
        .unwrap();

        let dictionary = cached_prediction_dictionary(&root).unwrap();
        let entries = dictionary.raw_prefix_entries("アイ", false).unwrap();
        assert!(!entries.iter().any(|entry| entry.word == "旧候補"));
        assert!(entries.iter().any(|entry| entry.word == "新候補"));

        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn learned_memory_prefix_entries_are_loaded_without_static_terminal_filter() {
        let base = temporary_dictionary_root("learning-memory");
        let root = base.join("dictionary");
        let memory = base.join("learning-memory");
        fs::create_dir_all(root.join("louds")).unwrap();
        fs::create_dir_all(&memory).unwrap();
        fs::write(root.join("louds/charID.chid"), "アイ").unwrap();

        write_memory_fixture(
            &memory,
            &[0, 1],
            DicdataElement {
                word: "学習候補".to_owned(),
                ruby: "アイ".to_owned(),
                lcid: 7,
                rcid: 33,
                mid: 1,
                value: -1.0,
            },
        );

        let dictionary = PredictionDictionary::open(&root).unwrap();
        let entries = dictionary.raw_prefix_entries("ア", false).unwrap();

        assert!(
            entries.iter().any(|entry| entry.word == "学習候補" && entry.rcid == 33),
            "learned memory must bypass the static dictionary terminal filter: {entries:?}",
        );

        fs::remove_dir_all(base).unwrap();
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
