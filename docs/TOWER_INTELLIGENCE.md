# Tower Intelligence 0.2.3

Tower Intelligence follows the same truth-first rule as the rest of the app: a configured target, a one-off neighbor report, or an HTTP acknowledgement is not treated as proof of a stable tower/cell.

## Scan identity

A radio cell is keyed by:

- RAT (LTE or NR)
- PCI
- EARFCN/ARFCN

Two cells on the same frequency remain separate when their PCI differs.

## Multi-sample scan

The default scan collects five read-only router samples. Each successful sample may contain the current LTE cell, structured neighbor-cell data, legacy MC801A `ngbr_cell_info`, or a combination of these fields. No missing cell is fabricated.

For every observed identity the engine calculates:

- median RSRP
- median RSRQ when available
- median SINR when available
- observation frequency across successful samples
- RSRP spread/stability when enough samples exist
- an evidence score based only on metrics that actually exist
- confidence (high, medium, low)

Missing RF metrics do not receive synthetic middle scores.

## Ranking

Ranking uses RF evidence, repeat observation/presence, and measured stability. Presence and stability cannot manufacture a score when no RF metric exists. A transient strong cell therefore does not automatically outrank a repeatedly observed stable cell.

## Locking

LTE cell locking still requires PCI + EARFCN and exact router read-back. After the write, the live serving cell must match the target before the app reports success. NR cells remain read-only until an exact, profile-gated NR cell-lock command is verified for the target firmware.

## Tower Guard

Tower Guard reacts only after repeated drift samples and retains the existing cooldown. It never sends a repair command when the live identity is unknown. Router-level cell lock remains the primary lock; the guard is a verified repair layer while the app is active.
