use std::collections::{BTreeMap, BTreeSet, HashSet};

use anyhow::{Context, Result, anyhow};
use kurbo::BezPath;
use write_fonts::FontBuilder;
use write_fonts::tables::cmap::Cmap;
use write_fonts::tables::glyf::{GlyfLocaBuilder, SimpleGlyph};
use write_fonts::tables::head::Head;
use write_fonts::tables::hhea::Hhea;
use write_fonts::tables::hmtx::{Hmtx, LongMetric};
use write_fonts::tables::maxp::Maxp;
use write_fonts::tables::name::{Name, NameRecord};
use write_fonts::tables::os2::Os2;
use write_fonts::tables::post::Post;
use write_fonts::types::{Fixed, GlyphId, LongDateTime, NameId, Version16Dot16};

use crate::resources::{ResourceLocation, Resources};

pub const LINE_HEIGHT: i32 = 10;
pub const ASCENT: i32 = 7;

const UNITS_PER_PIXEL: i32 = 100;
const UNITS_PER_EM: i32 = LINE_HEIGHT * UNITS_PER_PIXEL;
const ITALIC_SLOPE: f64 = 0.25;
const BOLD_OFFSET: u32 = 1;
const EPOCH: i64 = 3786912000;

#[derive(Clone)]
pub struct Glyph {
    pub mask: Vec<Vec<bool>>,
    pub advance: u32,
    pub ascent: i32,
}

#[derive(Default)]
pub struct BitmapFont {
    pub glyphs: BTreeMap<u32, Glyph>,
    pub blank_advances: BTreeMap<u32, u32>,
}

impl BitmapFont {
    pub fn advance(&self, codepoint: u32) -> u32 {
        match self.glyphs.get(&codepoint) {
            Some(glyph) => glyph.advance,
            None => self.blank_advances.get(&codepoint).copied().unwrap_or(0),
        }
    }

    pub fn width(&self, text: &str) -> u32 {
        text.chars().map(|c| self.advance(c as u32)).sum()
    }

    pub fn restrict(&self, codepoints: &BTreeSet<u32>) -> BitmapFont {
        BitmapFont {
            glyphs: self
                .glyphs
                .iter()
                .filter(|(c, _)| codepoints.contains(c))
                .map(|(c, g)| (*c, g.clone()))
                .collect(),
            blank_advances: self
                .blank_advances
                .iter()
                .filter(|(c, _)| codepoints.contains(c))
                .map(|(c, a)| (*c, *a))
                .collect(),
        }
    }

    fn codepoints(&self) -> Vec<u32> {
        let mut all: BTreeSet<u32> = self.glyphs.keys().copied().collect();
        all.extend(self.blank_advances.keys().copied());
        all.into_iter().collect()
    }
}

/// Resolves the vanilla font definition, following `reference` providers.
pub fn load_vanilla_font(resources: &Resources) -> Result<BitmapFont> {
    let mut font = BitmapFont::default();
    let mut seen = HashSet::new();
    load_providers(
        resources,
        &ResourceLocation::parse("minecraft:font/default.json"),
        &mut font,
        &mut seen,
    )?;
    Ok(font)
}

fn load_providers(
    resources: &Resources,
    location: &ResourceLocation,
    font: &mut BitmapFont,
    seen: &mut HashSet<String>,
) -> Result<()> {
    if !seen.insert(location.to_string()) {
        return Ok(());
    }

    let definition = resources.read_json(location)?;
    for provider in definition["providers"]
        .as_array()
        .context("font without providers")?
    {
        match provider["type"]
            .as_str()
            .context("provider without a type")?
        {
            "reference" => {
                let reference = ResourceLocation::parse(
                    provider["id"].as_str().context("reference without id")?,
                );
                let nested = ResourceLocation::new(
                    &reference.namespace,
                    format!("font/{}.json", reference.path),
                );
                load_providers(resources, &nested, font, seen)?;
            }
            "space" => {
                let advances = provider["advances"].as_object().context("space advances")?;
                for (character, advance) in advances {
                    let codepoint = character
                        .chars()
                        .next()
                        .context("empty space advance key")?;
                    let advance = advance.as_f64().context("space advance")?;
                    let advance =
                        u32::try_from(advance as i64).context("negative space advance")?;
                    font.blank_advances
                        .entry(codepoint as u32)
                        .or_insert(advance);
                }
            }
            "bitmap" => load_bitmap_provider(resources, provider, font)?,
            _ => {}
        }
    }
    Ok(())
}

fn load_bitmap_provider(
    resources: &Resources,
    provider: &serde_json::Value,
    font: &mut BitmapFont,
) -> Result<()> {
    let reference = ResourceLocation::parse(provider["file"].as_str().context("bitmap file")?);
    let location =
        ResourceLocation::new(&reference.namespace, format!("textures/{}", reference.path));
    let image = image::load_from_memory(&resources.read(&location)?)?.to_rgba8();

    let grid: Vec<Vec<char>> = provider["chars"]
        .as_array()
        .context("bitmap chars")?
        .iter()
        .map(|line| line.as_str().unwrap_or_default().chars().collect())
        .collect();
    let height = match provider.get("height") {
        Some(value) => value.as_i64().context("bitmap height")? as i32,
        None => 8,
    };
    let ascent = provider["ascent"].as_i64().context("bitmap ascent")? as i32;
    let cell_width = image.width() as usize / grid[0].len();
    let cell_height = image.height() as usize / grid.len();
    let scale = height as f64 / cell_height as f64;

    for (row, line) in grid.iter().enumerate() {
        for (column, character) in line.iter().enumerate() {
            let codepoint = *character as u32;
            if codepoint == 0
                || font.glyphs.contains_key(&codepoint)
                || font.blank_advances.contains_key(&codepoint)
            {
                continue;
            }
            let cell = alpha_cell(
                &image,
                column * cell_width,
                row * cell_height,
                cell_width,
                cell_height,
            );
            let pixels = actual_width(&cell);
            let advance = (0.5 + pixels as f64 * scale) as u32 + 1;
            if pixels == 0 {
                font.blank_advances.insert(codepoint, advance);
            } else {
                let mask =
                    scaled_mask(&cell, (cell_width as f64 * scale) as usize, height as usize);
                font.glyphs.insert(
                    codepoint,
                    Glyph {
                        mask,
                        advance,
                        ascent,
                    },
                );
            }
        }
    }
    Ok(())
}

pub fn load_grid_font(
    image_data: &[u8],
    characters: &str,
    char_width: usize,
    char_height: usize,
    resolution: usize,
) -> Result<BitmapFont> {
    let image = image::load_from_memory(image_data)?.to_rgba8();
    let columns = resolution / char_width;

    let mut font = BitmapFont::default();
    for (index, character) in characters.chars().enumerate() {
        let cell = alpha_cell(
            &image,
            (index % columns) * char_width,
            (index / columns) * char_height,
            char_width,
            char_height,
        );
        if actual_width(&cell) == 0 {
            font.blank_advances
                .insert(character as u32, char_width as u32);
        } else {
            font.glyphs.insert(
                character as u32,
                Glyph {
                    mask: cell,
                    advance: char_width as u32,
                    ascent: ASCENT,
                },
            );
        }
    }
    Ok(font)
}

fn alpha_cell(
    image: &image::RgbaImage,
    x: usize,
    y: usize,
    width: usize,
    height: usize,
) -> Vec<Vec<bool>> {
    (0..height)
        .map(|row| {
            (0..width)
                .map(|column| image.get_pixel((x + column) as u32, (y + row) as u32).0[3] != 0)
                .collect()
        })
        .collect()
}

fn actual_width(mask: &[Vec<bool>]) -> usize {
    let mut width = 0;
    for row in mask {
        for (index, set) in row.iter().enumerate() {
            if *set {
                width = width.max(index + 1);
            }
        }
    }
    width
}

fn scaled_mask(mask: &[Vec<bool>], width: usize, height: usize) -> Vec<Vec<bool>> {
    if mask.len() == height && mask[0].len() == width {
        return mask.to_vec();
    }
    (0..height)
        .map(|row| {
            let source_row = &mask[(row * 2 + 1) * mask.len() / (height * 2)];
            (0..width)
                .map(|column| source_row[(column * 2 + 1) * source_row.len() / (width * 2)])
                .collect()
        })
        .collect()
}

pub fn build_font(
    font: &BitmapFont,
    family: &str,
    bold: bool,
    italic: bool,
    style_name: &str,
) -> Result<Vec<u8>> {
    let codepoints = font.codepoints();
    let extra = if bold { BOLD_OFFSET } else { 0 };

    let mut glyf_builder = GlyfLocaBuilder::new();
    glyf_builder.add_glyph(&SimpleGlyph::default())?;
    let mut metrics = vec![LongMetric::new((4 * UNITS_PER_PIXEL) as u16, 0)];
    let mut mappings = Vec::new();
    let mut bounds = Bounds::default();
    let mut max_points = 0u16;
    let mut max_contours = 0u16;
    let mut max_advance = 0u16;

    for (index, codepoint) in codepoints.iter().enumerate() {
        let glyph = font.glyphs.get(codepoint);
        let advance = match glyph {
            Some(glyph) => glyph.advance,
            None => font.blank_advances[codepoint],
        } + extra;

        let mut path = BezPath::new();
        if let Some(glyph) = glyph {
            let mask = if bold {
                emboldened(&glyph.mask)
            } else {
                glyph.mask.clone()
            };
            for rectangle in rectangles(&mask) {
                draw_rectangle(&mut path, rectangle, glyph.ascent, italic);
            }
        }
        let outline = SimpleGlyph::from_bezpath(&path).map_err(|e| anyhow!("{e:?}"))?;
        max_contours = max_contours.max(outline.contours.len() as u16);
        max_points = max_points.max(
            outline
                .contours
                .iter()
                .map(|contour| contour.len())
                .sum::<usize>() as u16,
        );
        for point in outline.contours.iter().flat_map(|contour| contour.iter()) {
            bounds.extend(point.x, point.y);
        }
        glyf_builder.add_glyph(&outline)?;
        let width = u16::try_from(advance * UNITS_PER_PIXEL as u32).context("glyph advance")?;
        max_advance = max_advance.max(width);
        metrics.push(LongMetric::new(width, 0));

        let character = char::from_u32(*codepoint).expect("codepoint came from a char");
        mappings.push((character, GlyphId::new(index as u32 + 1)));
    }

    let (glyf, loca, loca_format) = glyf_builder.build();
    let descent = -(LINE_HEIGHT - ASCENT) * UNITS_PER_PIXEL;
    let metric_count = metrics.len() as u16;

    let mut builder = FontBuilder::new();
    builder.add_table(&Head {
        units_per_em: UNITS_PER_EM as u16,
        created: LongDateTime::new(EPOCH),
        modified: LongDateTime::new(EPOCH),
        mac_style: mac_style(bold, italic),
        index_to_loc_format: loca_format as i16,
        x_min: bounds.x_min,
        y_min: bounds.y_min,
        x_max: bounds.x_max,
        y_max: bounds.y_max,
        ..Default::default()
    })?;
    builder.add_table(&Hhea {
        ascender: ((ASCENT * UNITS_PER_PIXEL) as i16).into(),
        descender: (descent as i16).into(),
        line_gap: 0i16.into(),
        number_of_h_metrics: metric_count,
        caret_slope_rise: 1,
        advance_width_max: max_advance.into(),
        x_max_extent: bounds.x_max.into(),
        ..Default::default()
    })?;
    builder.add_table(&Maxp {
        num_glyphs: (codepoints.len() + 1) as u16,
        max_points: Some(max_points),
        max_contours: Some(max_contours),
        max_composite_points: Some(0),
        max_composite_contours: Some(0),
        max_zones: Some(2),
        max_twilight_points: Some(0),
        max_storage: Some(0),
        max_function_defs: Some(0),
        max_instruction_defs: Some(0),
        max_stack_elements: Some(0),
        max_size_of_instructions: Some(0),
        max_component_elements: Some(0),
        max_component_depth: Some(0),
    })?;
    builder.add_table(&Hmtx::new(metrics, Vec::new()))?;
    builder.add_table(&Cmap::from_mappings(mappings)?)?;
    builder.add_table(&glyf)?;
    builder.add_table(&loca)?;
    builder.add_table(&name_table(family, style_name))?;
    builder.add_table(&Os2 {
        s_typo_ascender: (ASCENT * UNITS_PER_PIXEL) as i16,
        s_typo_descender: descent as i16,
        s_typo_line_gap: 0,
        us_win_ascent: (ASCENT * UNITS_PER_PIXEL) as u16,
        us_win_descent: (-descent) as u16,
        us_weight_class: if bold { 700 } else { 400 },
        us_first_char_index: codepoints.first().copied().unwrap_or(0).min(0xFFFF) as u16,
        us_last_char_index: codepoints.last().copied().unwrap_or(0).min(0xFFFF) as u16,
        fs_selection: selection(bold, italic),
        ..Default::default()
    })?;
    builder.add_table(&Post {
        version: Version16Dot16::VERSION_3_0,
        underline_position: (-UNITS_PER_PIXEL as i16).into(),
        underline_thickness: (UNITS_PER_PIXEL as i16).into(),
        italic_angle: Fixed::from_f64(if italic {
            -ITALIC_SLOPE.atan().to_degrees()
        } else {
            0.0
        }),
        ..Default::default()
    })?;

    Ok(builder.build())
}

fn mac_style(bold: bool, italic: bool) -> write_fonts::tables::head::MacStyle {
    let mut style = write_fonts::tables::head::MacStyle::empty();
    if bold {
        style |= write_fonts::tables::head::MacStyle::BOLD;
    }
    if italic {
        style |= write_fonts::tables::head::MacStyle::ITALIC;
    }
    style
}

fn selection(bold: bool, italic: bool) -> write_fonts::tables::os2::SelectionFlags {
    use write_fonts::tables::os2::SelectionFlags;
    match (bold, italic) {
        (true, true) => SelectionFlags::BOLD | SelectionFlags::ITALIC,
        (true, false) => SelectionFlags::BOLD,
        (false, true) => SelectionFlags::ITALIC,
        (false, false) => SelectionFlags::REGULAR,
    }
}

fn name_table(family: &str, style_name: &str) -> Name {
    let full = format!("{family} {style_name}");
    let postscript = format!(
        "{}-{}",
        family.replace(' ', ""),
        style_name.replace(' ', "")
    );
    let entries = [
        (NameId::FAMILY_NAME, family.to_owned()),
        (NameId::SUBFAMILY_NAME, style_name.to_owned()),
        (NameId::FULL_NAME, full),
        (NameId::VERSION_STRING, "Version 1.0".to_owned()),
        (NameId::POSTSCRIPT_NAME, postscript),
    ];
    Name::new(
        entries
            .into_iter()
            .map(|(id, value)| NameRecord::new(3, 1, 0x409, id, value.into()))
            .collect(),
    )
}

fn emboldened(mask: &[Vec<bool>]) -> Vec<Vec<bool>> {
    mask.iter()
        .map(|row| {
            (0..row.len() + 1)
                .map(|x| row.get(x).copied().unwrap_or(false) || (x > 0 && row[x - 1]))
                .collect()
        })
        .collect()
}

fn rectangles(mask: &[Vec<bool>]) -> Vec<[usize; 4]> {
    let runs: Vec<Vec<(usize, usize)>> = mask
        .iter()
        .map(|row| {
            let mut out = Vec::new();
            let mut x = 0;
            while x < row.len() {
                if row[x] {
                    let start = x;
                    while x < row.len() && row[x] {
                        x += 1;
                    }
                    out.push((start, x));
                } else {
                    x += 1;
                }
            }
            out
        })
        .collect();

    let mut rectangles = Vec::new();
    for (y, row_runs) in runs.iter().enumerate() {
        for run in row_runs {
            if y > 0 && runs[y - 1].contains(run) {
                continue;
            }
            let mut bottom = y + 1;
            while bottom < runs.len() && runs[bottom].contains(run) {
                bottom += 1;
            }
            rectangles.push([run.0, y, run.1, bottom]);
        }
    }
    rectangles
}

fn draw_rectangle(path: &mut BezPath, rectangle: [usize; 4], ascent: i32, italic: bool) {
    let [x0, y0, x1, y1] = rectangle;
    let top = ((ascent - y0 as i32) * UNITS_PER_PIXEL) as f64;
    let bottom = ((ascent - y1 as i32) * UNITS_PER_PIXEL) as f64;
    let left = (x0 as i32 * UNITS_PER_PIXEL) as f64;
    let right = (x1 as i32 * UNITS_PER_PIXEL) as f64;

    let shift = |y: f64| {
        if italic {
            (ITALIC_SLOPE * y - 0.75 * UNITS_PER_PIXEL as f64).round()
        } else {
            0.0
        }
    };

    path.move_to((left + shift(bottom), bottom));
    path.line_to((right + shift(bottom), bottom));
    path.line_to((right + shift(top), top));
    path.line_to((left + shift(top), top));
    path.close_path();
}

#[derive(Default)]
struct Bounds {
    x_min: i16,
    y_min: i16,
    x_max: i16,
    y_max: i16,
}

impl Bounds {
    fn extend(&mut self, x: i16, y: i16) {
        self.x_min = self.x_min.min(x);
        self.y_min = self.y_min.min(y);
        self.x_max = self.x_max.max(x);
        self.y_max = self.y_max.max(y);
    }
}
