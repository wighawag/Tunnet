---
title: Tailscale's anti-spoof rule is source-matched and drops the host's own traffic to any 100.64.0.0/10 address
type: finding
status: spotted
created: 2026-09-07
source: Measured on an affected Linux host running both Tunnet and Tailscale (osdns 0.1.3 era, September 2026), plus the rule text read directly from the live iptables ruleset. Packet counts from the host's own counters.
---

Tailscale claims `100.64.0.0/10` and installs:

```
-A ts-input -s 100.64.0.0/10 ! -i tailscale0 -j DROP
```

The match is on **source**, not destination. The consequence is not the obvious one: it also drops the host's *own* traffic to any address in that range, including over loopback, because the reply or the locally-originated packet carries a source inside the range while not arriving on `tailscale0`.

Measured on the affected host: 50 of 50 packets to the Tunnet mesh address were dropped, 50 of 50 packets to the MagicDNS resolver at `100.100.100.53` were dropped, and a `127.0.0.1` control was untouched. Tailscale's own resolver answered normally on the same box, because Tailscale exempts its own address on `lo` and Tunnet had no equivalent exemption.

Why it matters: the failure is completely silent on both sides. The resolver stays listening and simply never receives a query, so DNS fails with no error and no log line from either product. Both UIs report "connected". This cost a full investigation with a packet capture to identify, and any overlay sharing the CGNAT range will hit it.

Note that ICMP is not a reliable probe here. The host in question drops ICMP machine-wide, so the "ping works after `tailscale down`" reproduction from the original report is not reproducible there; a TCP probe against a real listener is required instead.

Refs: tunnetio/Tunnet#18, tunnetio/Tunnet#17 (collision detector), tunnetio/Tunnet#24 (moves PeerDNS to loopback and removes the derived-address range entirely).
