package com.malik.ztesmartmanager.core.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedDeviceParserTest {
    @Test
    fun parsesWifiStationListFromJsonEncodedString() {
        val raw = JSONObject().put(
            "station_list",
            """[{"hostname":"iPhone","ip_addr":"192.168.0.21","mac_addr":"aa:bb:cc:dd:ee:ff","ssid_index":"1"}]"""
        )

        val devices = ConnectedDeviceParser.parse(raw, "station_list", DeviceTransport.WIFI)

        assertEquals(1, devices.size)
        assertEquals("iPhone", devices.single().displayName)
        assertEquals("192.168.0.21", devices.single().ipAddress)
        assertEquals("AA:BB:CC:DD:EE:FF", devices.single().macAddress)
        assertEquals(DeviceTransport.WIFI, devices.single().transport)
    }

    @Test
    fun parsesLanArrayAndMergesWithoutDuplicateMacs() {
        val wifi = ConnectedDeviceParser.parseValue(
            JSONArray("""[{"hostname":"Phone","mac_addr":"11:22:33:44:55:66"}]"""),
            DeviceTransport.WIFI
        )
        val lan = ConnectedDeviceParser.parseValue(
            JSONArray("""[
                {"hostname":"Phone duplicate","mac_addr":"11:22:33:44:55:66"},
                {"hostname":"NAS","ip_addr":"192.168.0.30","mac_addr":"AA:00:00:00:00:01"}
            ]"""),
            DeviceTransport.LAN
        )

        val merged = ConnectedDeviceParser.merge(wifi, lan)

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.displayName == "Phone" && it.transport == DeviceTransport.WIFI })
        assertTrue(merged.any { it.displayName == "NAS" && it.transport == DeviceTransport.LAN })
    }

    @Test
    fun unknownOrBlankShapesFailClosedToEmptyList() {
        assertTrue(ConnectedDeviceParser.parseValue("", DeviceTransport.WIFI).isEmpty())
        assertTrue(ConnectedDeviceParser.parseValue("not-json", DeviceTransport.WIFI).isEmpty())
        assertTrue(ConnectedDeviceParser.parseValue(JSONObject().put("unexpected", 1), DeviceTransport.LAN).isEmpty())
    }
}
