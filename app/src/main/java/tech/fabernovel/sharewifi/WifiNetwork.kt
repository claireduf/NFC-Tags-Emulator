package com.claireduf.sharewifi

data class WifiModel(
    val network: WifiNetwork,
    val missingPassword: Boolean,
    val status: Status
)

enum class Status {
    CONNECTED, REACHABLE, UNREACHABLE;

    companion object {
        fun getReachable(reachable: List<WifiNetwork>, network:  WifiNetwork ): Status {
           val optReachable = reachable.find { (ssid) -> ssid == network.ssid }
            if(optReachable != null) return REACHABLE else return UNREACHABLE
        }
    }
}


data class WifiNetwork(
    val ssid: String,
    val authType: AuthType,
    val key: String?
)

enum class AuthType {
    WPA_PSK,
    WPA2_PSK,
    WPA_EAP,
    WPA2_EAP,
    OPEN
}
