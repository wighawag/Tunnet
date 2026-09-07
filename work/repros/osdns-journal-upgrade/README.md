# osdns journal upgrade reproduction

Proves that an osdns journal record written by 0.1.3 locks out the 0.2.0-era manager entirely. Two crates share one `state_dir`: the producer is pinned to the released `=0.1.3`, the consumer to the git revision under test.

## Setup

A cargo workspace with two members. Producer `Cargo.toml`:

```toml
[dependencies]
osdns = { version = "=0.1.3", features = ["test-util"] }
serde_json = "1"
```

Consumer `Cargo.toml`:

```toml
[dependencies]
osdns = { git = "https://github.com/orielhaim/osdns.git", rev = "<rev>", features = ["test-util"] }
```

Sources are `producer-main.rs` and `consumer-main.rs` in this directory.

## Run

```
D=/tmp/osdns-state && rm -rf $D && mkdir -p $D
./target/debug/producer $D    # 0.1.3 applies a lease and leaks the record via mem::forget
./target/debug/consumer $D    # version under test tries to recover/abandon/apply
```

`std::mem::forget(lease)` skips the Drop that would restore and remove the record, which is a faithful stand-in for the process dying before lease drop.

## Results

At `8810f605`: all three calls fail with `missing field 'identity'`, a raw serde error that names no resource.

At `95efca6` (version 0.2.0): all three still fail, now with the typed `unsupported journal schema version 1 in <path> (supported: 3); clear or reset old osdns state before upgrading`. The diagnostic is fixed and names the file; the lockout is unchanged.
