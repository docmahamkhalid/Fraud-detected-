package com.example.data.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

data class UrlInspectionReport(
    val normalizedUrl: String,
    val host: String,
    val isValidFormat: Boolean,
    val isBlockedPrivateTarget: Boolean,
    val usesHttps: Boolean,
    val isPunycode: Boolean,
    val isIpAddressHost: Boolean,
    val isUrlShortener: Boolean,
    val lookalikeBrand: String?,
    val suspiciousTld: Boolean,
    val dnsResolved: Boolean,
    val resolvedPublicIp: Boolean,
    val httpStatusCode: Int?,
    val redirectTarget: String?,
    val networkReachable: Boolean,
    val signals: List<String>,
    val technicalRiskDelta: Int,
    val rejectionReason: String? = null
)

object ThreatIntelligenceService {

    private val shorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "is.gd", "ow.ly", "cutt.ly",
        "tiny.cc", "goo.gl", "shorturl.at", "buff.ly", "rebrand.ly"
    )

    private val highRiskTlds = setOf(
        "xyz", "top", "click", "loan", "zip", "tk", "ml", "ga", "cf", "gq", "work", "buzz", "cam"
    )

    private val protectedBrands = listOf(
        "paypal", "amazon", "google", "microsoft", "apple", "netflix", "whatsapp",
        "instagram", "facebook", "binance", "coinbase", "chase", "wellsfargo",
        "bankofamerica", "citibank", "hsbc", "barclays", "dhl", "fedex", "usps",
        "easypaisa", "jazzcash", "hbl", "meezanbank", "hdfcbank", "icicibank", "sbi"
    )

    private val officialDomains = mapOf(
        "paypal" to setOf("paypal.com"),
        "amazon" to setOf("amazon.com", "amazon.co.uk", "amazon.in", "amazon.ae"),
        "google" to setOf("google.com", "accounts.google.com", "mail.google.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com"),
        "apple" to setOf("apple.com", "icloud.com"),
        "netflix" to setOf("netflix.com"),
        "whatsapp" to setOf("whatsapp.com", "web.whatsapp.com"),
        "instagram" to setOf("instagram.com"),
        "facebook" to setOf("facebook.com"),
        "binance" to setOf("binance.com"),
        "coinbase" to setOf("coinbase.com"),
        "chase" to setOf("chase.com"),
        "dhl" to setOf("dhl.com"),
        "fedex" to setOf("fedex.com"),
        "usps" to setOf("usps.com"),
        "easypaisa" to setOf("easypaisa.com.pk"),
        "jazzcash" to setOf("jazzcash.com.pk"),
        "hbl" to setOf("hbl.com"),
        "hdfcbank" to setOf("hdfcbank.com"),
        "icicibank" to setOf("icicibank.com"),
        "sbi" to setOf("sbi.co.in", "onlinesbi.sbi")
    )

    private val safeHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun extractUrlsFromText(text: String): List<String> {
        val regex = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"{}|\\^`\[\]]+""")
        return regex.findAll(text).map { it.value.trimEnd('.', ',', ')', ';') }.distinct().toList()
    }

    suspend fun inspectUrl(rawInput: String): UrlInspectionReport = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return@withContext invalidReport(trimmed, "Website address cannot be empty.")
        }

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            return@withContext invalidReport(trimmed, "Malformed URL syntax.")
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        if (scheme != "http" && scheme != "https") {
            return@withContext invalidReport(
                trimmed,
                "Unsupported URI scheme '$scheme'. Only http and https links are allowed."
            )
        }

        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: ""
        if (host.isBlank() || !host.contains('.')) {
            return@withContext invalidReport(trimmed, "Invalid or incomplete domain name.")
        }

        // SSRF Guard: Reject localhost, internal TLDs, and literal private IPs
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".localhost")) {
            return@withContext blockedReport(withScheme, host, "Local or internal hostnames are blocked for security.")
        }

        val isIpLiteral = host.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$""")) || host.contains(":")
        if (isIpLiteral && isPrivateOrSpecialIpString(host)) {
            return@withContext blockedReport(withScheme, host, "Private or loopback IP ranges are blocked for security.")
        }

        val signals = mutableListOf<String>()
        var delta = 0

        val usesHttps = scheme == "https"
        if (!usesHttps) {
            signals.add("Connection does not use HTTPS encryption")
            delta += 20
        }

        val isPunycode = host.split('.').any { it.startsWith("xn--") }
        if (isPunycode) {
            signals.add("Domain uses Punycode (xn--), which can disguise lookalike characters")
            delta += 25
        }

        if (isIpLiteral) {
            signals.add("URL points directly to a raw IP address instead of a registered domain")
            delta += 30
        }

        val isShortener = shorteners.any { host == it || host.endsWith(".$it") }
        if (isShortener) {
            signals.add("Uses a link shortener ($host) that hides the final destination")
            delta += 20
        }

        val tld = host.substringAfterLast('.', "")
        val suspiciousTld = tld in highRiskTlds
        if (suspiciousTld) {
            signals.add("Top-level domain '.$tld' is frequently abused in phishing campaigns")
            delta += 15
        }

        val subdomainCount = host.split('.').size
        if (subdomainCount >= 4) {
            signals.add("Excessive subdomain nesting ($subdomainCount levels), often used to mimic trusted brands")
            delta += 15
        }

        val lookalike = detectLookalikeBrand(host)
        if (lookalike != null) {
            signals.add("Domain closely resembles '$lookalike' without matching its official domain")
            delta += 35
        }

        // DNS check to enforce SSRF protection on resolved A/AAAA records
        var dnsResolved = false
        var resolvedPublicIp = false
        try {
            val addresses = InetAddress.getAllByName(host)
            if (addresses.isNotEmpty()) {
                dnsResolved = true
                val hasPrivate = addresses.any { isDisallowedInetAddress(it) }
                if (hasPrivate) {
                    return@withContext blockedReport(
                        withScheme,
                        host,
                        "Domain resolves to a private or reserved network IP address and was blocked."
                    )
                }
                resolvedPublicIp = true
            }
        } catch (_: Exception) {
            signals.add("Domain DNS lookup could not be resolved")
        }

        var httpStatus: Int? = null
        var redirectTarget: String? = null
        var networkReachable = false

        if (resolvedPublicIp) {
            try {
                val request = Request.Builder()
                    .url(withScheme)
                    .head()
                    .header("User-Agent", "ScamShieldSafetyCheck/1.0")
                    .build()
                safeHttpClient.newCall(request).execute().use { response ->
                    networkReachable = true
                    httpStatus = response.code
                    if (response.isRedirect) {
                        redirectTarget = response.header("Location")
                        if (!redirectTarget.isNullOrBlank()) {
                            signals.add("Redirects to external destination: ${redirectTarget.take(80)}")
                            delta += 12
                        }
                    }
                }
            } catch (e: javax.net.ssl.SSLException) {
                signals.add("TLS/SSL certificate check failed (${e.localizedMessage ?: "untrusted certificate"})")
                delta += 25
            } catch (_: Exception) {
                signals.add("Live server header check could not reach the host")
            }
        }

        UrlInspectionReport(
            normalizedUrl = withScheme,
            host = host,
            isValidFormat = true,
            isBlockedPrivateTarget = false,
            usesHttps = usesHttps,
            isPunycode = isPunycode,
            isIpAddressHost = isIpLiteral,
            isUrlShortener = isShortener,
            lookalikeBrand = lookalike,
            suspiciousTld = suspiciousTld,
            dnsResolved = dnsResolved,
            resolvedPublicIp = resolvedPublicIp,
            httpStatusCode = httpStatus,
            redirectTarget = redirectTarget,
            networkReachable = networkReachable,
            signals = signals,
            technicalRiskDelta = delta.coerceIn(0, 85)
        )
    }

    private fun invalidReport(raw: String, reason: String) = UrlInspectionReport(
        normalizedUrl = raw,
        host = "",
        isValidFormat = false,
        isBlockedPrivateTarget = false,
        usesHttps = false,
        isPunycode = false,
        isIpAddressHost = false,
        isUrlShortener = false,
        lookalikeBrand = null,
        suspiciousTld = false,
        dnsResolved = false,
        resolvedPublicIp = false,
        httpStatusCode = null,
        redirectTarget = null,
        networkReachable = false,
        signals = listOf(reason),
        technicalRiskDelta = 0,
        rejectionReason = reason
    )

    private fun blockedReport(url: String, host: String, reason: String) = UrlInspectionReport(
        normalizedUrl = url,
        host = host,
        isValidFormat = true,
        isBlockedPrivateTarget = true,
        usesHttps = false,
        isPunycode = false,
        isIpAddressHost = false,
        isUrlShortener = false,
        lookalikeBrand = null,
        suspiciousTld = false,
        dnsResolved = false,
        resolvedPublicIp = false,
        httpStatusCode = null,
        redirectTarget = null,
        networkReachable = false,
        signals = listOf(reason),
        technicalRiskDelta = 80,
        rejectionReason = reason
    )

    private fun isDisallowedInetAddress(addr: InetAddress): Boolean {
        if (addr.isAnyLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress ||
            addr.isSiteLocalAddress || addr.isMulticastAddress
        ) {
            return true
        }
        if (addr is Inet4Address) {
            val bytes = addr.address
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0 || b0 == 10 || b0 == 127) return true
            if (b0 == 169 && b1 == 254) return true
            if (b0 == 172 && b1 in 16..31) return true
            if (b0 == 192 && b1 == 168) return true
            if (b0 == 100 && b1 in 64..127) return true // CGNAT
        }
        return false
    }

    private fun isPrivateOrSpecialIpString(host: String): Boolean {
        return try {
            val addr = InetAddress.getByName(host)
            isDisallowedInetAddress(addr)
        } catch (_: Exception) {
            true
        }
    }

    private fun detectLookalikeBrand(host: String): String? {
        val parts = host.split('.')
        val registeredBase = if (parts.size >= 2) parts[parts.size - 2] else host
        for (brand in protectedBrands) {
            val officialSet = officialDomains[brand].orEmpty()
            val isOfficial = officialSet.any { host == it || host.endsWith(".$it") }
            if (isOfficial) return null

            // Substring brand impersonation in domain/subdomain (e.g. paypal-secure-login.xyz)
            if (host.contains(brand) && !isOfficial) {
                return brand.replaceFirstChar { it.uppercase() }
            }
            // Typosquatting via Levenshtein distance <= 2 on main label
            if (registeredBase.length >= 4 && levenshtein(registeredBase, brand) in 1..2) {
                return brand.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
