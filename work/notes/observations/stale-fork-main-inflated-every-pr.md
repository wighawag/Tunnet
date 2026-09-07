---
title: A stale fork main made seven PRs look enormous and nearly re-introduced dropped work
type: observation
status: spotted
spotted: 2026-09-07
---

Every branch cut from `wighawag/Tunnet`'s `main` inherited 8 datapath commits that `tunnetio/Tunnet`'s `main` no longer had. The fork's `main` was pinned at `53d17cb7`, the old datapath tip, while upstream `main` had moved to `a1a58ff`.

The symptom was that PRs #13, #15, #16, #17, #19, #20 and #21 each showed roughly +13,800 lines across ~48 files, when the actual fixes were between 69 and 689 lines across 1 to 6 files. Every one of them was proposing to re-add the 8 dropped commits as a side effect of its base, which is invisible in the PR description and easy to merge by accident.

What made it hard to see: the diffs looked plausible. A large PR is not obviously a broken PR, and the extra commits had legitimate-looking messages. It only surfaced when the file list for a one-line DNS fix included `crates/tunnet-core/src/scheduler.rs`.

Fixed by rebasing each branch onto `upstream/main` and dropping the 8 commits, then pointing the fork's `main` at `upstream/main` and setting local `main` to track `upstream/main` rather than `origin/main`, so it follows upstream by default and cannot silently drift again.

The datapath work itself was never at risk: it lives on `tunnetio/Tunnet`'s `dataplane-rework` branch with identical SHAs, and has since moved 5 commits ahead of the copy that was riding along in #13. So the copy being dropped from those PRs was already stale.

Lesson worth keeping: when a PR's file list contains files unrelated to its stated purpose, check the merge base before reading the diff. `git log --oneline $(git merge-base HEAD upstream/main)..HEAD` answers in one command what reading a 14,000-line diff does not.
