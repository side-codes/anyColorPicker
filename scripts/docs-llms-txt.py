"""Write llms.txt (https://llmstxt.org) into the built documentation site.

    python scripts/docs-llms-txt.py [site dir]

Its sections and their order are mkdocs.yml's nav, and each page's title and description come from the page's
front matter, so llms.txt lists what the sidebar lists, described as a search result describes it. The site
directory defaults to mkdocs.yml's site_dir, or site/.
"""

import pathlib
import sys

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent


class ConfigLoader(yaml.SafeLoader):
    """Reads mkdocs.yml without running it: a `!!python/name:` reference (a Markdown extension's hook) stays a string."""


ConfigLoader.add_multi_constructor("tag:yaml.org,2002:python/name:", lambda loader, suffix, node: suffix)


def front_matter(page: pathlib.Path) -> dict:
    text = page.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        raise SystemExit(f"{page}: a page needs front matter with its title and description")
    return yaml.safe_load(text.split("---\n", 2)[1])


def main() -> None:
    config = yaml.load((ROOT / "mkdocs.yml").read_text(encoding="utf-8"), Loader=ConfigLoader)
    site = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / config.get("site_dir", "site")
    docs = ROOT / config.get("docs_dir", "docs")
    base = config["site_url"]

    lines = [
        f"# {config['site_name']}",
        "",
        f"> {config['site_description']}",
        "",
        f"{config['extra']['llms_txt_summary']} Source and issues: {config['repo_url']}",
    ]
    for entry in config["nav"]:
        ((section, pages),) = entry.items()
        if isinstance(pages, str):
            continue  # the home page, which llms.txt itself stands in for
        lines += ["", f"## {section}", ""]
        for name in pages:
            if not isinstance(name, str):
                raise SystemExit(f"nav: list {name} by its file name; its title belongs in its front matter")
            meta = front_matter(docs / name)
            lines.append(f"- [{meta['title']}]({base}{name.removesuffix('.md')}.html): {meta['description']}")

    with open(site / "llms.txt", "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
