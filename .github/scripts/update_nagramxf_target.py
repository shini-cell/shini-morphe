#!/usr/bin/env python3
"""Append the latest NagramXF release to the patch compatibility targets."""

import re
import sys
from pathlib import Path


if len(sys.argv) != 4:
    raise SystemExit("Usage: update_nagramxf_target.py <release-name> <tag> <commit-sha>")

release_name, release_tag, commit_sha = sys.argv[1:]
match = re.fullmatch(r"(\d+(?:\.\d+)*)\.(\d+)", release_name)
if not match:
    raise SystemExit(f"Unexpected NagramXF release name: {release_name!r}")

app_version, version_code = match.groups()
if release_tag != version_code:
    raise SystemExit(
        f"Release tag {release_tag!r} does not match version code {version_code!r}; refusing to guess."
    )
if not re.fullmatch(r"[0-9a-fA-F]{40}", commit_sha):
    raise SystemExit(f"Unexpected release commit SHA: {commit_sha!r}")

target_version = f"{app_version}-{commit_sha[:7]}"
constants_path = Path(
    "patches/src/main/kotlin/app/shinicell/morphepatches/patches/shared/Constants.kt"
)
source = constants_path.read_text(encoding="utf-8")
targets_marker = "targets = listOf("
compatibility_marker = "val COMPATIBILITY_NAGRAM_XF"
compatibility_start = source.index(compatibility_marker)
list_start = source.index(targets_marker, compatibility_start)
list_open = source.index("(", list_start)

# Find the end of this listOf(...) block, accounting for nested AppTarget/mapOf calls.
depth = 0
list_close = None
for index in range(list_open, len(source)):
    if source[index] == "(":
        depth += 1
    elif source[index] == ")":
        depth -= 1
        if depth == 0:
            list_close = index
            break
if list_close is None:
    raise SystemExit("Could not find the end of Nagram XF's targets list.")

targets_source = source[list_open + 1 : list_close]
escaped_version = re.escape(target_version)
if re.search(rf'AppTarget\s*\(\s*version\s*=\s*"{escaped_version}"', targets_source):
    print(f"NagramXF {target_version} (code {version_code}) is already supported.")
    raise SystemExit(0)

new_target = (
    "\n            AppTarget(\n"
    f'                version = "{target_version}",\n'
    f"                versionCodes = mapOf(SupportedAbi.ARM64_V8A to {version_code}),\n"
    "            ),"
)
updated_source = source[:list_close] + new_target + source[list_close:]
constants_path.write_text(updated_source, encoding="utf-8", newline="\n")
print(f"Added NagramXF {target_version} (ARM64 version code {version_code}).")
