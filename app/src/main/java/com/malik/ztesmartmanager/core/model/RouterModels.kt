package com.malik.ztesmartmanager.core.model

data class CarrierCell(
    val role: CellRole,
    val band: String?,
    val pci: Int?,
    val arfcn: Int?,
    val bandwidthMhz: Double?
)

enum class CellRole { PRIMARY, SECONDARY, NR }

data class RouterSnapshot(
    val model: String? = null,
    val firmware: String? = null,
    val hardwareVersion: String? = null,
    val networkType: String? = null,
    val operatorCode: String? = null,
    val lteRsrp: Double? = null,
    val lteRsrq: Double? = null,
    val lteRssi: Double? = null,
    val lteSinr: Double? = null,
    val nrRsrp: Double? = null,
    val nrSinr: Double? = null,
    val lteBand: String? = null,
    val nrBand: String? = null,
    val pci: Int? = null,
    val earfcn: Int? = null,
    val cellId: Long? = null,
    val caActive: Boolean = false,
    val cells: List<CarrierCell> = emptyList(),
    val modem4gTemperature: Double? = null,
    val modem5gTemperature: Double? = null,
    val raw: Map<String, String> = emptyMap()
)

data class RouterCapabilities(
    val modelFamily: String,
    val supportsLteBandLock: Boolean,
    val supportsNrBandLock: Boolean,
    val supportsCellLock: Boolean,
    val supportsCarrierAggregationRead: Boolean,
    val supportsAntennaControl: Boolean,
    val supportedLteBands: Set<Int>,
    val supportedNrBands: Set<Int>
)

data class OperationResult(
    val success: Boolean,
    val verified: Boolean,
    val message: String,
    val rawResult: String? = null
)
