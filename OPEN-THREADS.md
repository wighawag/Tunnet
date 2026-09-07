# Open threads

Live state as of 2026-09-07. Unlike the notes in `work/notes/`, this file is meant to be edited and pruned as things close.

## Blocking on someone else

**osdns release.** The maintainer fixed orielhaim/osdns#2 and said he planned to publish "in about an hour" on 2026-09-07. As of the last check crates.io is still at 0.1.3, while `master` carries `version = "0.2.0"` at commit `95efca6`. The 0.x-major bump means our `^0.1.3` pin will not pick it up automatically, so nothing changes for us until we act.

**Do not bump the osdns pin without a migration step.** Verified at `95efca6` with the reproduction in `work/repros/osdns-journal-upgrade/`: a journal record written by 0.1.3 makes `recover_stale()`, `abandon_journal()` and `apply()` all fail. The manager is unusable until the file is deleted by hand. The error is now typed and names the path, which is a real improvement, but it is still a hard failure, and `abandon_journal` (the documented escape) cannot escape it.

The practical consequence for Tunnet: the machines carrying leftover records are the machines that hit the original bug, so this hits our affected users specifically. Before upgrading, the agent needs to either clear the old journal directory on version change, or catch the typed error and remove the path it names. That work does not exist yet and is not tracked by any issue.

## Open PRs

- **#24** (`fix/direct-addressing-authority`) not ours; supersedes the closed #15, #19 and #20. Several of our decisions are gated on it landing.
- **#21** osdns vanished-interface shim. Still needed while osdns is unreleased. Carries the revised upgrade gate in a comment: bump, delete `is_vanished_resource` and its three tests, and start inspecting `RecoveryOutcome::Failed`, which we currently ignore.
- **#17** CGNAT collision detector plus the investigation write-up.
- **#13** Android app, rebased onto current main. Builds and links for `aarch64-linux-android`, never verified on a device. PeerDNS on Android remains unconfirmed: the bind failure documented in the closed #19 should be fixed by the loopback move in #24, but that is an inference.

## Deferred cleanup

Three fork branches are the only copies of work whose PRs were closed as superseded, so they were deliberately not deleted: `fix/magicdns-off-cgnat`, `fix/peerdns-duplicate-bind-retry`, `fix/connect-collision-index`. Delete them once #24 merges.

`apps/android/` in the code checkout holds roughly 541 MB of stale build output, untracked and ignored. Safe to delete at any time.

## Standing conventions

`main` is never modified. Branch from `upstream/main`, open a PR. Local `main` tracks `upstream/main` so it cannot drift; a stale fork `main` was what inflated seven PRs to roughly +13,800 lines each (see `work/notes/observations/stale-fork-main-inflated-every-pr.md`).

Recordings live on this orphan `work` branch, checked out as a separate worktree at `../Tunnet-work`, so the code tree is never disturbed.
