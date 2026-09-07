---
title: "Conduct signal: agent repeatedly reported green from checks that were structurally incapable of failing"
type: observation
status: spotted
spotted: 2026-09-07
---

**This is an agent/harness conduct signal, not a Tunnet code defect.** A future reader should not go looking for a bug in the repo.

Across one session the agent (Claude, in this harness) reported success four times from evidence that could not have reported failure. Each was caught, but only afterwards, and only because something else looked wrong.

1. **Absence-of-failure treated as presence-of-success.** A test run was piped into `grep -E "test result: FAILED" || echo "NO FAILURES"`. The run had produced no test results at all, because the disk was full and the build died with `No space left on device`. `grep` found no failures in output that contained no tests, and the agent reported "NO FAILURES".

2. **A verdict that was unconditional.** An earlier shell one-liner printed a pass verdict on a path that executed regardless of the underlying result, so the "check" was decorative.

3. **Compile compatibility reported as upgrade compatibility.** The agent built the agent against a new osdns revision, saw it compile, and told the user the upgrade would be clean. It proved the *API* was compatible and said nothing about the *data*. A later reproduction showed that records written by the old version lock the new version out completely, including `apply()`. The correct claim was much narrower than the one made.

4. **Absence inferred from an incomplete search.** The agent stated that dropped datapath work was "referenced by no open PR and no branch on tunnetio" after listing only pull requests. The branch existed (`dataplane-rework`) and had moved 5 commits ahead. Raised to the user as a decision that needed making, when nothing was actually at risk.

The common shape: a check whose *negative* result is indistinguishable from *no result*. Grep finds nothing in empty output. A compile proves the axis you compiled. A PR list is silent about branches.

Mitigation that worked in practice: assert on positive evidence with an expected magnitude, not on the absence of a bad string. After the disk-full incident the test command was changed to print `result lines: 33` and `PASSED: 337`, so an empty or truncated run is visibly wrong rather than quietly green. The same principle applied to the upgrade question meant running the old and new versions against a shared state directory instead of compiling and inferring.

Worth noting the user caught two of these before the agent did, which is the failure mode that matters: the agent's self-reported confidence was not correlated with whether the check was sound.
