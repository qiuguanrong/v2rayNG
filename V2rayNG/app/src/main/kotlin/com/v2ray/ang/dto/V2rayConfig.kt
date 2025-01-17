package com.v2ray.ang.dto

import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class V2rayConfig(
        var stats: Any? = null,
        val log: LogBean,
        var policy: PolicyBean?,
        val inbounds: ArrayList<InboundBean>,
        var outbounds: ArrayList<OutboundBean>,
        var dns: DnsBean,
        val routing: RoutingBean,
        val api: Any? = null,
        val transport: Any? = null,
        val reverse: Any? = null,
        val fakedns: Any? = null) {
    companion object {
        const val DEFAULT_PORT = 443
        const val DEFAULT_SECURITY = "auto"
        const val DEFAULT_LEVEL = 8
        const val DEFAULT_NETWORK = "tcp"
        const val DEFAULT_FLOW = "xtls-rprx-splice"

        const val TLS = "tls"
        const val XTLS = "xtls"
        const val HTTP = "http"
    }

    data class LogBean(val access: String,
                       val error: String,
                       var loglevel: String?)

    data class InboundBean(
            var tag: String,
            var port: Int,
            var protocol: String,
            var listen: String? = null,
            val settings: Any? = null,
            val sniffing: SniffingBean?,
            val streamSettings: Any? = null,
            val allocate: Any? = null) {

        data class InSettingsBean(val auth: String? = null,
                                  val udp: Boolean? = null,
                                  val userLevel: Int? = null,
                                  val address: String? = null,
                                  val port: Int? = null,
                                  val network: String? = null)

        data class SniffingBean(var enabled: Boolean,
                                val destOverride: List<String>,
                                val metadataOnly: Boolean? = null)
    }

    data class OutboundBean(val tag: String = "proxy",
                            var protocol: String,
                            var settings: OutSettingsBean? = null,
                            var streamSettings: StreamSettingsBean? = null,
                            val proxySettings: Any? = null,
                            val sendThrough: String? = null,
                            val mux: MuxBean? = MuxBean(false)) {

        data class OutSettingsBean(var vnext: List<VnextBean>? = null,
                                   var servers: List<ServersBean>? = null,
                                    /*Blackhole*/
                                   var response: Response? = null,
                                    /*DNS*/
                                   val network: String? = null,
                                   val address: String? = null,
                                   val port: Int? = null,
                                    /*Freedom*/
                                   val domainStrategy: String? = null,
                                   val redirect: String? = null,
                                   val userLevel: Int? = null,
                                    /*Loopback*/
                                   val inboundTag: String? = null) {

            data class VnextBean(var address: String = "",
                                 var port: Int = DEFAULT_PORT,
                                 var users: List<UsersBean>) {

                data class UsersBean(var id: String = "",
                                     var alterId: Int? = null,
                                     var security: String = DEFAULT_SECURITY,
                                     var level: Int = DEFAULT_LEVEL,
                                     var encryption: String = "",
                                     var flow: String = "")
            }

            data class ServersBean(var address: String = "",
                                   var method: String = "chacha20-poly1305",
                                   var ota: Boolean = false,
                                   var password: String = "",
                                   var port: Int = DEFAULT_PORT,
                                   var level: Int = DEFAULT_LEVEL,
                                   var users: List<SocksUsersBean>? = null) {


                data class SocksUsersBean(var user: String = "",
                                          var pass: String = "",
                                          var level: Int = DEFAULT_LEVEL)
            }

            data class Response(var type: String)
        }

        data class StreamSettingsBean(var network: String = DEFAULT_NETWORK,
                                      var security: String = "",
                                      var tcpSettings: TcpSettingsBean? = null,
                                      var kcpSettings: KcpSettingsBean? = null,
                                      var wsSettings: WsSettingsBean? = null,
                                      var httpSettings: HttpSettingsBean? = null,
                                      var tlsSettings: TlsSettingsBean? = null,
                                      var quicSettings: QuicSettingBean? = null,
                                      var xtlsSettings: TlsSettingsBean? = null,
                                      val grpcSettings: Any? = null,
                                      val dsSettings: Any? = null,
                                      val sockopt: Any? = null
        ) {

            data class TcpSettingsBean(var header: HeaderBean = HeaderBean()) {
                data class HeaderBean(var type: String = "none",
                                      var request: RequestBean? = null,
                                      var response: Any? = null) {
                    data class RequestBean(var path: List<String> = ArrayList(),
                                           var headers: HeadersBean = HeadersBean()) {
                        data class HeadersBean(var Host: List<String> = ArrayList())
                    }
                }
            }

            data class KcpSettingsBean(var mtu: Int = 1350,
                                       var tti: Int = 50,
                                       var uplinkCapacity: Int = 12,
                                       var downlinkCapacity: Int = 100,
                                       var congestion: Boolean = false,
                                       var readBufferSize: Int = 1,
                                       var writeBufferSize: Int = 1,
                                       var header: HeaderBean = HeaderBean(),
                                       var seed: String? = null) {
                data class HeaderBean(var type: String = "none")
            }

            data class WsSettingsBean(var path: String = "",
                                      var headers: HeadersBean = HeadersBean()) {
                data class HeadersBean(var Host: String = "")
            }

            data class HttpSettingsBean(var host: List<String> = ArrayList(),
                                        var path: String = "")

            data class TlsSettingsBean(var allowInsecure: Boolean = false,
                                       var serverName: String = "",
                                       val alpn: List<String>? = null,
                                       val certificates: List<Any>? = null,
                                       val disableSystemRoot: Boolean? = null)

            data class QuicSettingBean(var security: String = "none",
                                       var key: String = "",
                                       var header: HeaderBean = HeaderBean()) {
                data class HeaderBean(var type: String = "none")
            }

            fun populateTransportSettings(transport: String, headerType: String?, host: String?, path: String?, seed: String?,
                                          quicSecurity: String?, key: String?): String {
                var sni = ""
                network = transport
                when (network) {
                    "tcp" -> if (headerType == HTTP) {
                        val tcpSetting = TcpSettingsBean()
                        tcpSetting.header.type = HTTP
                        if (!TextUtils.isEmpty(host) || !TextUtils.isEmpty(path)) {
                            val requestObj = TcpSettingsBean.HeaderBean.RequestBean()
                            requestObj.headers.Host = (host ?: "").split(",").map { it.trim() }
                            requestObj.path = (path ?: "").split(",").map { it.trim() }
                            tcpSetting.header.request = requestObj
                            sni = requestObj.headers.Host.getOrNull(0) ?: sni
                        }
                        tcpSettings = tcpSetting
                    }
                    "kcp" -> {
                        val kcpsetting = KcpSettingsBean()
                        kcpsetting.header.type = headerType ?: "none"
                        if (seed.isNullOrEmpty()) {
                            kcpsetting.seed = null
                        } else {
                            kcpsetting.seed = seed
                        }
                        kcpSettings = kcpsetting
                    }
                    "ws" -> {
                        val wssetting = WsSettingsBean()
                        wssetting.headers.Host = host ?: ""
                        sni = wssetting.headers.Host
                        wssetting.path = path ?: "/"
                        wsSettings = wssetting
                    }
                    "h2", "http" -> {
                        network = "h2"
                        val h2Setting = HttpSettingsBean()
                        h2Setting.host = (host ?: "").split(",").map { it.trim() }
                        sni = h2Setting.host.getOrNull(0) ?: sni
                        h2Setting.path = path ?: "/"
                        httpSettings = h2Setting
                    }
                    "quic" -> {
                        val quicsetting = QuicSettingBean()
                        quicsetting.security = quicSecurity ?: "none"
                        quicsetting.key = key ?: ""
                        quicsetting.header.type = headerType ?: "none"
                        quicSettings = quicsetting
                    }
                }
                return sni
            }

            fun populateTlsSettings(streamSecurity: String, allowInsecure: Boolean, sni: String) {
                security = streamSecurity
                val tlsSetting = TlsSettingsBean(
                        allowInsecure = allowInsecure,
                        serverName = sni
                )
                if (security == TLS) {
                    tlsSettings = tlsSetting
                } else if (security == XTLS) {
                    xtlsSettings = tlsSetting
                }
            }
        }

        data class MuxBean(var enabled: Boolean, var concurrency: Int = 8)

        fun getServerAddress(): String? {
            if (protocol.equals(EConfigType.VMESS.name, true)
                    || protocol.equals(EConfigType.VLESS.name, true)) {
                return settings?.vnext?.get(0)?.address
            } else if (protocol.equals(EConfigType.SHADOWSOCKS.name, true)
                    || protocol.equals(EConfigType.SOCKS.name, true)
                    || protocol.equals(EConfigType.TROJAN.name, true)) {
                return settings?.servers?.get(0)?.address
            }
            return null
        }

        fun getServerPort(): Int? {
            if (protocol.equals(EConfigType.VMESS.name, true)
                    || protocol.equals(EConfigType.VLESS.name, true)) {
                return settings?.vnext?.get(0)?.port
            } else if (protocol.equals(EConfigType.SHADOWSOCKS.name, true)
                    || protocol.equals(EConfigType.SOCKS.name, true)
                    || protocol.equals(EConfigType.TROJAN.name, true)) {
                return settings?.servers?.get(0)?.port
            }
            return null
        }

        fun getPassword(): String? {
            if (protocol.equals(EConfigType.VMESS.name, true)
                    || protocol.equals(EConfigType.VLESS.name, true)) {
                return settings?.vnext?.get(0)?.users?.get(0)?.id
            } else if (protocol.equals(EConfigType.SHADOWSOCKS.name, true)
                    || protocol.equals(EConfigType.TROJAN.name, true)) {
                return settings?.servers?.get(0)?.password
            } else if (protocol.equals(EConfigType.SOCKS.name, true)) {
                return settings?.servers?.get(0)?.users?.get(0)?.pass
            }
            return null
        }

        fun getSecurityEncryption(): String? {
            return when {
                protocol.equals(EConfigType.VMESS.name, true) -> settings?.vnext?.get(0)?.users?.get(0)?.security
                protocol.equals(EConfigType.VLESS.name, true) -> settings?.vnext?.get(0)?.users?.get(0)?.encryption
                protocol.equals(EConfigType.SHADOWSOCKS.name, true) -> settings?.servers?.get(0)?.method
                else -> null
            }
        }

        fun getTransportSettingDetails(): List<String>? {
            if (protocol.equals(EConfigType.VMESS.name, true)
                    || protocol.equals(EConfigType.VLESS.name, true)) {
                val transport = streamSettings?.network ?: return null
                return when(transport) {
                    "tcp" -> {
                        val tcpSetting = streamSettings?.tcpSettings ?: return null
                        listOf(tcpSetting.header.type,
                                tcpSetting.header.request?.headers?.Host?.joinToString().orEmpty(),
                                tcpSetting.header.request?.path?.joinToString().orEmpty())
                    }
                    "kcp" -> {
                        val kcpSetting = streamSettings?.kcpSettings ?: return null
                        listOf(kcpSetting.header.type,
                                "",
                                kcpSetting.seed.orEmpty())
                    }
                    "ws" -> {
                        val wsSetting = streamSettings?.wsSettings ?: return null
                        listOf("",
                                wsSetting.headers.Host,
                                wsSetting.path)
                    }
                    "h2" -> {
                        val h2Setting = streamSettings?.httpSettings ?: return null
                        listOf("",
                                h2Setting.host.joinToString(),
                                h2Setting.path)
                    }
                    "quic" -> {
                        val quicSetting = streamSettings?.quicSettings ?: return null
                        listOf(quicSetting.header.type,
                                quicSetting.security,
                                quicSetting.key)
                    }
                    else -> null
                }
            }
            return null
        }
    }

    data class DnsBean(var servers: List<Any>? = null,
                       var hosts: Map<String, String>? = null,
                       val clientIp: String? = null,
                       val disableCache: Boolean? = null,
                       val tag: String? = null
    ) {
        data class ServersBean(var address: String = "",
                               var port: Int = 0,
                               var domains: List<String>?,
                               var expectIPs: List<String>?,
                               val clientIp: String? = null)
    }

    data class RoutingBean(var domainStrategy: String,
                           val domainMatcher: String? = null,
                           var rules: ArrayList<RulesBean>,
                           val balancers: List<Any>? = null) {

        data class RulesBean(var type: String = "",
                             var ip: ArrayList<String>? = null,
                             var domain: ArrayList<String>? = null,
                             var outboundTag: String = "",
                             var balancerTag: String? = null,
                             var port: String? = null,
                             val sourcePort: String? = null,
                             val network: String? = null,
                             val source: List<String>? = null,
                             val user: List<String>? = null,
                             var inboundTag: List<String>? = null,
                             val protocol: List<String>? = null,
                             val attrs: String? = null,
                             val domainMatcher: String? = null
        )
    }

    data class PolicyBean(var levels: Map<String, LevelBean>,
                          var system: Any? = null) {
        data class LevelBean(
                var handshake: Int? = null,
                var connIdle: Int? = null,
                var uplinkOnly: Int? = null,
                var downlinkOnly: Int? = null,
                val statsUserUplink: Boolean? = null,
                val statsUserDownlink: Boolean? = null,
                var bufferSize: Int? = null)
    }

    fun getProxyOutbound(): OutboundBean? {
        outbounds.forEach { outbound ->
            if (outbound.protocol.equals(EConfigType.VMESS.name, true) ||
                    outbound.protocol.equals(EConfigType.VLESS.name, true) ||
                    outbound.protocol.equals(EConfigType.SHADOWSOCKS.name, true) ||
                    outbound.protocol.equals(EConfigType.SOCKS.name, true) ||
                    outbound.protocol.equals(EConfigType.TROJAN.name, true)) {
                return outbound
            }
        }
        return null
    }

    fun toPrettyPrinting(): String {
        return GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter( // custom serialiser is needed here since JSON by default parse number as Double, core will fail to start
                        object: TypeToken<Double>() {}.type,
                        JsonSerializer { src: Double?, _: Type?, _: JsonSerializationContext? -> JsonPrimitive(src?.toInt()) }
                )
                .create()
                .toJson(this)
    }
}
