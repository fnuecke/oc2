use std::collections::{HashMap, HashSet};
use std::sync::LazyLock;

use anyhow::{Context, Result, bail};
use serde_json::Value;

use crate::resources::{ResourceLocation, Resources};

pub const NORMALS: [(&str, [f64; 3]); 6] = [
    ("down", [0.0, -1.0, 0.0]),
    ("up", [0.0, 1.0, 0.0]),
    ("north", [0.0, 0.0, -1.0]),
    ("south", [0.0, 0.0, 1.0]),
    ("west", [-1.0, 0.0, 0.0]),
    ("east", [1.0, 0.0, 0.0]),
];

pub fn normal(direction: &str) -> [f64; 3] {
    NORMALS
        .iter()
        .find(|(name, _)| *name == direction)
        .expect("direction")
        .1
}

const MIN: usize = 0;
const MAX: usize = 1;
const FACE_VERTICES: [(&str, [[usize; 3]; 4]); 6] = [
    (
        "down",
        [
            [MIN, MIN, MAX],
            [MIN, MIN, MIN],
            [MAX, MIN, MIN],
            [MAX, MIN, MAX],
        ],
    ),
    (
        "up",
        [
            [MIN, MAX, MIN],
            [MIN, MAX, MAX],
            [MAX, MAX, MAX],
            [MAX, MAX, MIN],
        ],
    ),
    (
        "north",
        [
            [MAX, MAX, MIN],
            [MAX, MIN, MIN],
            [MIN, MIN, MIN],
            [MIN, MAX, MIN],
        ],
    ),
    (
        "south",
        [
            [MIN, MAX, MAX],
            [MIN, MIN, MAX],
            [MAX, MIN, MAX],
            [MAX, MAX, MAX],
        ],
    ),
    (
        "west",
        [
            [MIN, MAX, MIN],
            [MIN, MIN, MIN],
            [MIN, MIN, MAX],
            [MIN, MAX, MAX],
        ],
    ),
    (
        "east",
        [
            [MAX, MAX, MAX],
            [MAX, MIN, MAX],
            [MAX, MIN, MIN],
            [MAX, MAX, MIN],
        ],
    ),
];

static RESCALE_22_5: LazyLock<f64> =
    LazyLock::new(|| 1.0 / (std::f64::consts::PI / 8.0).cos() - 1.0);
static RESCALE_45: LazyLock<f64> = LazyLock::new(|| 1.0 / (std::f64::consts::PI / 4.0).cos() - 1.0);

fn face_vertices(direction: &str) -> [[usize; 3]; 4] {
    FACE_VERTICES
        .iter()
        .find(|(name, _)| *name == direction)
        .expect("direction")
        .1
}

#[derive(Clone, Debug)]
pub struct Quad {
    pub positions: [[f64; 3]; 4],
    pub uvs: [[f64; 2]; 4],
    pub texture: String,
    pub normal: [f64; 3],
}

pub struct Model {
    pub textures: HashMap<String, String>,
    pub elements: Option<Vec<Value>>,
    pub display: HashMap<String, Value>,
    pub gui_light: String,
}

impl Model {
    pub fn texture(&self, name: &str) -> Option<&str> {
        let mut name = name.strip_prefix('#').unwrap_or(name).to_owned();
        let mut seen = HashSet::new();
        while seen.insert(name.clone()) {
            let resolved = self.textures.get(&name)?;
            if !resolved.starts_with('#') {
                return self.textures.get(&name).map(String::as_str);
            }
            name = resolved[1..].to_owned();
        }
        None
    }
}

pub fn load_model(resources: &Resources, location: &ResourceLocation) -> Result<Model> {
    let mut chain: Vec<Value> = Vec::new();
    let mut seen = HashSet::new();
    let mut current = Some(location.clone());
    while let Some(location) = current {
        if location.path.starts_with("builtin/") {
            break;
        }
        if !seen.insert(location.to_string()) {
            bail!("model parent cycle at {location}");
        }
        let definition = resources.read_json(&ResourceLocation::new(
            &location.namespace,
            format!("models/{}.json", location.path),
        ))?;
        current = definition["parent"].as_str().map(ResourceLocation::parse);
        chain.push(definition);
    }

    let mut textures = HashMap::new();
    let mut display = HashMap::new();
    let mut elements = None;
    let mut gui_light = String::new();
    for definition in chain.iter().rev() {
        if let Some(entries) = definition["textures"].as_object() {
            for (key, value) in entries {
                textures.insert(key.clone(), value.as_str().unwrap_or_default().to_owned());
            }
        }
        if let Some(entries) = definition["display"].as_object() {
            for (slot, transform) in entries {
                display.insert(slot.clone(), transform.clone());
            }
        }
        if let Some(entries) = definition["elements"].as_array() {
            elements = Some(entries.clone());
        }
        if let Some(value) = definition["gui_light"].as_str() {
            gui_light = value.to_owned();
        }
    }

    if gui_light.is_empty() {
        gui_light = "side".to_owned();
    }
    Ok(Model {
        textures,
        elements,
        display,
        gui_light,
    })
}

pub fn is_generated_item(model: &Model) -> bool {
    model.elements.is_none() && model.textures.contains_key("layer0")
}

pub fn bake(model: &Model) -> Result<Vec<Quad>> {
    let mut quads = Vec::new();
    for element in model.elements.iter().flatten() {
        let source = read_vec3(&element["from"])?;
        let target = read_vec3(&element["to"])?;
        let bounds = [
            [source[0], target[0]],
            [source[1], target[1]],
            [source[2], target[2]],
        ];
        let rotation = element.get("rotation");
        let faces = element["faces"]
            .as_object()
            .context("element without faces")?;
        let mut faces: Vec<_> = faces.iter().collect();
        faces.sort_by_key(|(direction, _)| *direction);
        for (direction, face) in faces {
            let reference = face["texture"].as_str().context("face without texture")?;
            let texture = model
                .texture(reference)
                .with_context(|| format!("unresolved texture {reference:?} in model"))?
                .to_owned();
            if face["tintindex"].as_i64().unwrap_or(-1) >= 0 {
                bail!("tinted element faces are not supported");
            }
            let uv = match face.get("uv") {
                Some(value) => read_vec4(value)?,
                None => default_uv(direction, &source, &target),
            };
            let uv_rotation = face["rotation"].as_f64().unwrap_or(0.0) as i64;

            let mut positions = [[0.0; 3]; 4];
            for (index, corner) in face_vertices(direction).iter().enumerate() {
                let point = [
                    bounds[0][corner[0]] / 16.0,
                    bounds[1][corner[1]] / 16.0,
                    bounds[2][corner[2]] / 16.0,
                ];
                positions[index] = rotate_element(point, rotation)?;
            }
            let mut uvs = [[0.0; 2]; 4];
            for (index, slot) in uvs.iter_mut().enumerate() {
                *slot = [
                    u(&uv, uv_rotation, index) / 16.0,
                    v(&uv, uv_rotation, index) / 16.0,
                ];
            }

            let normal = facing(&positions);
            quads.push(Quad {
                positions,
                uvs,
                texture,
                normal,
            });
        }
    }
    Ok(quads)
}

fn read_vec3(value: &Value) -> Result<[f64; 3]> {
    let values = numbers(value)?;
    let [x, y, z] = values[..] else {
        bail!("expected three numbers, got {}", values.len())
    };
    Ok([x, y, z])
}

fn read_vec4(value: &Value) -> Result<[f64; 4]> {
    let values = numbers(value)?;
    let [a, b, c, d] = values[..] else {
        bail!("expected four numbers, got {}", values.len())
    };
    Ok([a, b, c, d])
}

fn numbers(value: &Value) -> Result<Vec<f64>> {
    value
        .as_array()
        .context("expected an array of numbers")?
        .iter()
        .map(|entry| entry.as_f64().context("expected a number"))
        .collect()
}

fn default_uv(direction: &str, source: &[f64; 3], target: &[f64; 3]) -> [f64; 4] {
    match direction {
        "down" => [source[0], 16.0 - target[2], target[0], 16.0 - source[2]],
        "up" => [source[0], source[2], target[0], target[2]],
        "south" => [source[0], 16.0 - target[1], target[0], 16.0 - source[1]],
        "west" => [source[2], 16.0 - target[1], target[2], 16.0 - source[1]],
        "east" => [
            16.0 - target[2],
            16.0 - target[1],
            16.0 - source[2],
            16.0 - source[1],
        ],
        _ => [
            16.0 - target[0],
            16.0 - target[1],
            16.0 - source[0],
            16.0 - source[1],
        ],
    }
}

fn u(uv: &[f64; 4], rotation: i64, index: usize) -> f64 {
    let shifted = shifted_index(rotation, index);
    if shifted == 0 || shifted == 1 {
        uv[0]
    } else {
        uv[2]
    }
}

fn v(uv: &[f64; 4], rotation: i64, index: usize) -> f64 {
    let shifted = shifted_index(rotation, index);
    if shifted == 0 || shifted == 3 {
        uv[1]
    } else {
        uv[3]
    }
}

fn shifted_index(rotation: i64, index: usize) -> i64 {
    (index as i64 + rotation.div_euclid(90)).rem_euclid(4)
}

fn rotate_element(point: [f64; 3], rotation: Option<&Value>) -> Result<[f64; 3]> {
    let Some(rotation) = rotation else {
        return Ok(point);
    };

    let axis = rotation["axis"]
        .as_str()
        .context("element rotation without an axis")?;
    let degrees = rotation["angle"]
        .as_f64()
        .context("element rotation without an angle")?;
    let angle = degrees.to_radians();
    let origin = read_vec3(&rotation["origin"])?.map(|value| value / 16.0);

    let mut scale = [1.0; 3];
    if rotation["rescale"].as_bool().unwrap_or(false) {
        let factor = 1.0
            + if degrees.abs() == 22.5 {
                *RESCALE_22_5
            } else {
                *RESCALE_45
            };
        for (index, name) in ["x", "y", "z"].iter().enumerate() {
            if *name != axis {
                scale[index] = factor;
            }
        }
    }

    let local = [
        point[0] - origin[0],
        point[1] - origin[1],
        point[2] - origin[2],
    ];
    let (sin, cos) = angle.sin_cos();
    let rotated = match axis {
        "x" => [
            local[0],
            local[1] * cos - local[2] * sin,
            local[1] * sin + local[2] * cos,
        ],
        "y" => [
            local[0] * cos + local[2] * sin,
            local[1],
            -local[0] * sin + local[2] * cos,
        ],
        _ => [
            local[0] * cos - local[1] * sin,
            local[0] * sin + local[1] * cos,
            local[2],
        ],
    };
    Ok([
        rotated[0] * scale[0] + origin[0],
        rotated[1] * scale[1] + origin[1],
        rotated[2] * scale[2] + origin[2],
    ])
}

fn facing(positions: &[[f64; 3]; 4]) -> [f64; 3] {
    let a = subtract(positions[0], positions[1]);
    let b = subtract(positions[2], positions[1]);
    let cross = [
        b[1] * a[2] - b[2] * a[1],
        b[2] * a[0] - b[0] * a[2],
        b[0] * a[1] - b[1] * a[0],
    ];
    let length = (cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2]).sqrt();
    if length == 0.0 {
        return normal("up");
    }
    let unit = [cross[0] / length, cross[1] / length, cross[2] / length];

    let mut best = normal("up");
    let mut best_dot = 0.0;
    for (_, candidate) in NORMALS {
        let dot = unit[0] * candidate[0] + unit[1] * candidate[1] + unit[2] * candidate[2];
        if dot >= 0.0 && dot > best_dot {
            best = candidate;
            best_dot = dot;
        }
    }
    best
}

fn subtract(a: [f64; 3], b: [f64; 3]) -> [f64; 3] {
    [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
}
