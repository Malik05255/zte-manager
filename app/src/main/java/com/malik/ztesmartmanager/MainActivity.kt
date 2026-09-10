package com.malik.ztesmartmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { VerifiedAdvancedApp() } } }
    }
}

@Composable
private fun VerifiedAdvancedApp() {
    var address by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by rememberSaveable { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
    var snapshot by remember { mutableStateOf<RouterSnapshot?>(null) }
    var message by remember { mutableStateOf("غير متصل") }
    var busy by remember { mutableStateOf(false) }
    var lteChoice by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nrChoice by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(client) {
        val connected = client ?: return@LaunchedEffect
        while (client === connected) {
            delay(2_000)
            if (!busy) runCatching { connected.readSnapshot() }.onSuccess { snapshot = it }
        }
    }

    if (client == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("ZTE Manager • Verified", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(address, { address = it }, label = { Text("عنوان الراوتر") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching {
                            val c = ZteRouterClient(address)
                            c.login(password)
                            snapshot = c.readSnapshot()
                            client = c
                            message = "متصل بالراوتر"
                        }.onFailure { message = it.message ?: "فشل الاتصال" }
                        busy = false
                    }
                },
                enabled = !busy && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "جاري الاتصال..." else "اتصال") }
            Spacer(Modifier.height(8.dp))
            Text(message)
        }
        return
    }

    val connected = client ?: return
    val caps = connected.profile.capabilities
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        snapshot?.let { s ->
            item { VerifiedStateCard(s) }
        }
        item {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
        if (caps.supportsLteBandLock) {
            item {
                BandChoiceCard("إعداد 4G المطلوب", caps.supportedLteBands, lteChoice, "B", busy,
                    onToggle = { lteChoice = toggleChoice(lteChoice, it) },
                    onApply = {
                        scope.launch {
                            busy = true
                            val result = connected.setLteBands(lteChoice)
                            message = result.message
                            snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                            busy = false
                        }
                    })
            }
        }
        if (caps.supportsNrBandLock) {
            item {
                BandChoiceCard("إعداد 5G المطلوب", caps.supportedNrBands, nrChoice, "N", busy,
                    onToggle = { nrChoice = toggleChoice(nrChoice, it) },
                    onApply = {
                        scope.launch {
                            busy = true
                            val result = connected.setNrBands(nrChoice)
                            message = result.message
                            snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                            busy = false
                        }
                    })
            }
        }
    }
}

@Composable
private fun VerifiedStateCard(snapshot: RouterSnapshot) {
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val lteCarriers = snapshot.cells.filter { it.role != CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }
    val nrCarriers = snapshot.cells.filter { it.role == CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }
    val caText = when {
        !caVerified -> "غير مؤكد"
        !snapshot.caActive -> "غير نشط"
        lteCarriers.size >= 2 -> "نشط فعليًا • ${lteCarriers.mapNotNull { it.band }.joinToString(" + ")}"
        else -> "حالة CA نشطة لكن تفاصيل الحوامل غير كافية"
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("الحالة الفعلية الموثقة", fontWeight = FontWeight.Bold)
            TruthRow("الاتصال", snapshot.raw["_zte_verified_network_type"]?.ifBlank { "غير مؤكد" } ?: "غير مؤكد")
            TruthRow("CA", caText)
            TruthRow("LTE", lteCarriers.joinToString(" + ") { it.band ?: "LTE" }.ifBlank { "غير مؤكد/غير نشط" })
            TruthRow("NR", nrCarriers.joinToString(" + ") { it.band ?: "NR" }.ifBlank { "غير مؤكد/غير نشط" })
            TruthRow("LTE PCI/EARFCN", if (snapshot.pci != null && snapshot.earfcn != null) "${snapshot.pci} / ${snapshot.earfcn}" else "غير مؤكد")
            TruthRow("Cell ID", snapshot.cellId?.toString() ?: "غير مؤكد")
            Spacer(Modifier.height(8.dp))
            Text("الإعداد المسموح لا يُعرض هنا كتردد نشط. HTTP success لا يساوي Verified.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BandChoiceCard(
    title: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("هذه اختيارات لإرسال إعداد جديد فقط، وليست حالة اتصال فعلية.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            bands.sorted().chunked(4).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { band ->
                        FilterChip(selected = band in selected, onClick = { onToggle(band) }, label = { Text("$prefix$band") }, modifier = Modifier.weight(1f))
                    }
                    repeat(4 - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onApply, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text("إرسال والتحقق من read-back")
            }
        }
    }
}

@Composable
private fun TruthRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun toggleChoice(current: Set<Int>, band: Int): Set<Int> = if (band in current) current - band else current + band
