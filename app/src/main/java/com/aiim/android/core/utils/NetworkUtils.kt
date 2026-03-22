package com.aiim.android.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.core.content.ContextCompat.getSystemService
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

/**
 * 网络工具：获取局域网 IPv4、校验 IP，尽量与「设置 → WLAN → 当前网络详情」里看到的 IPv4 一致。
 */
object NetworkUtils {

    /**
     * 获取当前用于上网的那张网卡的局域网 IPv4（优先私网地址，排除回环与常见 VPN 接口）。
     */
    fun getLocalIpAddress(context: Context): String {
        val app = context.applicationContext

        fromActiveNetworkLinkProperties(app)?.let { return it }

        fromNetworkInterfaces()?.let { return it }

        fromWifiManager(app)?.let { return it }

        return "127.0.0.1"
    }

    private fun fromActiveNetworkLinkProperties(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(network) ?: return null
        val candidates = mutableListOf<String>()
        for (la in props.linkAddresses) {
            val addr = la.address ?: continue
            if (addr.isLoopbackAddress || addr !is Inet4Address) continue
            val host = addr.hostAddress ?: continue
            if (isPrivateOrHotspotIpv4(host)) candidates.add(host)
        }
        return candidates.firstOrNull()
    }

    private fun fromNetworkInterfaces(): String? {
        val scored = mutableListOf<Pair<Int, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name.lowercase()
                if (name.startsWith("dummy") || name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("rmnet")) {
                    continue
                }
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr.isLoopbackAddress || addr !is Inet4Address) continue
                    val host = addr.hostAddress ?: continue
                    if (!isPrivateOrHotspotIpv4(host)) continue
                    val priority = when {
                        name.startsWith("wlan") -> 0
                        name.startsWith("ap") || name.startsWith("swlan") || name.contains("softap") -> 1
                        name.startsWith("eth") -> 2
                        name.startsWith("rndis") || name.startsWith("usb") -> 3
                        else -> 4
                    }
                    scored.add(priority to host)
                }
            }
        } catch (_: SocketException) {
            return null
        }
        return scored.minByOrNull { it.first }?.second
    }

    @Suppress("DEPRECATION")
    private fun fromWifiManager(context: Context): String? {
        return try {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val ip = wm.connectionInfo?.ipAddress ?: return null
            if (ip == 0) return null
            val formatted = Formatter.formatIpAddress(ip)
            formatted.takeIf { isPrivateOrHotspotIpv4(it) }
        } catch (_: Exception) {
            null
        }
    }

    /** 192.168.x / 10.x / 172.16–31.x，以及常见热点网段 */
    private fun isPrivateOrHotspotIpv4(ip: String): Boolean {
        val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        val a = parts[0]
        val b = parts[1]
        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 192 && b == 43 -> true // 不少机型开热点时分配 192.168.43.x
            a == 192 && b == 137 -> true // 部分 Windows 共享 / 旧热点
            else -> false
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = getSystemService(context, ConnectivityManager::class.java)
        return connectivityManager?.let { cm ->
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: false
    }

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = getSystemService(context, ConnectivityManager::class.java)
        return connectivityManager?.let { cm ->
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } ?: false
    }

    fun isValidIpAddress(ipAddress: String): Boolean {
        val pattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        return pattern.matches(ipAddress)
    }

    fun getNetworkType(context: Context): String {
        val connectivityManager = getSystemService(context, ConnectivityManager::class.java)
        return connectivityManager?.let { cm ->
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)

            when {
                capabilities == null -> "无网络"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                else -> "未知网络"
            }
        } ?: "无网络"
    }
}
