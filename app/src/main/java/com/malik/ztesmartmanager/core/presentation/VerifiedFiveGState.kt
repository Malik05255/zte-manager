package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot

enum class VerifiedFiveGMode {
    NSA,
    SA,
    NR_MODE_UNSPECIFIED,
    NOT_VERIFIED
}

data class VerifiedFiveGState(
    val verified: Boolean,
    val mode: VerifiedFiveGMode,
    val headline: String,
    val evidenceMessage: String,
    val band: String?,
    val pci: Int?,
    val arfcn: Int?,
    val rsrp: Double?,
    val sinr: Double?,
    val lteAnchorBand: String?,
    val lteAnchorPci: Int?,
    val lteAnchorArfcn: Int?
)

/**
 * Converts parser evidence into text/UI-safe 5G state.
 *
 * This layer deliberately does not infer 5G from configured NR bands, stale NR fields,
 * or a network-mode preference. The only positive 5G state comes from the parser's
 * `_zte_nr_active_verified=true` contract.
 */
object VerifiedFiveGPresenter {
    fun from(snapshot: RouterSnapshot): VerifiedFiveGState {
        val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
        val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", ignoreCase = true)
        val radioMode = snapshot.raw["_zte_radio_mode"].orEmpty()
        val nrCell = snapshot.cells.firstOrNull { it.role == CellRole.NR }

        val mode = when {
            !nrVerified -> VerifiedFiveGMode.NOT_VERIFIED
            radioMode == "NSA_ACTIVE_VERIFIED" -> VerifiedFiveGMode.NSA
            radioMode == "SA_ACTIVE_VERIFIED" -> VerifiedFiveGMode.SA
            else -> VerifiedFiveGMode.NR_MODE_UNSPECIFIED
        }

        val headline = when (mode) {
            VerifiedFiveGMode.NSA -> "5G NSA"
            VerifiedFiveGMode.SA -> "5G SA"
            VerifiedFiveGMode.NR_MODE_UNSPECIFIED -> "5G NR"
            VerifiedFiveGMode.NOT_VERIFIED -> "5G غير مُثبت"
        }

        val evidenceMessage = when {
            nrVerified -> "اتصال NR حي موثّق: حالة راديو صريحة + Band + ARFCN + PCI + RSRP"
            snapshot.raw["_zte_nr_explicit_state"].equals("true", true) &&
                !snapshot.raw["_zte_nr_structural_evidence"].equals("true", true) ->
                "الراوتر يعلن حالة 5G، لكن هوية الـCarrier غير مكتملة؛ لذلك لا نعرضه كاتصال مؤكد"
            snapshot.raw["_zte_nr_explicit_state"].equals("true", true) &&
                !snapshot.raw["_zte_nr_signal_evidence"].equals("true", true) ->
                "حالة 5G وبيانات NR موجودة، لكن RSRP الحي غير مكتمل؛ الحالة غير مؤكدة"
            snapshot.raw["_zte_nr_structural_evidence"].equals("true", true) ->
                "توجد حقول NR، لكن لا توجد حالة راديو حية مكتملة تثبت اتصال 5G الآن"
            else -> "لا يوجد دليل حي مكتمل يسمح بإعلان اتصال 5G الآن"
        }

        return VerifiedFiveGState(
            verified = nrVerified,
            mode = mode,
            headline = headline,
            evidenceMessage = evidenceMessage,
            band = if (nrVerified) snapshot.nrBand ?: nrCell?.band else null,
            pci = if (nrVerified) nrCell?.pci else null,
            arfcn = if (nrVerified) nrCell?.arfcn else null,
            rsrp = if (nrVerified) snapshot.nrRsrp else null,
            sinr = if (nrVerified) snapshot.nrSinr else null,
            lteAnchorBand = if (nrVerified && mode == VerifiedFiveGMode.NSA && lteVerified) snapshot.lteBand else null,
            lteAnchorPci = if (nrVerified && mode == VerifiedFiveGMode.NSA && lteVerified) snapshot.pci else null,
            lteAnchorArfcn = if (nrVerified && mode == VerifiedFiveGMode.NSA && lteVerified) snapshot.earfcn else null
        )
    }
}
