use std::collections::{BTreeSet, HashMap, HashSet};
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};
use minijinja::value::Value;
use minijinja::{AutoEscape, Environment, Error, Output, State, context};
use serde::Serialize;

use crate::document::{self, Line, Links, Node};
use crate::icons::{ICON_SIZE, IconRenderer};
use crate::mcfont;
use crate::resources::{ResourceLocation, Resources};

type PageImages = (HashMap<String, String>, HashMap<String, (u32, u32)>);

const MONOSPACE_CHARS: &str = concat!(
    " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`",
    "abcdefghijklmnopqrstuvwxyz{|}~"
);

const UI_TEXTURES: [&str; 3] = ["manual", "scroll_button", "tab_button"];

const TABS: [(&str, &str, &str); 4] = [
    (
        "index.md",
        "Home",
        "texture:oc2:textures/gui/manual/home.png",
    ),
    ("block/index.md", "Blocks", "item:oc2:computer"),
    ("item/index.md", "Items", "item:oc2:transistor"),
    (
        "device/index.md",
        "API",
        "texture:oc2:textures/gui/manual/api.png",
    ),
];

const PAGE_TEMPLATE: &str = include_str!("../templates/page.html");
const REDIRECT_TEMPLATE: &str = include_str!("../templates/redirect.html");
const STYLESHEET: &str = include_str!("../static/manual.css");
const SCRIPT: &str = include_str!("../static/manual.js");

#[derive(Serialize)]
struct Tab {
    href: String,
    title: String,
    icon: String,
    active: bool,
}

pub struct SiteBuilder<'a> {
    resources: &'a Resources,
    doc_roots: Vec<PathBuf>,
    language: String,
    output: PathBuf,
}

impl<'a> SiteBuilder<'a> {
    pub fn new(
        resources: &'a Resources,
        doc_roots: Vec<PathBuf>,
        language: String,
        output: PathBuf,
    ) -> Self {
        Self {
            resources,
            doc_roots,
            language,
            output,
        }
    }

    pub fn build(&self) -> Result<()> {
        let mut sources = Vec::new();
        for root in &self.doc_roots {
            for path in markdown_sources(&root.join(&self.language))? {
                sources.push((root, path));
            }
        }
        if sources.is_empty() {
            bail!(
                "no manual pages under {}",
                self.doc_roots[0].join(&self.language).display()
            );
        }

        let mut pages = HashSet::new();
        let mut redirects = Vec::new();
        let mut parsed: Vec<(String, Vec<Line>)> = Vec::new();
        for (root, path) in &sources {
            let key = path
                .strip_prefix(root)?
                .to_string_lossy()
                .replace(std::path::MAIN_SEPARATOR, "/");
            let text = std::fs::read_to_string(path)?;
            pages.insert(key.clone());
            match document::redirect_target(&text) {
                Some(target) => redirects.push((key, target)),
                None => parsed.push((key, document::parse(&text)?)),
            }
        }

        std::fs::create_dir_all(self.output.join("assets"))?;
        let (images, sizes) = self.copy_images()?;
        let icons = self.render_icons(&parsed)?;
        let links = Links {
            pages,
            images,
            icons,
            sizes,
        };

        let list_indent = self.write_fonts(&parsed)?;
        self.copy_ui_textures()?;
        std::fs::write(self.output.join("assets/manual.js"), SCRIPT)?;
        std::fs::write(
            self.output.join("assets/manual.css"),
            STYLESHEET.replace("__LIST_INDENT__", &list_indent.to_string()),
        )?;

        let environment = templates();
        for (key, lines) in &parsed {
            self.write_page(&environment, key, lines, &links)?;
        }
        for (key, target) in &redirects {
            let href = links.page_href(key, target).0;
            self.write_redirect(&environment, key, &href)?;
        }
        self.write_redirect(
            &environment,
            "index.md",
            &format!("{}/index.html", self.language),
        )?;

        println!(
            "rendered {} pages into {}",
            sources.len(),
            self.output.display()
        );
        Ok(())
    }

    fn copy_images(&self) -> Result<PageImages> {
        let target = self.output.join("img");
        std::fs::create_dir_all(&target)?;
        let mut images = HashMap::new();
        let mut sizes = HashMap::new();

        let mut entries: Vec<PathBuf> = std::fs::read_dir(self.doc_roots[0].join("img"))?
            .filter_map(|entry| entry.ok().map(|entry| entry.path()))
            .filter(|path| path.extension().is_some_and(|extension| extension == "png"))
            .collect();
        entries.sort();

        for source in entries {
            let name = source
                .file_name()
                .context("image name")?
                .to_string_lossy()
                .into_owned();
            std::fs::copy(&source, target.join(&name))?;
            let key = format!("img/{name}");
            let dimensions = image::image_dimensions(&source)?;
            images.insert(key.clone(), key.clone());
            sizes.insert(key, dimensions);
        }
        Ok((images, sizes))
    }

    fn render_icons(&self, parsed: &[(String, Vec<Line>)]) -> Result<HashMap<String, String>> {
        let mut references: BTreeSet<String> =
            TABS.iter().map(|(_, _, icon)| (*icon).to_owned()).collect();
        for (_, lines) in parsed {
            for line in lines {
                collect_images(&line.nodes, &mut references);
            }
        }

        let target = self.output.join("icons");
        std::fs::create_dir_all(&target)?;
        let mut renderer = IconRenderer::new(self.resources, ICON_SIZE);
        let mut icons = HashMap::new();
        for reference in references {
            let Some((kind, data)) = reference.split_once(':') else {
                continue;
            };
            let name = format!("{}.png", slug(&reference));
            match kind {
                "texture" => {
                    let bytes = self.resources.read(&ResourceLocation::parse(data))?;
                    std::fs::write(target.join(&name), bytes)?;
                }
                "item" | "block" => renderer.render(data)?.save(target.join(&name))?,
                _ => bail!("unsupported image reference: {reference}"),
            }
            icons.insert(reference, format!("icons/{name}"));
        }
        Ok(icons)
    }

    fn copy_ui_textures(&self) -> Result<()> {
        let target = self.output.join("assets/ui");
        std::fs::create_dir_all(&target)?;
        for name in UI_TEXTURES {
            let location = ResourceLocation::new("oc2", format!("textures/gui/manual/{name}.png"));
            std::fs::write(
                target.join(format!("{name}.png")),
                self.resources.read(&location)?,
            )?;
        }
        Ok(())
    }

    fn write_fonts(&self, parsed: &[(String, Vec<Line>)]) -> Result<u32> {
        let target = self.output.join("assets/fonts");
        std::fs::create_dir_all(&target)?;

        let mut codepoints: BTreeSet<u32> = MONOSPACE_CHARS.chars().map(|c| c as u32).collect();
        for (_, lines) in parsed {
            for line in lines {
                collect_text(&line.nodes, &mut codepoints);
            }
        }

        let regular = mcfont::load_vanilla_font(self.resources)?.restrict(&codepoints);
        for (bold, italic, name) in [
            (false, false, "regular"),
            (true, false, "bold"),
            (false, true, "italic"),
            (true, true, "bolditalic"),
        ] {
            let style = capitalize(name);
            let data = mcfont::build_font(&regular, "OC2 Manual", bold, italic, &style)?;
            std::fs::write(target.join(format!("manual-{name}.ttf")), data)?;
        }

        let atlas = self
            .resources
            .read(&ResourceLocation::new("oc2", "textures/font/monospace.png"))?;
        let monospace = mcfont::load_grid_font(&atlas, MONOSPACE_CHARS, 6, 9, 96)?;
        let data = mcfont::build_font(&monospace, "OC2 Manual Mono", false, false, "Regular")?;
        std::fs::write(target.join("manual-mono.ttf"), data)?;

        Ok(regular.width("- "))
    }

    fn write_redirect(&self, environment: &Environment, key: &str, target: &str) -> Result<()> {
        let path = self.output.join(html_path(key));
        std::fs::create_dir_all(path.parent().context("page parent")?)?;
        let rendered = environment
            .get_template("redirect.html")?
            .render(context! { target => target })?;
        std::fs::write(path, rendered)?;
        Ok(())
    }

    fn write_page(
        &self,
        environment: &Environment,
        key: &str,
        lines: &[Line],
        links: &Links,
    ) -> Result<()> {
        let depth = key.matches('/').count();
        let prefix = "../".repeat(depth);
        let path = self.output.join(html_path(key));
        std::fs::create_dir_all(path.parent().context("page parent")?)?;

        let tabs: Vec<Tab> = TABS
            .iter()
            .map(|(page, name, icon)| {
                let full = format!("{}/{page}", self.language);
                Tab {
                    href: html_path(&links.relative(key, &full)),
                    title: (*name).to_owned(),
                    icon: links.relative(key, &links.icons[*icon]),
                    active: key == full,
                }
            })
            .collect();

        let rendered = environment.get_template("page.html")?.render(context! {
            title => title(lines),
            language => self.language,
            body => document::to_html(lines, key, links),
            assets => format!("{prefix}assets"),
            tabs => tabs,
        })?;
        std::fs::write(path, rendered)?;
        Ok(())
    }
}

fn format_value(out: &mut Output, state: &State, value: &Value) -> Result<(), Error> {
    if value.is_safe() || matches!(state.auto_escape(), AutoEscape::None) {
        write!(out, "{value}")?;
    } else {
        out.write_str(&document::escape(&value.to_string()))?;
    }
    Ok(())
}

fn templates() -> Environment<'static> {
    let mut environment = Environment::new();
    environment.set_formatter(format_value);
    environment.set_trim_blocks(true);
    environment.set_lstrip_blocks(true);
    environment
        .add_template("page.html", PAGE_TEMPLATE)
        .expect("page template");
    environment
        .add_template("redirect.html", REDIRECT_TEMPLATE)
        .expect("redirect template");
    environment
}

fn html_path(key: &str) -> String {
    match key.strip_suffix(".md") {
        Some(stem) => format!("{stem}.html"),
        None => key.to_owned(),
    }
}

fn markdown_sources(root: &Path) -> Result<Vec<PathBuf>> {
    let mut found = Vec::new();
    if !root.is_dir() {
        return Ok(found);
    }
    let mut stack = vec![root.to_path_buf()];
    while let Some(directory) = stack.pop() {
        for entry in std::fs::read_dir(directory)? {
            let path = entry?.path();
            if path.is_dir() {
                stack.push(path);
            } else if path.extension().is_some_and(|extension| extension == "md") {
                found.push(path);
            }
        }
    }
    found.sort();
    Ok(found)
}

fn slug(reference: &str) -> String {
    let mut out = String::new();
    for character in reference.to_lowercase().chars() {
        if character.is_ascii_alphanumeric() {
            out.push(character);
        } else if !out.ends_with('_') {
            out.push('_');
        }
    }
    out.trim_matches('_').to_owned()
}

fn capitalize(value: &str) -> String {
    let mut characters = value.chars();
    match characters.next() {
        Some(first) => first.to_uppercase().collect::<String>() + characters.as_str(),
        None => String::new(),
    }
}

fn collect_images(nodes: &[Node], out: &mut BTreeSet<String>) {
    for node in nodes {
        match node {
            Node::Image { url, .. } => {
                out.insert(url.clone());
            }
            Node::Bold(children)
            | Node::Italic(children)
            | Node::Strikethrough(children)
            | Node::Header { children, .. }
            | Node::Link { children, .. } => collect_images(children, out),
            Node::Text(_) | Node::Code(_) => {}
        }
    }
}

fn collect_text(nodes: &[Node], out: &mut BTreeSet<u32>) {
    for node in nodes {
        match node {
            Node::Text(text) | Node::Code(text) => out.extend(text.chars().map(|c| c as u32)),
            Node::Bold(children)
            | Node::Italic(children)
            | Node::Strikethrough(children)
            | Node::Header { children, .. }
            | Node::Link { children, .. } => collect_text(children, out),
            Node::Image { title, .. } => out.extend(title.chars().map(|c| c as u32)),
        }
    }
}

fn title(lines: &[Line]) -> String {
    for line in lines {
        if line.header_level > 0
            && let Node::Header { children, .. } = &line.nodes[0]
        {
            return plain(children).trim().to_owned();
        }
    }
    "Manual".to_owned()
}

fn plain(nodes: &[Node]) -> String {
    nodes
        .iter()
        .map(|node| match node {
            Node::Text(text) | Node::Code(text) => text.clone(),
            Node::Image { title, .. } => title.clone(),
            Node::Bold(children)
            | Node::Italic(children)
            | Node::Strikethrough(children)
            | Node::Header { children, .. }
            | Node::Link { children, .. } => plain(children),
        })
        .collect()
}
