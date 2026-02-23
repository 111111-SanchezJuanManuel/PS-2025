#!/usr/bin/env python3
"""
Remove commented lines and inline comments from source files under EscenaLocal.

Usage:
  python tools/remove_comments.py --path EscenaLocal

This script creates a backup for each modified file with extension `.bak`.
It skips directories: .angular, target, node_modules, .git, dist

Supported comment patterns:
- Line comments: // (TS/JS/Java/C-like), -- (SQL), # (YAML/properties)
- Block comments: /* ... */ (multi-line)
- HTML/XML comments: <!-- ... --> (multi-line)

WARNING: This is a destructive operation. Commit or backup your repo before running.
"""
import argparse
import io
import os
import re
import shutil

SKIP_DIRS = {'.angular', 'target', 'node_modules', '.git', 'dist'}
TEXT_EXTS = {
    '.ts', '.js', '.java', '.css', '.html', '.xml', '.yml', '.yaml',
    '.properties', '.json', '.scss', '.less', '.txt'
}


def should_skip(path):
    parts = set(p for p in path.split(os.sep) if p)
    return bool(parts & SKIP_DIRS)


def remove_comments_from_text(text, ext):
    # State machine to remove block comments (/* */) and HTML comments (<!-- -->)
    out_lines = []
    in_c_block = False
    in_html_block = False
    for line in text.splitlines():
        orig = line
        i = 0
        if in_c_block:
            end = line.find('*/')
            if end != -1:
                line = line[end+2:]
                in_c_block = False
            else:
                continue
        if in_html_block:
            end = line.find('-->')
            if end != -1:
                line = line[end+3:]
                in_html_block = False
            else:
                continue

        while True:
            idx_c = line.find('/*')
            idx_html = line.find('<!--')
            idx_slash = line.find('//')
            # for YAML/properties, '#' is a comment only for .yml/.yaml/.properties
            idx_hash = line.find('#') if ext in ('.yml', '.yaml', '.properties') else -1
            idx_dash = line.find('--') if ext == '.sql' else -1

            # find earliest positive index
            candidates = [(idx, kind) for idx, kind in [(idx_c, 'c'), (idx_html, 'html'), (idx_slash, 'slash'), (idx_hash, 'hash'), (idx_dash, 'dash')] if idx != -1]
            if not candidates:
                break
            idx, kind = min(candidates, key=lambda x: x[0])
            if kind == 'c':
                end = line.find('*/', idx+2)
                if end != -1:
                    line = line[:idx] + line[end+2:]
                    continue
                else:
                    line = line[:idx]
                    in_c_block = True
                    break
            elif kind == 'html':
                end = line.find('-->', idx+4)
                if end != -1:
                    line = line[:idx] + line[end+3:]
                    continue
                else:
                    line = line[:idx]
                    in_html_block = True
                    break
            elif kind == 'slash':
                # remove everything after //
                # naive: this will remove URLs in strings too
                line = line[:idx]
                break
            elif kind == 'hash':
                line = line[:idx]
                break
            elif kind == 'dash':
                line = line[:idx]
                break

        # Trim right-side whitespace
        if line.strip() == '':
            continue
        out_lines.append(line.rstrip())

    return '\n'.join(out_lines) + ('\n' if out_lines else '')


def process_file(path, repo_root):
    _, ext = os.path.splitext(path)
    if ext.lower() not in TEXT_EXTS:
        return False
    full = os.path.join(repo_root, path)
    try:
        with io.open(full, 'r', encoding='utf-8') as f:
            src = f.read()
    except Exception:
        return False
    new = remove_comments_from_text(src, ext.lower())
    if new != src:
        bak = full + '.bak'
        shutil.copy2(full, bak)
        with io.open(full, 'w', encoding='utf-8') as f:
            f.write(new)
        return True
    return False


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--path', default='EscenaLocal', help='Path to the project folder to clean')
    args = p.parse_args()
    repo_root = os.getcwd()
    target = os.path.join(repo_root, args.path)
    if not os.path.isdir(target):
        print('Path not found:', target)
        return
    modified = []
    for root, dirs, files in os.walk(target):
        # prune skipped dirs
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for fn in files:
            rel = os.path.relpath(os.path.join(root, fn), repo_root)
            if should_skip(rel):
                continue
            if process_file(rel, repo_root):
                modified.append(rel)

    print('Modified files:', len(modified))
    for m in modified:
        print(' -', m)


if __name__ == '__main__':
    main()
