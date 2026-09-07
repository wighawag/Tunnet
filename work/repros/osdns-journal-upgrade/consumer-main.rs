use osdns::testing::{FakeDns, manager_for_testing};
use osdns::{DnsConfig, DnsScope, InterfaceSelector};
use std::time::Duration;

fn main() {
    let dir = std::path::PathBuf::from(std::env::args().nth(1).unwrap());
    let fake = FakeDns::new();
    let manager = manager_for_testing("io.tunnet.agent", &dir, &fake, Duration::from_secs(30)).unwrap();
    match manager.recover_stale() {
        Ok(o) => println!("recover_stale  : OK {o:?}"),
        Err(e) => println!("recover_stale  : FAILED {e}"),
    }
    let rid: osdns::ResourceId = "fake:interface:1".parse().unwrap();
    match manager.abandon_journal(&rid) {
        Ok(n) => println!("abandon_journal: OK {n:?}"),
        Err(e) => println!("abandon_journal: FAILED {e}"),
    }
    let cfg = DnsConfig::builder(DnsScope::Interface(InterfaceSelector::Index(2)))
        .nameserver("9.9.9.9".parse().unwrap()).build().unwrap();
    match manager.apply(&cfg) {
        Ok(l) => { println!("apply (unrelated resource): OK"); std::mem::forget(l); }
        Err(e) => println!("apply (unrelated resource): FAILED {e}"),
    }
}
