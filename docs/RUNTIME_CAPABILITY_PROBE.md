# Runtime firmware capability probe

The static router profile is a compatibility hint, not proof that a write is safe on the connected firmware.

Before LTE band, NR band, LTE cell lock/unlock, or network-mode writes, the client performs a fresh read-only probe. A write can proceed only when the router exposes the exact state needed for read-back and rollback.

## States

- `SAFE_TO_ATTEMPT`: the profile permits the feature and the connected firmware exposes a complete, restorable read-back surface. This does **not** mean the requested write has succeeded; success is still declared only after post-write read-back matches.
- `READ_ONLY`: related runtime fields exist, but the current values are incomplete, blank/zero where no safe AUTO value is known, conflicting, or otherwise not safely restorable.
- `PROFILE_ONLY`: the model profile knows the feature, but this firmware has not exposed the required runtime evidence.
- `UNAVAILABLE`: neither a safe write path nor a usable runtime surface has been established.

## Write rules

`Configured != Active` and `HTTP success != Verified` remain unchanged.

1. Probe first, without mutating the router.
2. Refuse the write if rollback/read-back prerequisites are not established.
3. Capture the exact pre-write safety backup.
4. Send the command.
5. Require exact post-write read-back.
6. For cell lock, also require the live serving cell to match before the UI may call the lock verified.

## Antenna control

The MC801A profile knows the legacy antenna command, but the current application does not have a trustworthy read-back/rollback contract for it. Therefore antenna writes are blocked in 0.2.7 rather than being sent and presented as an unverified partial success.

## Probe data

The probe requests radio configuration and telemetry fields only. It does not request or store router passwords, authentication material, IMSI, or IMEI.
