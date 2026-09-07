// Simulates osdns 0.1.3 leaving a journal record behind (process died before
// the lease was dropped) - exactly the scenario from orielhaim/osdns#2.
use osdns::testing::{FakeDns, manager_for_testing};
use osdns::{DnsConfig, DnsScope, InterfaceSelector};
use std::time::Duration;

fn main() {
    let dir = std::path::PathBuf::from(std::env::args().nth(1).unwrap());
    let fake = FakeDns::new();
    let manager = manager_for_testing("io.tunnet.agent", &dir, &fake, Duration::from_secs(30)).unwrap();
    let cfg = DnsConfig::builder(DnsScope::Interface(InterfaceSelector::Index(1)))
        .nameserver("1.1.1.1".parse().unwrap())
        .build()
        .unwrap();
    let lease = manager.apply(&cfg).unwrap();
    // Never drop it: the process "crashes" and the record stays on disk.
    std::mem::forget(lease);
    let j = dir.join("journal");
    for e in std::fs::read_dir(&j).unwrap().flatten() {
        let bytes = std::fs::read(e.path()).unwrap();
        let v: serde_json::Value = serde_json::from_slice(&bytes).unwrap();
        println!("PRODUCED by 0.1.3: {}", e.file_name().to_string_lossy());
        println!("  schema_version = {}", v["schema_version"]);
        println!("  has identity field = {}", v.get("identity").is_some());
    }
}
