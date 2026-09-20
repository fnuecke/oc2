use std::cell::RefCell;
use std::fmt;
use std::io::{Cursor, Read};
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};
use sha1::{Digest, Sha1};
use zip::ZipArchive;

const VERSION_MANIFEST: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
const TIMEOUT: u64 = 60;

#[derive(Clone, PartialEq, Eq, Hash, Debug)]
pub struct ResourceLocation {
    pub namespace: String,
    pub path: String,
}

impl ResourceLocation {
    pub fn new(namespace: &str, path: impl Into<String>) -> Self {
        Self {
            namespace: namespace.to_owned(),
            path: path.into(),
        }
    }

    pub fn parse(value: &str) -> Self {
        Self::parse_with(value, "minecraft")
    }

    pub fn parse_with(value: &str, default_namespace: &str) -> Self {
        match value.split_once(':') {
            Some((namespace, path)) => Self::new(
                if namespace.is_empty() {
                    default_namespace
                } else {
                    namespace
                },
                path,
            ),
            None => Self::new(default_namespace, value),
        }
    }
}

impl fmt::Display for ResourceLocation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}:{}", self.namespace, self.path)
    }
}

pub struct Resources {
    roots: Vec<PathBuf>,
    jar: RefCell<ZipArchive<Cursor<Vec<u8>>>>,
}

impl Resources {
    pub fn new(roots: Vec<PathBuf>, client_jar: &Path) -> Result<Self> {
        let bytes = std::fs::read(client_jar)
            .with_context(|| format!("reading {}", client_jar.display()))?;
        let jar = ZipArchive::new(Cursor::new(bytes))?;
        Ok(Self {
            roots,
            jar: RefCell::new(jar),
        })
    }

    pub fn find(&self, location: &ResourceLocation) -> Option<Vec<u8>> {
        let relative = format!("assets/{}/{}", location.namespace, location.path);
        for root in &self.roots {
            let candidate = root.join(&relative);
            if candidate.is_file() {
                return Some(std::fs::read(candidate).expect("reading a file that exists"));
            }
        }
        let mut jar = self.jar.borrow_mut();
        let Ok(mut entry) = jar.by_name(&relative) else {
            return None;
        };
        let mut bytes = Vec::new();
        entry
            .read_to_end(&mut bytes)
            .expect("reading a jar entry that exists");
        Some(bytes)
    }

    pub fn read(&self, location: &ResourceLocation) -> Result<Vec<u8>> {
        self.find(location)
            .with_context(|| format!("no such resource: {location}"))
    }

    pub fn read_json(&self, location: &ResourceLocation) -> Result<serde_json::Value> {
        let bytes = self.read(location)?;
        serde_json::from_slice(&bytes).with_context(|| format!("parsing {location}"))
    }
}

pub fn minecraft_version(repo: &Path) -> Result<String> {
    let path = repo.join("gradle").join("libs.versions.toml");
    let catalog =
        std::fs::read_to_string(&path).with_context(|| format!("reading {}", path.display()))?;
    for line in catalog.lines() {
        let Some(rest) = line.strip_prefix("minecraft") else {
            continue;
        };
        let Some(rest) = rest.trim_start().strip_prefix('=') else {
            continue;
        };
        let Some(value) = rest.trim_start().strip_prefix('"') else {
            continue;
        };
        if let Some(end) = value.find('"') {
            return Ok(value[..end].to_owned());
        }
    }
    bail!("no `minecraft` version in {}", path.display())
}

pub fn client_jar(version: &str, cache: &Path) -> Result<PathBuf> {
    let local = dirs_home()
        .join(".gradle/caches/fabric-loom")
        .join(version)
        .join("minecraft-client.jar");
    if local.is_file() {
        return Ok(local);
    }

    let target = cache.join(format!("minecraft-client-{version}.jar"));
    if target.is_file() {
        return Ok(target);
    }

    let manifest: serde_json::Value = fetch_json(VERSION_MANIFEST)?;
    let entry = manifest["versions"]
        .as_array()
        .context("version manifest has no `versions`")?
        .iter()
        .find(|v| v["id"] == version)
        .with_context(|| format!("Mojang's version manifest has no Minecraft {version}"))?;
    let meta: serde_json::Value = fetch_json(entry["url"].as_str().context("version url")?)?;
    let download = &meta["downloads"]["client"];
    let expected = download["sha1"].as_str().context("client sha1")?;

    println!("downloading Minecraft {version} client jar");
    let data = fetch(download["url"].as_str().context("client url")?)?;
    let digest: String = Sha1::digest(&data)
        .iter()
        .map(|b| format!("{b:02x}"))
        .collect();
    if digest != expected {
        bail!("client jar sha1 mismatch: got {digest}, expected {expected}");
    }

    std::fs::create_dir_all(cache)?;
    let staging = target.with_extension("part");
    std::fs::write(&staging, &data)?;
    std::fs::rename(&staging, &target)?;
    Ok(target)
}

fn dirs_home() -> PathBuf {
    std::env::var_os("HOME").map(PathBuf::from).expect("HOME")
}

fn agent() -> ureq::Agent {
    ureq::Agent::config_builder()
        .timeout_global(Some(std::time::Duration::from_secs(TIMEOUT)))
        .build()
        .into()
}

fn fetch(url: &str) -> Result<Vec<u8>> {
    let mut response = agent().get(url).call()?;
    let mut bytes = Vec::new();
    response.body_mut().as_reader().read_to_end(&mut bytes)?;
    Ok(bytes)
}

fn fetch_json(url: &str) -> Result<serde_json::Value> {
    Ok(serde_json::from_slice(&fetch(url)?)?)
}
