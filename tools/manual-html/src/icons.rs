use std::collections::HashMap;

use anyhow::{Context, Result};
use image::RgbaImage;

use crate::mcmodel::{self, Quad};
use crate::render::{self, Texture};
use crate::resources::{ResourceLocation, Resources};

pub const ICON_SIZE: usize = 128;

const NO_TINT: u32 = 0xFFFFFFFF;
const TINTED_LAYER: usize = 1;
const DEFAULT_TINTS: [(&str, u32); 5] = [
    ("oc2:hard_drive_small", 0xFF9D9D97),
    ("oc2:hard_drive_medium", 0xFF5E7C16),
    ("oc2:hard_drive_large", 0xFF169C9C),
    ("oc2:flash_memory", 0xFF77B294),
    ("oc2:floppy", 0xFFA06540),
];

pub struct IconRenderer<'a> {
    resources: &'a Resources,
    size: usize,
    textures: HashMap<String, Texture>,
}

impl<'a> IconRenderer<'a> {
    pub fn new(resources: &'a Resources, size: usize) -> Self {
        Self {
            resources,
            size,
            textures: HashMap::new(),
        }
    }

    pub fn render(&mut self, reference: &str) -> Result<RgbaImage> {
        let location =
            ResourceLocation::parse(reference.split('[').next().expect("split yields one part"));
        let model = mcmodel::load_model(
            self.resources,
            &ResourceLocation::new(&location.namespace, format!("item/{}", location.path)),
        )?;
        let lights = if model.gui_light == "front" {
            &*render::FLAT_LIGHTS
        } else {
            &*render::GUI_LIGHTS
        };

        let id = location.to_string();
        let canvas = if id == "oc2:robot" {
            let quads = self.robot()?;
            render::render(
                &quads,
                &model.display,
                &self.textures,
                self.size,
                lights,
                false,
            )
        } else if mcmodel::is_generated_item(&model) {
            let mut names = Vec::new();
            let mut index = 0;
            while model.textures.contains_key(&format!("layer{index}")) {
                let name = model
                    .texture(&format!("layer{index}"))
                    .context("layer texture")?
                    .to_owned();
                self.load_texture(&name)?;
                names.push(name);
                index += 1;
            }
            let tint = DEFAULT_TINTS
                .iter()
                .find(|(name, _)| *name == id)
                .map(|(_, tint)| *tint)
                .unwrap_or(NO_TINT);
            let tints: Vec<[f64; 3]> = (0..names.len())
                .map(|index| rgb(if index == TINTED_LAYER { tint } else { NO_TINT }))
                .collect();
            let layers: Vec<&Texture> = names.iter().map(|name| &self.textures[name]).collect();
            render::render_flat(&layers, &tints, self.size, lights)
        } else {
            let quads = mcmodel::bake(&model)?;
            for quad in &quads {
                self.load_texture(&quad.texture)?;
            }
            render::render(
                &quads,
                &model.display,
                &self.textures,
                self.size,
                lights,
                true,
            )
        };

        Ok(to_image(&canvas))
    }

    fn load_texture(&mut self, name: &str) -> Result<()> {
        if self.textures.contains_key(name) {
            return Ok(());
        }

        let location = ResourceLocation::parse(name);
        let path = format!("textures/{}.png", location.path);
        let data = self
            .resources
            .read(&ResourceLocation::new(&location.namespace, &path))?;
        let mut image = image::load_from_memory(&data)?.to_rgba8();

        let meta = self.resources.find(&ResourceLocation::new(
            &location.namespace,
            format!("{path}.mcmeta"),
        ));
        if let Some(meta) = meta {
            let parsed: serde_json::Value = serde_json::from_slice(&meta)?;
            if parsed.get("animation").is_some() {
                let width = image.width();
                image = image::imageops::crop_imm(&image, 0, 0, width, width).to_image();
            }
        }

        let pixels = image
            .pixels()
            .map(|pixel| {
                [
                    pixel.0[0] as f64 / 255.0,
                    pixel.0[1] as f64 / 255.0,
                    pixel.0[2] as f64 / 255.0,
                    pixel.0[3] as f64 / 255.0,
                ]
            })
            .collect();
        self.textures.insert(
            name.to_owned(),
            Texture {
                width: image.width() as usize,
                height: image.height() as usize,
                pixels,
            },
        );
        Ok(())
    }

    fn robot(&mut self) -> Result<Vec<Quad>> {
        let texture = "oc2:entity/robot/robot";
        self.load_texture(texture)?;
        let size = (64.0, 64.0);
        let mut quads = entity_cube(
            texture,
            1.0,
            1.0,
            (-7.0, 8.0, -7.0),
            (14.0, 6.0, 14.0),
            size,
        );
        quads.extend(entity_cube(
            texture,
            1.0,
            23.0,
            (-7.0, 0.0, -7.0),
            (14.0, 7.0, 14.0),
            size,
        ));
        quads.extend(entity_cube(
            texture,
            1.0,
            34.0,
            (-6.0, 7.0, -6.0),
            (12.0, 1.0, 12.0),
            size,
        ));
        Ok(quads)
    }
}

fn rgb(argb: u32) -> [f64; 3] {
    [
        ((argb >> 16) & 0xFF) as f64 / 255.0,
        ((argb >> 8) & 0xFF) as f64 / 255.0,
        (argb & 0xFF) as f64 / 255.0,
    ]
}

fn to_image(canvas: &render::Canvas) -> RgbaImage {
    let size = canvas.size as u32;
    RgbaImage::from_fn(size, size, |x, y| {
        let pixel = canvas.pixels[y as usize * canvas.size + x as usize];
        let alpha = pixel[3];
        let channel = |value: f64| {
            let straight = if alpha > 0.0 { value / alpha } else { 0.0 };
            (straight.clamp(0.0, 1.0) * 255.0 + 0.5) as u8
        };
        image::Rgba([
            channel(pixel[0]),
            channel(pixel[1]),
            channel(pixel[2]),
            (alpha.clamp(0.0, 1.0) * 255.0 + 0.5) as u8,
        ])
    })
}

fn entity_cube(
    texture: &str,
    tex_u: f64,
    tex_v: f64,
    origin: (f64, f64, f64),
    size: (f64, f64, f64),
    texture_size: (f64, f64),
) -> Vec<Quad> {
    let (x0, y0, z0) = origin;
    let (dx, dy, dz) = size;
    let (x1, y1, z1) = (x0 + dx, y0 + dy, z0 + dz);

    let v1 = [x0, y0, z0];
    let v2 = [x1, y0, z0];
    let v3 = [x1, y1, z0];
    let v4 = [x0, y1, z0];
    let v5 = [x0, y0, z1];
    let v6 = [x1, y0, z1];
    let v7 = [x1, y1, z1];
    let v8 = [x0, y1, z1];

    let (u_a, u_b) = (tex_u, tex_u + dz);
    let (u_c, u_d) = (tex_u + dz + dx, tex_u + dz + dx + dx);
    let (u_e, u_f) = (tex_u + dz + dx + dz, tex_u + dz + dx + dz + dx);
    let (v_a, v_b, v_c) = (tex_v, tex_v + dz, tex_v + dz + dy);

    let faces = [
        ([v6, v5, v1, v2], [u_b, v_a, u_c, v_b], "down"),
        ([v3, v4, v8, v7], [u_c, v_b, u_d, v_a], "up"),
        ([v1, v5, v8, v4], [u_a, v_b, u_b, v_c], "west"),
        ([v2, v1, v4, v3], [u_b, v_b, u_c, v_c], "north"),
        ([v6, v2, v3, v7], [u_c, v_b, u_e, v_c], "east"),
        ([v5, v6, v7, v8], [u_e, v_b, u_f, v_c], "south"),
    ];

    let (width, height) = texture_size;
    faces
        .into_iter()
        .map(|(positions, [u1, tv1, u2, tv2], direction)| {
            let uvs = [
                [u2 / width, tv1 / height],
                [u1 / width, tv1 / height],
                [u1 / width, tv2 / height],
                [u2 / width, tv2 / height],
            ];
            let mut shifted = [[0.0; 3]; 4];
            for (index, position) in positions.iter().enumerate() {
                shifted[index] = [
                    position[0] / 16.0 + 0.5,
                    position[1] / 16.0,
                    position[2] / 16.0 + 0.5,
                ];
            }
            Quad {
                positions: shifted,
                uvs,
                texture: texture.to_owned(),
                normal: mcmodel::normal(direction),
            }
        })
        .collect()
}
