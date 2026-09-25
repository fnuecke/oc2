mod document;
mod icons;
mod mcfont;
mod mcmodel;
mod render;
mod resources;
mod site;

use std::path::PathBuf;

use anyhow::{Context, Result, bail};
use clap::Parser;

const ASSET_ROOTS: [&str; 2] = [
    "common/src/main/resources",
    "common/src/generated/resources",
];

#[derive(Parser)]
#[command(about, long_about = None)]
struct Arguments {
    /// The oc2 checkout (default: the one holding this tool)
    #[arg(long)]
    repo: Option<PathBuf>,
    #[arg(long)]
    output: Option<PathBuf>,
    #[arg(long, default_value = "en_us")]
    language: String,
    /// Default: the version catalog's
    #[arg(long)]
    minecraft_version: Option<String>,
    /// Skips the lookup and download
    #[arg(long)]
    minecraft_jar: Option<PathBuf>,
    #[arg(long)]
    cache: Option<PathBuf>,
    /// Empty the output directory first
    #[arg(long)]
    clean: bool,
}

fn main() -> Result<()> {
    let arguments = Arguments::parse();
    let repo = match arguments.repo {
        Some(repo) => repo,
        None => PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .and_then(std::path::Path::parent)
            .context("locating the repository")?
            .to_path_buf(),
    };
    let output = arguments
        .output
        .unwrap_or_else(|| repo.join("build").join("pages").join("manual"));
    let cache = arguments.cache.unwrap_or_else(|| {
        PathBuf::from(std::env::var("HOME").unwrap_or_default()).join(".cache/oc2-manual-html")
    });

    let version = match arguments.minecraft_version {
        Some(version) => version,
        None => resources::minecraft_version(&repo)?,
    };
    let jar = match arguments.minecraft_jar {
        Some(jar) => jar,
        None => resources::client_jar(&version, &cache)?,
    };
    if !jar.is_file() {
        bail!("no Minecraft client jar at {}", jar.display());
    }

    let roots: Vec<PathBuf> = ASSET_ROOTS.iter().map(|root| repo.join(root)).collect();
    for root in &roots {
        if !root.is_dir() {
            bail!("not an oc2 checkout, missing: {}", root.display());
        }
    }

    let doc_roots: Vec<PathBuf> = roots
        .iter()
        .map(|root| root.join("assets/oc2/doc"))
        .collect();
    if arguments.clean && output.exists() {
        std::fs::remove_dir_all(&output)?;
    }
    std::fs::create_dir_all(&output)?;

    let resources = resources::Resources::new(roots, &jar)?;
    site::SiteBuilder::new(&resources, doc_roots, arguments.language, output).build()
}
