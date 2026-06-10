#!/usr/bin/env -S uv run --script
#
# /// script
# requires-python = ">=3.14"
# dependencies = [
#  "pygitscore",
#  "tree-sitter",
#  "tree-sitter-java",
#  "tree-sitter-sql",
#  "tree-sitter-javascript",
#  "tree-sitter-html",
#  "tree-sitter-css",
# ]
#
# [tool.uv.sources]
# pygitscore = { git = "ssh://git@lab.compute.dtu.dk/sse/pygitscore.git" }
# ///


import subprocess as sp
import tree_sitter as ts
import csv
import io

from pathlib import Path

import gitscore


def emit_tokens(parser, origin, rejected=frozenset()):
    tree = parser.parse(origin)
    cursor = tree.walk()
    godeeper = True
    while True:
        assert cursor.node is not None
        if godeeper and cursor.node.type not in rejected:
            if not cursor.goto_first_child():
                yield cursor.node
                godeeper = False
        elif cursor.goto_next_sibling():
            godeeper = True
        elif cursor.goto_parent():
            godeeper = False
        else:
            break


def encode_tokens(workdir, pattern, parser, output):
    rejected = {
        "line_comment",
        "ERROR",
        "block_comment",
        "import_declaration",
        "package_declaration",
    }

    for file in sorted(workdir.glob(pattern)):
        for token in emit_tokens(parser, file.read_bytes(), rejected):
            output.write(token.text)
            output.write(b"\0")


def source(workdir: Path, output):
    import tree_sitter_java as tsjava
    import tree_sitter_sql as tssql

    java_parser = ts.Parser(ts.Language(tsjava.language()))
    encode_tokens(workdir, "src/main/**/*.java", java_parser, output)

    sql_parser = ts.Parser(ts.Language(tssql.language()))
    encode_tokens(workdir, "src/main/**/*.sql", sql_parser, output)


def test_source(workdir: Path, output):
    import tree_sitter_java as tsjava

    java_parser = ts.Parser(ts.Language(tsjava.language()))
    encode_tokens(workdir, "src/test/**/*.java", java_parser, output)


def extra(workdir: Path, output):
    import tree_sitter_javascript as tsjavascript
    import tree_sitter_html as tshtml
    import tree_sitter_css as tscss

    javascript_parser = ts.Parser(ts.Language(tsjavascript.language()))
    encode_tokens(workdir, "src/**/*.js", javascript_parser, output)

    html_parser = ts.Parser(ts.Language(tshtml.language()))
    encode_tokens(workdir, "src/**/*.html", html_parser, output)

    css_parser = ts.Parser(ts.Language(tscss.language()))
    encode_tokens(workdir, "src/**/*.css", css_parser, output)


def test_coverage(api, target):
    while val := (yield):
        commit, result = val

        if "inst_covered" in result and "inst_total" in result:
            continue

        workdir = api.checkout(commit)

        output = sp.run(["mvn", "verify"], cwd=workdir)

        if output.returncode != 0:
            return 0

        covered = 0
        missed = 0
        with open(workdir / "target" / "site" / "jacoco" / "jacoco.csv") as f:
            reader = csv.DictReader(f)
            for line in reader:
                covered += int(line["INSTRUCTION_COVERED"])
                missed += int(line["INSTRUCTION_MISSED"])

        result["inst_covered"] = {"score": covered}
        result["inst_total"] = {"score": covered + missed}


if __name__ == "__main__":
    gitscore.main(
        judges=[
            gitscore.make_judge_from_encoder("source", source),
            gitscore.make_judge_from_encoder("extra", extra),
            gitscore.make_judge_from_encoder("test", test_source),
            # test_coverage,
        ]
    )
