use std::collections::HashMap;
use std::sync::LazyLock;

use serde_json::Value;

use crate::mcmodel::Quad;

const SLOT_SIZE: f64 = 16.0;
const LIGHT_POWER: f64 = 0.6;
const AMBIENT_LIGHT: f64 = 0.4;
const DIFFUSE_0: [f64; 3] = [0.2, 1.0, -0.7];
const DIFFUSE_1: [f64; 3] = [-0.2, 1.0, 0.7];

pub type Matrix = [[f64; 3]; 3];

pub struct Texture {
    pub width: usize,
    pub height: usize,
    pub pixels: Vec<[f64; 4]>,
}

impl Texture {
    pub fn sample(&self, x: usize, y: usize) -> [f64; 4] {
        self.pixels[y * self.width + x]
    }
}

pub struct Canvas {
    pub size: usize,
    pub pixels: Vec<[f64; 4]>,
}

impl Canvas {
    fn new(size: usize) -> Self {
        Self {
            size,
            pixels: vec![[0.0; 4]; size * size],
        }
    }
}

fn rotation(axis: char, angle: f64) -> Matrix {
    let (sin, cos) = angle.sin_cos();
    match axis {
        'x' => [[1.0, 0.0, 0.0], [0.0, cos, -sin], [0.0, sin, cos]],
        'y' => [[cos, 0.0, sin], [0.0, 1.0, 0.0], [-sin, 0.0, cos]],
        _ => [[cos, -sin, 0.0], [sin, cos, 0.0], [0.0, 0.0, 1.0]],
    }
}

fn multiply(a: Matrix, b: Matrix) -> Matrix {
    let mut out = [[0.0; 3]; 3];
    for row in 0..3 {
        for column in 0..3 {
            out[row][column] = (0..3).map(|k| a[row][k] * b[k][column]).sum();
        }
    }
    out
}

fn apply(matrix: Matrix, vector: [f64; 3]) -> [f64; 3] {
    let mut out = [0.0; 3];
    for row in 0..3 {
        out[row] = (0..3).map(|k| matrix[row][k] * vector[k]).sum();
    }
    out
}

fn diagonal(values: [f64; 3]) -> Matrix {
    [
        [values[0], 0.0, 0.0],
        [0.0, values[1], 0.0],
        [0.0, 0.0, values[2]],
    ]
}

fn rotation_xyz(x: f64, y: f64, z: f64) -> Matrix {
    multiply(
        multiply(rotation('x', x), rotation('y', y)),
        rotation('z', z),
    )
}

fn rotation_yxz(y: f64, x: f64, z: f64) -> Matrix {
    multiply(
        multiply(rotation('y', y), rotation('x', x)),
        rotation('z', z),
    )
}

fn normalize(vector: [f64; 3]) -> [f64; 3] {
    let length = (vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]).sqrt();
    [vector[0] / length, vector[1] / length, vector[2] / length]
}

pub static GUI_LIGHTS: LazyLock<[[f64; 3]; 2]> = LazyLock::new(|| {
    let matrix = multiply(
        multiply(
            diagonal([1.0, -1.0, 1.0]),
            rotation_yxz(1.0821041, 3.2375858, 0.0),
        ),
        rotation_yxz(
            -std::f64::consts::PI / 8.0,
            std::f64::consts::PI * 3.0 / 4.0,
            0.0,
        ),
    );
    [
        apply(matrix, normalize(DIFFUSE_0)),
        apply(matrix, normalize(DIFFUSE_1)),
    ]
});

pub static FLAT_LIGHTS: LazyLock<[[f64; 3]; 2]> = LazyLock::new(|| {
    let matrix = multiply(
        rotation('y', -std::f64::consts::PI / 8.0),
        rotation('x', std::f64::consts::PI * 3.0 / 4.0),
    );
    [
        apply(matrix, normalize(DIFFUSE_0)),
        apply(matrix, normalize(DIFFUSE_1)),
    ]
});

pub fn diffuse(normal: [f64; 3], lights: &[[f64; 3]; 2]) -> f64 {
    let accumulate = |light: [f64; 3]| {
        (normal[0] * light[0] + normal[1] * light[1] + normal[2] * light[2]).max(0.0)
    };
    (1.0f64).min((accumulate(lights[0]) + accumulate(lights[1])) * LIGHT_POWER + AMBIENT_LIGHT)
}

fn transform(display: &HashMap<String, Value>) -> (Matrix, [f64; 3]) {
    let gui = display.get("gui");
    let rotation_degrees = read_vec3(gui, "rotation", [0.0; 3]);
    let translation = read_vec3(gui, "translation", [0.0; 3]).map(|value| value * 0.0625);
    let scale = read_vec3(gui, "scale", [1.0; 3]);

    let flip = diagonal([SLOT_SIZE, -SLOT_SIZE, SLOT_SIZE]);
    let matrix = multiply(
        multiply(
            flip,
            rotation_xyz(
                rotation_degrees[0].to_radians(),
                rotation_degrees[1].to_radians(),
                rotation_degrees[2].to_radians(),
            ),
        ),
        diagonal(scale),
    );
    (matrix, apply(flip, translation))
}

fn normal_transform(display: &HashMap<String, Value>) -> Matrix {
    let rotation_degrees = read_vec3(display.get("gui"), "rotation", [0.0; 3]);
    multiply(
        diagonal([1.0, -1.0, 1.0]),
        rotation_xyz(
            rotation_degrees[0].to_radians(),
            rotation_degrees[1].to_radians(),
            rotation_degrees[2].to_radians(),
        ),
    )
}

fn read_vec3(value: Option<&Value>, key: &str, fallback: [f64; 3]) -> [f64; 3] {
    let Some(values) = value.and_then(|value| value[key].as_array()) else {
        return fallback;
    };
    let numbers: Vec<f64> = values.iter().filter_map(Value::as_f64).collect();
    let [x, y, z] = numbers[..] else {
        panic!("`{key}` needs three numbers")
    };
    [x, y, z]
}

pub fn render(
    quads: &[Quad],
    display: &HashMap<String, Value>,
    textures: &HashMap<String, Texture>,
    size: usize,
    lights: &[[f64; 3]; 2],
    cull: bool,
) -> Canvas {
    let (matrix, offset) = transform(display);
    let normals = normal_transform(display);
    let pixels_per_unit = size as f64 / SLOT_SIZE;
    let center = SLOT_SIZE / 2.0;

    let mut canvas = Canvas::new(size);
    let mut depth = vec![f64::NEG_INFINITY; size * size];

    let mut prepared: Vec<(f64, [[f64; 3]; 4], &Quad, f64)> = Vec::new();
    for quad in quads {
        let normal = apply(normals, quad.normal);
        if cull && normal[2] <= 0.0 {
            continue;
        }
        let mut screen = [[0.0; 3]; 4];
        for (index, position) in quad.positions.iter().enumerate() {
            let point = apply(
                matrix,
                [position[0] - 0.5, position[1] - 0.5, position[2] - 0.5],
            );
            screen[index] = [
                (point[0] + offset[0] + center) * pixels_per_unit,
                (point[1] + offset[1] + center) * pixels_per_unit,
                point[2] + offset[2],
            ];
        }
        let depth_key = screen.iter().map(|point| point[2]).sum::<f64>() / 4.0;
        prepared.push((depth_key, screen, quad, diffuse(normal, lights)));
    }

    prepared.sort_by(|a, b| a.0.total_cmp(&b.0));

    for (_, screen, quad, light) in prepared {
        let texture = &textures[&quad.texture];
        for [a, b, c] in [[0, 1, 2], [0, 2, 3]] {
            triangle(
                &mut canvas,
                &mut depth,
                [screen[a], screen[b], screen[c]],
                [quad.uvs[a], quad.uvs[b], quad.uvs[c]],
                texture,
                light,
            );
        }
    }
    canvas
}

fn triangle(
    canvas: &mut Canvas,
    depth: &mut [f64],
    screen: [[f64; 3]; 3],
    uvs: [[f64; 2]; 3],
    texture: &Texture,
    light: f64,
) {
    let size = canvas.size as f64;
    let min_x = screen
        .iter()
        .map(|p| p[0])
        .fold(f64::INFINITY, f64::min)
        .floor()
        .max(0.0) as usize;
    let max_x = screen
        .iter()
        .map(|p| p[0])
        .fold(f64::NEG_INFINITY, f64::max)
        .ceil()
        .min(size - 1.0);
    let min_y = screen
        .iter()
        .map(|p| p[1])
        .fold(f64::INFINITY, f64::min)
        .floor()
        .max(0.0) as usize;
    let max_y = screen
        .iter()
        .map(|p| p[1])
        .fold(f64::NEG_INFINITY, f64::max)
        .ceil()
        .min(size - 1.0);
    if max_x < 0.0 || max_y < 0.0 {
        return;
    }
    let (max_x, max_y) = (max_x as usize, max_y as usize);
    if min_x > max_x || min_y > max_y {
        return;
    }

    let [x0, y0] = [screen[0][0], screen[0][1]];
    let [x1, y1] = [screen[1][0], screen[1][1]];
    let [x2, y2] = [screen[2][0], screen[2][1]];
    let area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    if area.abs() < 1e-9 {
        return;
    }

    for y in min_y..=max_y {
        for x in min_x..=max_x {
            let px = x as f64 + 0.5;
            let py = y as f64 + 0.5;
            let w1 = ((px - x0) * (y2 - y0) - (x2 - x0) * (py - y0)) / area;
            let w2 = ((x1 - x0) * (py - y0) - (px - x0) * (y1 - y0)) / area;
            let w0 = 1.0 - w1 - w2;
            if w0 < 0.0 || w1 < 0.0 || w2 < 0.0 {
                continue;
            }

            let index = y * canvas.size + x;
            let z = w0 * screen[0][2] + w1 * screen[1][2] + w2 * screen[2][2];
            if z < depth[index] {
                continue;
            }

            let u = w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0];
            let v = w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1];
            let tx = ((u * texture.width as f64) as isize).clamp(0, texture.width as isize - 1);
            let ty = ((v * texture.height as f64) as isize).clamp(0, texture.height as isize - 1);
            let sample = texture.sample(tx as usize, ty as usize);

            let alpha = sample[3];
            let target = &mut canvas.pixels[index];
            for channel in 0..3 {
                target[channel] = sample[channel] * light * alpha + target[channel] * (1.0 - alpha);
            }
            target[3] = alpha + target[3] * (1.0 - alpha);

            if alpha >= 0.5 {
                depth[index] = z;
            }
        }
    }
}

pub fn render_flat(
    layers: &[&Texture],
    tints: &[[f64; 3]],
    size: usize,
    lights: &[[f64; 3]; 2],
) -> Canvas {
    let mut canvas = Canvas::new(size);
    let light = diffuse([0.0, 0.0, 1.0], lights);
    for (layer, tint) in layers.iter().zip(tints) {
        for y in 0..size {
            let source_y = y * layer.height / size;
            for x in 0..size {
                let source_x = x * layer.width / size;
                let sample = layer.sample(source_x, source_y);
                let alpha = sample[3];
                let target = &mut canvas.pixels[y * size + x];
                for channel in 0..3 {
                    target[channel] = sample[channel] * tint[channel] * light * alpha
                        + target[channel] * (1.0 - alpha);
                }
                target[3] = alpha + target[3] * (1.0 - alpha);
            }
        }
    }
    canvas
}
