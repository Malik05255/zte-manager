package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot

enum class CarrierMatrixKind { LTE_PRIMARY, LTE_SECONDARY, NR }

data class CarrierMatrixRow(
    val kind: CarrierMatrixKind,
    val roleLabel: String,
    val band: String?,
    val pci: Int?,
    val arfcn: Int?,
    val bandwidthMhz: Double?
)

data class CarrierMatrixModel(
    val rows: List<CarrierMatrixRow>,
    val lteVerified: Boolean,
    val nrVerified: Boolean,
    val caVerified: Boolean,
    val caActive: Boolean,
    val lteCarrierCount: Int,
    val nrCarrierCount: Int,
    val caHeadline: String,
    val evidenceMessage: String
) {
    val hasVerifiedCarrier: Boolean get() = rows.isNotEmpty()
}

/**
 * Truth-first projection of the parser's verified carrier contract.
 *
 * This presenter never reads configured band masks. It additionally gates snapshot.cells with the
 * parser's verification flags so an inconsistent/cached cell cannot leak into the UI:
 * - LTE PCell requires verified live LTE.
 * - LTE SCells require verified live LTE plus verified ACTIVE CA.
 * - NR carriers require verified live NR.
 */
object CarrierMatrixPresenter {
    fun from(snapshot: RouterSnapshot): CarrierMatrixModel {
        val lteVerified = flag(snapshot, "_zte_lte_active_verified")
        val nrVerified = flag(snapshot, "_zte_nr_active_verified")
        val caVerified = flag(snapshot, "_zte_ca_verified")
        val caActive = caVerified && flag(snapshot, "_zte_ca_active") && snapshot.caActive

        val safeCells = snapshot.cells.filter { cell ->
            when (cell.role) {
                CellRole.PRIMARY -> lteVerified
                CellRole.SECONDARY -> lteVerified && caActive
                CellRole.NR -> nrVerified
            }
        }

        var lteSecondaryIndex = 0
        var nrIndex = 0
        val rows = safeCells.map { cell ->
            when (cell.role) {
                CellRole.PRIMARY -> row(cell, CarrierMatrixKind.LTE_PRIMARY, "LTE PCell")
                CellRole.SECONDARY -> {
                    lteSecondaryIndex += 1
                    row(cell, CarrierMatrixKind.LTE_SECONDARY, "LTE SCell $lteSecondaryIndex")
                }
                CellRole.NR -> {
                    nrIndex += 1
                    // Current parser model does not distinguish NR primary from NR secondary.
                    // Keep the label neutral rather than inventing a role.
                    row(cell, CarrierMatrixKind.NR, "NR Carrier $nrIndex")
                }
            }
        }

        val lteCount = rows.count { it.kind != CarrierMatrixKind.NR }
        val nrCount = rows.count { it.kind == CarrierMatrixKind.NR }
        val caHeadline = when {
            caActive && lteCount >= 2 -> "LTE CA موثّق • $lteCount Carriers"
            caActive -> "LTE CA نشط موثّق • تفاصيل الـSCell غير مكتملة"
            caVerified -> "LTE CA غير نشط • موثّق"
            else -> "LTE CA غير مؤكد"
        }

        val evidenceMessage = when {
            rows.isEmpty() -> "لا توجد هوية Carrier حية مكتملة يمكن عرضها الآن"
            nrVerified && caActive -> "يعرض فقط LTE/NR Carriers التي اجتازت تحقق الحالة والهوية الحية"
            nrVerified -> "NR ظاهر فقط لأن اتصال 5G الحي موثّق؛ لا تُستخدم إعدادات NR كدليل"
            caActive -> "SCells ظاهرة فقط لأن CA نشطة وموثّقة؛ لا تُستخدم أقنعة LTE كدليل"
            else -> "المعروض هو Carrier حي موثّق فقط؛ الإعدادات المطلوبة لا تدخل في هذه القائمة"
        }

        return CarrierMatrixModel(
            rows = rows,
            lteVerified = lteVerified,
            nrVerified = nrVerified,
            caVerified = caVerified,
            caActive = caActive,
            lteCarrierCount = lteCount,
            nrCarrierCount = nrCount,
            caHeadline = caHeadline,
            evidenceMessage = evidenceMessage
        )
    }

    private fun row(cell: CarrierCell, kind: CarrierMatrixKind, label: String) = CarrierMatrixRow(
        kind = kind,
        roleLabel = label,
        band = cell.band,
        pci = cell.pci,
        arfcn = cell.arfcn,
        bandwidthMhz = cell.bandwidthMhz
    )

    private fun flag(snapshot: RouterSnapshot, key: String): Boolean =
        snapshot.raw[key].equals("true", ignoreCase = true)
}
