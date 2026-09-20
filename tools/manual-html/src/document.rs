use std::collections::{HashMap, HashSet};
use std::sync::LazyLock;

use fancy_regex::{Captures, Regex};

const LIST_PREFIXES: [&str; 2] = ["- ", "* "];
const REDIRECT_PRAGMA: &str = "#redirect ";

pub enum Node {
    Text(String),
    Code(Vec<Node>),
    Bold(Vec<Node>),
    Italic(Vec<Node>),
    Strikethrough(Vec<Node>),
    Header { level: usize, children: Vec<Node> },
    Link { url: String, children: Vec<Node> },
    Image { title: String, url: String },
}

pub struct Line {
    pub nodes: Vec<Node>,
    pub is_list_item: bool,
    pub header_level: usize,
}

impl Line {
    pub fn is_blank(&self) -> bool {
        self.nodes.is_empty()
    }
}

#[derive(Clone, Copy, PartialEq)]
enum Pass {
    Header,
    Code,
    Image,
    Link,
    Bold,
    Italic,
    Strikethrough,
}

static PASSES: LazyLock<Vec<(Pass, Regex)>> = LazyLock::new(|| {
    let compile = |pattern: &str| Regex::new(pattern).expect("pattern");
    vec![
        (Pass::Header, compile(r"^(#+)\s(.*)")),
        (Pass::Code, compile(r"(`)(.*?)\1")),
        (Pass::Image, compile(r"!\[([^\[]*)\]\(([^\)]+)\)")),
        (Pass::Link, compile(r"\[([^\[]+)\]\(([^\)]+)\)")),
        (Pass::Bold, compile(r"(\*\*|__)(\S.*?\S|$)\1")),
        (Pass::Italic, compile(r"(\*|_)(\S.*?\S|$)\1")),
        (Pass::Strikethrough, compile(r"~~(\S.*?\S|$)~~")),
    ]
});

pub fn redirect_target(source: &str) -> Option<String> {
    let first = source.lines().next().unwrap_or_default();
    let (pragma, target) = first.split_at_checked(REDIRECT_PRAGMA.len())?;
    pragma
        .eq_ignore_ascii_case(REDIRECT_PRAGMA)
        .then(|| target.trim().to_owned())
}

pub fn parse(source: &str) -> Result<Vec<Line>, fancy_regex::Error> {
    let mut raw: Vec<&str> = source.lines().map(str::trim_end).collect();
    let leading = raw.iter().take_while(|line| line.trim().is_empty()).count();
    raw.drain(..leading);
    while raw.last().is_some_and(|line| line.trim().is_empty()) {
        raw.pop();
    }

    raw.into_iter()
        .map(|line| {
            let text = line.trim_start();
            let nodes = refine(text, 0)?;
            let header_level = match nodes.first() {
                Some(Node::Header { level, .. }) => *level,
                _ => 0,
            };
            Ok(Line {
                nodes,
                is_list_item: LIST_PREFIXES.iter().any(|prefix| text.starts_with(prefix)),
                header_level,
            })
        })
        .collect()
}

fn refine(text: &str, index: usize) -> Result<Vec<Node>, fancy_regex::Error> {
    let Some((kind, pattern)) = PASSES.get(index) else {
        return Ok(if text.is_empty() {
            Vec::new()
        } else {
            vec![Node::Text(text.to_owned())]
        });
    };

    let mut result = Vec::new();
    let mut position = 0;
    for captures in pattern.captures_iter(text) {
        let captures = captures?;
        let whole = captures.get(0).expect("group 0");
        if whole.start() > position {
            result.extend(refine(&text[position..whole.start()], index + 1)?);
        }
        position = whole.end();
        result.push(node(*kind, &captures, index + 1)?);
    }

    if position == 0 {
        return refine(text, index + 1);
    }
    if position < text.len() {
        result.extend(refine(&text[position..], index + 1)?);
    }
    Ok(result)
}

fn node(
    kind: Pass,
    captures: &Captures<'_, str>,
    index: usize,
) -> Result<Node, fancy_regex::Error> {
    let group = |n: usize| captures.get(n).map(|m| m.as_str()).unwrap_or_default();
    Ok(match kind {
        Pass::Header => Node::Header {
            level: group(1).len(),
            children: refine(group(2), index)?,
        },
        Pass::Code => Node::Code(refine(group(2), index)?),
        Pass::Image => Node::Image {
            title: group(1).to_owned(),
            url: group(2).to_owned(),
        },
        Pass::Link => Node::Link {
            url: group(2).to_owned(),
            children: refine(group(1), index)?,
        },
        Pass::Bold => Node::Bold(refine(group(2), index)?),
        Pass::Italic => Node::Italic(refine(group(2), index)?),
        Pass::Strikethrough => Node::Strikethrough(refine(group(1), index)?),
    })
}

pub struct Links {
    pub pages: HashSet<String>,
    pub images: HashMap<String, String>,
    pub icons: HashMap<String, String>,
    pub sizes: HashMap<String, (u32, u32)>,
}

impl Links {
    pub fn resolve(&self, source: &str, url: &str) -> String {
        if let Some(absolute) = url.strip_prefix('/') {
            return normalize(absolute);
        }
        normalize(&format!("{}/{url}", parent(source)))
    }

    pub fn relative(&self, source: &str, target: &str) -> String {
        relative_path(&parent(source), target)
    }

    pub fn page_href(&self, source: &str, url: &str) -> (String, bool, bool) {
        if url.starts_with("http://") || url.starts_with("https://") {
            return (url.to_owned(), true, true);
        }
        let (path, fragment) = match url.split_once('#') {
            Some((path, fragment)) => (path, format!("#{fragment}")),
            None => (url, String::new()),
        };
        let target = self.resolve(source, path);
        let relative = self.relative(source, &target);
        let href = match relative.strip_suffix(".md") {
            Some(stem) => format!("{stem}.html{fragment}"),
            None => format!("{relative}{fragment}"),
        };
        (href, false, self.pages.contains(&target))
    }

    pub fn image_href(&self, source: &str, url: &str) -> Option<(String, Option<(u32, u32)>)> {
        let target = if url.contains(':') {
            self.icons.get(url).cloned()
        } else {
            self.images.get(&self.resolve(source, url)).cloned()
        }?;
        Some((
            self.relative(source, &target),
            self.sizes.get(&target).copied(),
        ))
    }
}

fn parent(path: &str) -> String {
    match path.rsplit_once('/') {
        Some((head, _)) => head.to_owned(),
        None => String::new(),
    }
}

fn normalize(path: &str) -> String {
    let mut parts: Vec<&str> = Vec::new();
    for part in path.split('/') {
        match part {
            "" | "." => {}
            ".." => {
                if matches!(parts.last(), Some(&last) if last != "..") {
                    parts.pop();
                } else {
                    parts.push("..");
                }
            }
            other => parts.push(other),
        }
    }
    parts.join("/")
}

fn relative_path(from: &str, to: &str) -> String {
    let from: Vec<&str> = from.split('/').filter(|part| !part.is_empty()).collect();
    let to: Vec<&str> = to.split('/').filter(|part| !part.is_empty()).collect();
    let shared = from.iter().zip(&to).take_while(|(a, b)| a == b).count();
    let mut parts: Vec<&str> = vec![".."; from.len() - shared];
    parts.extend(&to[shared..]);
    parts.join("/")
}

pub fn to_html(lines: &[Line], source: &str, links: &Links) -> String {
    let mut out = Vec::new();
    for line in lines {
        if line.is_blank() {
            out.push("<div class=\"blank\"></div>".to_owned());
            continue;
        }
        if line.header_level > 0 {
            let level = line.header_level.min(6);
            let Node::Header { children, .. } = &line.nodes[0] else {
                unreachable!()
            };
            out.push(format!(
                "<h{level}>{}</h{level}>",
                html(children, source, links)
            ));
            continue;
        }
        if line.nodes.len() == 1 && matches!(line.nodes[0], Node::Image { .. }) {
            out.push(format!(
                "<div class=\"figure\">{}</div>",
                node_html(&line.nodes[0], source, links)
            ));
            continue;
        }

        let classes = if line.is_list_item {
            "line list"
        } else {
            "line"
        };
        out.push(format!(
            "<div class=\"{classes}\">{}</div>",
            html(&line.nodes, source, links)
        ));
    }
    out.join("\n")
}

fn html(nodes: &[Node], source: &str, links: &Links) -> String {
    nodes
        .iter()
        .map(|node| node_html(node, source, links))
        .collect()
}

fn node_html(node: &Node, source: &str, links: &Links) -> String {
    match node {
        Node::Text(text) => escape(text),
        Node::Code(children) => format!("<code>{}</code>", html(children, source, links)),
        Node::Bold(children) => format!("<strong>{}</strong>", html(children, source, links)),
        Node::Italic(children) => format!("<em>{}</em>", html(children, source, links)),
        Node::Strikethrough(children) => format!("<s>{}</s>", html(children, source, links)),
        Node::Header { children, .. } => html(children, source, links),
        Node::Link { url, children } => {
            let (href, is_external, exists) = links.page_href(source, url);
            let classes = if exists { "" } else { " class=\"dead\"" };
            let external = if is_external {
                " target=\"_blank\" rel=\"noopener noreferrer\""
            } else {
                ""
            };
            format!(
                "<a href=\"{}\" title=\"{}\"{classes}{external}>{}</a>",
                escape(&href),
                escape(url),
                html(children, source, links)
            )
        }
        Node::Image { title, url } => {
            let title = escape(title);
            let Some((src, size)) = links.image_href(source, url) else {
                let text = if title.is_empty() {
                    "missing content"
                } else {
                    &title
                };
                return format!("<span class=\"missing\" title=\"{title}\">{text}</span>");
            };
            let style = if url.contains(':') { "icon" } else { "image" };
            let width = match size {
                Some((width, _)) => format!(" style=\"width:calc({width} * var(--u))\""),
                None => String::new(),
            };
            format!(
                "<img class=\"{style}\" src=\"{}\"{width} alt=\"{title}\" title=\"{title}\">",
                escape(&src)
            )
        }
    }
}

pub fn escape(text: &str) -> String {
    let mut out = String::with_capacity(text.len());
    for character in text.chars() {
        match character {
            '&' => out.push_str("&amp;"),
            '<' => out.push_str("&lt;"),
            '>' => out.push_str("&gt;"),
            '"' => out.push_str("&quot;"),
            '\'' => out.push_str("&#x27;"),
            other => out.push(other),
        }
    }
    out
}
