package tech.fabernovel.nfctagsemulator

import org.junit.Assert
import org.junit.Test

/**
 * Created by epo on 29/09/2017.
 */
class MergeListsTest {

        val known = listOf(
                WifiNetwork(
                        "ssid1",
                        AuthType.WPA_EAP,
                        null
                ),
                WifiNetwork(
                        "ssid2",
                        AuthType.WPA_EAP,
                        null
                ),
                WifiNetwork(
                        "ssid3",
                        AuthType.WPA_EAP,
                        null
                ),
                WifiNetwork(
                        "ssid4",
                        AuthType.WPA_EAP,
                        null
                )
        )

    val reachable: List<WifiNetwork> = listOf(
            WifiNetwork(
                    "ssid2",
                    AuthType.WPA_EAP,
                    null
            )
    )

    val stored: List<WifiNetwork> = listOf(
            WifiNetwork(
                    "ssid1",
                    AuthType.WPA_EAP,
                    "password1"
            ),
            WifiNetwork(
                    "ssid2",
                    AuthType.WPA_EAP,
                    "password2"
            ),
            WifiNetwork(
                    "ssid4",
                    AuthType.WPA_EAP,
                    null
            ),
            WifiNetwork(
                    "ssid10",
                    AuthType.WPA_EAP,
                    "password10"
            )
    )

    val connected = WifiNetwork(
            "ssid4",
            AuthType.WPA_EAP,
            null
    )

    @Test
    fun mergelists_CheckConnected() {

        val mergedList = MainActivity.mergeStoredAndDeviceWifiLists(
                known,
                reachable,
                connected,
                stored
        )
        Assert.assertTrue(mergedList.size == 4)
        //connected wifi is well returned in the merged list
        Assert.assertTrue(mergedList.filter { wifi -> wifi.status.equals(Status.CONNECTED) }.size == 1)
        Assert.assertTrue(mergedList.find { (network, _, status) -> status.equals(Status.CONNECTED) && network.ssid == "ssid4" } != null)

        //merged list contains wifi models with password
        Assert.assertTrue(mergedList.filter { wifi -> wifi.status.equals(Status.REACHABLE) }.size == 1)
        Assert.assertTrue(mergedList.find { (network, _, status) -> status.equals(Status.REACHABLE) && network.ssid == "ssid2" } != null)

        //merged lists contains wifi models with passwords
        Assert.assertTrue(mergedList.filter { wifi -> !wifi.missingPassword  }.size == 2)
        Assert.assertTrue(mergedList.find { (network, missingPassword, _) -> !missingPassword} != null)
        Assert.assertTrue(mergedList.filter { (network, _, _) -> (network.key.equals("password1") || network.key.equals("password2")) }.size == 2)
    }
}