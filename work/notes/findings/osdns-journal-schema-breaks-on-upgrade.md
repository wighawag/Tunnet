---
title: osdns records written by 0.1.3 lock out the post-fix version entirely, including apply() and abandon_journal()
type: finding
status: spotted
created: 2026-09-07
source: Two-crate reproduction run 2026-09-07 against osdns commit 8810f605 (the code staged for the release that fixes orielhaim/osdns#2), with the producer pinned to `=0.1.3` from crates.io. Both crates used `test-util` and `manager_for_testing`, sharing one `state_dir`.
---

`JournalRecord` gained a required `identity` field and `SCHEMA_VERSION` went from 1 to 3. A record written by 0.1.3 therefore fails to deserialize, and the failure is not contained:

```
RECOVER FAILED: journal is corrupt: .../ef6d08c2...json: missing field `identity`
abandon_journal ESCAPE FAILED: journal is corrupt: .../ef6d08c2...json: missing field `identity`
```

`apply()` fails with the same error, confirmed by seeding a healthy current-schema record for an unrelated resource and watching the call fail before reaching the backend. So after upgrading: cannot apply, cannot recover, cannot abandon. The only remedy is deleting the file by hand, which is exactly the remedy from the original bug report.

Three causes compound:

1. Deserialization fails before the version check, so the friendly "unsupported journal schema version 1" diagnostic is unreachable for precisely the records it describes.
2. `records()` aborts its whole read on the first bad file, and `recover_stale` opens with `journal.records()?`. The upstream fix for "one bad record aborts the pass" was applied one layer up, in `recover_record`, so the original defect survives underneath it.
3. The filename scheme changed from `{lease}-{slug}.json` to `{lease}-{slug}-{stable_hash}.json`, and `slug()` changed `:` from `_` to `+`. Old files are not addressable by the new path derivation even once parsing is fixed.

Second, quieter behaviour change in the same release: `recover_stale()` no longer returns `Err` for a failing record, it returns `Ok` with `RecoveryOutcome::Failed` in the vec. A caller written `recover_stale()?` that ignores the returned outcomes keeps compiling and silently swallows failures it previously surfaced. Tunnet is such a caller.

Impact on us: do not bump the osdns pin on release day. The machines carrying leftover records are the machines that hit the original bug, which is to say our affected users. Upgrading blindly converts a DNS-integration failure into a total lockout of the manager including `apply()`, which is strictly worse than the current state.

Refs: orielhaim/osdns#2, tunnetio/Tunnet#21 (carries the downstream `NoSuchLink` shim and the revised upgrade gate).

## Update, 2026-09-07 (re-verified against `95efca6`, version 0.2.0)

The maintainer added `7f821ac fix: reject unsupported journal schema versions explicitly`. `records()` now parses a `JournalEnvelope` first and returns a typed `Error::UnsupportedJournalVersion { path, found, supported }` before attempting the full record, so the diagnostic is reachable and names the file:

```
unsupported journal schema version 1 in <path> (supported: 3);
clear or reset old osdns state before upgrading
```

The lockout itself is unchanged. Re-running the reproduction against `95efca6`, `recover_stale()`, `abandon_journal()` and `apply()` all still fail. `records()` still returns `Err` from inside its loop, so one legacy file still blocks every unrelated healthy record, and `abandon_journal` still cannot serve as the escape hatch.

Read charitably, this is now a deliberate upgrade contract rather than an oversight: the version moved to 0.2.0 rather than 0.1.4, so the upgrade is explicit rather than automatic, and the error tells the operator to clear old state. That is defensible. It does mean the migration burden moved to every downstream, and ours is not written yet.

Because the error carries `path`, a downstream can now recover programmatically by deleting the file it names. That is the shape our migration should take, rather than blindly wiping the journal directory.
