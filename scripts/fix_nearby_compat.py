from pathlib import Path

path = Path("app/src/main/java/com/malik/ztesmartmanager/core/tower/TowerLockEngine.kt")
text = path.read_text(encoding="utf-8")
old = """    val sinr: Double?,
    val cellId: Long? = null,
    val areaCode: Int? = null,
    val samplesSeen: Int = 1,
    val samplesTotal: Int = 1,
    val presencePercent: Int = 100,
    val stabilityScore: Int? = null,
    val evidenceScore: Int? = null,
    val confidence: CellConfidence? = null
"""
new = """    val sinr: Double?,
    val samplesSeen: Int = 1,
    val samplesTotal: Int = 1,
    val presencePercent: Int = 100,
    val stabilityScore: Int? = null,
    val evidenceScore: Int? = null,
    val confidence: CellConfidence? = null,
    val cellId: Long? = null,
    val areaCode: Int? = null
"""
if old in text:
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
elif new not in text:
    raise SystemExit("NearbyCell block not found")
