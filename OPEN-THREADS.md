# Open threads

Live state as of 2026-09-07. Unlike the notes in `work/notes/`, this file is meant to be edited and pruned as things close.

## Done: osdns 0.2.0

Published 2026-09-07. The changelog adopted both points raised upstream: per-record failures are now `RecoveryOutcome::Failed` with "callers must inspect returned outcomes", and 0.1.x state is documented as **intentionally not migrated**.

Handled in #25, which bumps the pin, matches every recovery outcome explicitly (the old `_ =>` catch-all would have logged a real `Failed` at debug and reported success), and clears pre-upgrade journal records by deleting the file named in the typed `UnsupportedJournalVersion` error. Verified against the published crate, not a git revision: without that migration, `recover_stale`, `abandon_journal` and `apply` all fail and DNS integration stays dead on any machine that crashed before upgrading.

Reproduction retained in `work/repros/osdns-journal-upgrade/` since it is the only way to exercise the cross-version path.

## Open PRs

- **#24** (`fix/direct-addressing-authority`) not ours; supersedes the closed #15, #19 and #20. Several of our decisions are gated on it landing.
- **#25** osdns 0.2.0 upgrade plus the pre-upgrade journal migration. Supersedes #21.
- **#21** osdns vanished-interface shim. Obsolete once #25 lands, but must not be closed before then: on 0.1.3 a vanished interface still disables DNS integration permanently.
- **#17** CGNAT collision detector plus the investigation write-up.
- **#13** Android app, rebased onto current main. Builds and links for `aarch64-linux-android`, never verified on a device. PeerDNS on Android remains unconfirmed: the bind failure documented in the closed #19 should be fixed by the loopback move in #24, but that is an inference.

## Deferred cleanup

Three fork branches are the only copies of work whose PRs were closed as superseded, so they were deliberately not deleted: `fix/magicdns-off-cgnat`, `fix/peerdns-duplicate-bind-retry`, `fix/connect-collision-index`. Delete them once #24 merges.

`apps/android/` in the code checkout holds roughly 541 MB of stale build output, untracked and ignored. Safe to delete at any time.

## Standing conventions

`main` is never modified. Branch from `upstream/main`, open a PR. Local `main` tracks `upstream/main` so it cannot drift; a stale fork `main` was what inflated seven PRs to roughly +13,800 lines each (see `work/notes/observations/stale-fork-main-inflated-every-pr.md`).

Recordings live on this orphan `work` branch, checked out as a separate worktree at `../Tunnet-work`, so the code tree is never disturbed.
