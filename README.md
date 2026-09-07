# Tunnet work branch

Recordings for the Tunnet project: observations, findings, and ideas that should survive a conversation without touching the code repository.

This branch is an **orphan**: it shares no ancestry with `main`. `git merge-base work main` returns nothing, so its contents can never appear in a diff or a pull request against `main`, and it cannot be merged by accident. That is the point. The code repo keeps the rule "never modify `main`, only branches and PRs", and this branch keeps notes out of that flow entirely.

## Working in it

It is checked out as a separate worktree, so the code checkout is never disturbed:

```
git worktree add ../Tunnet-work work    # once
cd ../Tunnet-work                       # write notes here
```

The code tree stays on whatever feature branch you are using, with a clean status, while notes are written and committed here.

## Layout

| Path | Holds | Mutability |
| --- | --- | --- |
| `work/notes/observations/` | spotted but unverified, including agent/harness conduct signals | append-only |
| `work/notes/findings/` | verified external or domain ground truth, `source:` required | accumulates |
| `work/notes/ideas/` | proposed enhancements, pre-spec | editable |

Bucket routing follows the `capture-signal` skill. The distinction that matters most: an internal investigation of our own code is an **observation**, while a **finding** records verified behaviour of the outside world and must carry provenance. A decision we made and reasoned through is an ADR, and ADRs are durable project record rather than a note, so they belong in the code repo through a PR, not here.

## Conventions

Content-derived slugs, never counters. Frontmatter carries `title`, `type`, `status`, and a date. Findings additionally carry `source:` stating what the evidence is and how current it is. Observations are append-only: add an `## Update` block rather than rewriting what was first seen.

Keep this branch text-only. Everything under `refs/heads` is fetched on every clone, so binaries and captures here would inflate the repository permanently. Large artifacts belong in a sidecar repo or in release assets.
