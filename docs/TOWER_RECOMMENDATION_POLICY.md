# Tower Recommendation Policy

The tower/cell recommendation layer is evidence-first and deliberately conservative.

## Non-negotiable rules

- A scan rank is not proof that a cell is better.
- Same EARFCN with a different PCI is a different LTE cell.
- Missing RF measurements are never replaced with synthetic values.
- A recommendation never causes an automatic lock.
- A candidate must be re-observed immediately before any lock write.
- A lock is successful only after exact router read-back and a matching live serving cell.
- A saved fingerprint is created only after a verified live lock. It is a radio identity, not a geographic tower location.

## Recommendation requirements

A recommended LTE candidate must have a complete PCI + EARFCN identity, measured RF evidence, at least medium scan confidence, at least 60% presence, and at least three observations. If the current cell is already within the configured score margin of the best candidate, the app recommends keeping the current cell rather than encouraging churn.

If evidence is missing, close, or contradictory, the result is `INSUFFICIENT_EVIDENCE` instead of guessing.

## Pre-lock revalidation

Before locking a scan result, the app performs a short read-only validation scan. The same PCI + EARFCN must be seen repeatedly with RF evidence. If it disappears or cannot be re-observed reliably, no write is sent.

## Saved fingerprints

The local fingerprint contains only radio identity/evidence needed to recognize a previously verified target. It does not store router passwords, auth/session tokens, IMSI/IMEI, or geographic coordinates. A saved fingerprint is never silently re-applied after reconnect; it must be revalidated first.
