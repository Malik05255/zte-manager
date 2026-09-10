# ZTE Manager — Truth-First Architecture

## Product rule

The application must never promote a configured, cached, inferred, or requested radio state as a live fact.

Every user-facing statement belongs to one of three classes:

1. **Verified live** — may be shown as a fact.
2. **Configured / desired** — must be labelled as an allowed/locked/requested setting, not an active state.
3. **Unknown / unverified** — must be shown as unknown rather than guessed.

## What was learned from other CPE projects

This design synthesizes mechanisms, not source code, from mature Huawei, ZTE, Quectel/OpenWrt and generic cellular-monitoring projects.

### Huawei monitoring projects

Useful ideas:
- keep raw modem responses available for firmware diagnostics;
- treat PCC/SCC/NR cells as separate observed carriers;
- collect history rather than trusting one RF sample;
- expose neighbor cells when the firmware actually provides them;
- normalize signal metrics within physically plausible ranges.

Rejected behavior:
- never infer Carrier Aggregation from a multi-band configuration mask. A router being *allowed* B1+B3 does not prove that B1 and B3 are simultaneously active.

### Quectel/OpenWrt tooling

Useful ideas:
- separate desired configuration from observed modem state;
- declarative re-application of cell/band locks;
- watchdogs require several degraded/drifted samples before recovery;
- use cooldown/recovery windows to prevent ping-pong;
- LTE, NR NSA and NR SA are separate control domains.

### ZTE tooling

Useful ideas:
- on legacy MC801A-class firmware, verified LTE cell lock is PCI + EARFCN through `LTE_LOCK_CELL_SET`;
- on newer goform firmware, `neighbor_cell_info`, `current_cell_info`, and `locked_cell_info` may expose structured cells;
- some newer ZTE firmware supports `CELL_LOCK` with RAT + PCI + EARFCN, but this command must not be sent to MC801A unless that exact firmware capability is positively identified;
- cached SCell/NR fields exist on some firmware, so explicit live-state flags must outrank stale payload fields;
- successful POST response is only acknowledgement. A setting is verified only when read-back matches.

## Verified 5G rule

The UI may say **5G active** only when both are true in the same current poll:

- the router reports an explicit active 5G radio state (for example EN-DC/ENDC for NSA, or an explicit SA/5G state), and
- a complete live NR carrier signature exists: valid NR band + NR ARFCN + NR PCI + valid NR RSRP.

Stale NR band, PCI, Cell ID, or RSRP values by themselves are insufficient.

If the explicit state says LTE-NSA/NSA-ready but NR is not attached, the UI must not say 5G is active.

If evidence conflicts or is incomplete, show **Unverified / غير مؤكد**.

## Verified Carrier Aggregation rule

The UI may say **CA active** only when an explicit live activation field reports active.

The UI may say **two (or more) bands are actually aggregated** only when:

- CA is explicitly active, and
- the current poll contains a primary LTE carrier plus one or more unique active LTE secondary carriers.

Configured masks such as B1+B3 are displayed only as **Allowed/Configured bands**.

Cached `lte_multi_ca_scell_info` must never override an explicit deactivated CA state.

## Verified tower/cell lock rule

For MC801A legacy locking:

- target = current verified LTE PCI + EARFCN;
- Cell ID/eNodeB are stored as additional identity evidence;
- send `LTE_LOCK_CELL_SET`;
- do not claim success from HTTP acknowledgement;
- read back `lte_pci_lock` + `lte_earfcn_lock` and require an exact match;
- then compare the live serving PCI/EARFCN and, when available, Cell ID/eNodeB;
- only after both stages pass may the UI say the target is locked and enable Tower Guard.

If PCI/EARFCN match but Cell ID/eNodeB changes, the app must explicitly say that physical-tower identity cannot be guaranteed by this firmware.

## Tower Guard

Tower Guard is a desired-state watchdog, not a proof generator.

- poll live serving-cell identity;
- one anomalous sample does nothing;
- require 3 consecutive drift samples before repair;
- apply a cooldown between repair attempts;
- a repair is successful only if the lock command itself is read-back verified;
- when evidence is unavailable, do nothing rather than send blind commands.

## Smart Optimizer

The optimizer follows this transaction:

1. Capture original configured state.
2. Collect a multi-sample live baseline.
3. Measure network performance.
4. Generate a bounded candidate list.
5. Apply candidate.
6. Require exact configuration read-back.
7. Allow radio to settle.
8. Collect multiple live RF samples.
9. Reject candidate if observed active LTE carriers fall outside its allowed set.
10. Score measured RF + latency/jitter/loss/download using only available evidence.
11. Give a CA bonus only for verified live CA with multiple observed LTE carriers.
12. Require a meaningful improvement threshold.
13. Apply the winner again and perform final read-back + live validation.
14. Roll back to the original configuration on any failed final verification.

A failed internet probe does not fabricate 100% packet loss; missing data is simply excluded from the weighted score.

## Signal scoring

- no missing metric receives a synthetic average value;
- available metrics are dynamically reweighted;
- multi-sample medians reduce transient spikes;
- stability is calculated only with enough samples;
- serving-cell changes carry a stability penalty;
- the score becomes `Unknown` when no trustworthy RF evidence exists.

## Firmware capability policy

Every router profile must separately declare:

- readable fields;
- verified write commands;
- write parameters;
- read-back fields;
- encoding rules (hex vs decimal);
- whether a setting survives reboot;
- whether neighbor/current/locked cell arrays exist;
- whether LTE, NR NSA and NR SA locking are independently supported.

Unsupported or unverified commands stay disabled. Feature discovery is read-only until a command has been confirmed for that model/firmware.

## UI language policy

Preferred labels:

- `5G NSA / 4G — Verified`
- `5G SA — Verified`
- `4G — Verified`
- `CA active — B3 + B7` only with live PCC/SCC evidence
- `Configured bands: B3 + B7` when this is only a mask
- `Lock saved and verified` only after read-back
- `Unverified / غير مؤكد` whenever evidence is incomplete

Never use words equivalent to connected, active, aggregated, locked, fixed, or selected-tower as facts without the corresponding verification path above.
