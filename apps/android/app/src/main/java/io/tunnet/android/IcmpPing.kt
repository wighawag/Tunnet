package io.tunnet.android

/**
 * ICMP echo to a mesh IPv4 via the platform `ping` binary.
 * This is ordinary host networking, not Tunnet's QUIC stream ping.
 */
object IcmpPing {
    private val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
    private val timeMs = Regex("""time[=<]([\d.]+)\s*ms""", RegexOption.IGNORE_CASE)

    sealed interface Result {
        data class Reply(val latencyMs: Double) : Result
        data class Failure(val message: String) : Result
    }

    fun isIpv4(host: String): Boolean {
        val match = ipv4.matchEntire(host.trim()) ?: return false
        return match.groupValues.drop(1).all { it.toInt() in 0..255 }
    }

    fun parseOutput(text: String, exitCode: Int): Result {
        val ms = timeMs.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        if (ms != null && (exitCode == 0 || text.contains("bytes from", ignoreCase = true))) {
            return Result.Reply(ms)
        }
        val detail = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.take(160)
        return Result.Failure(detail ?: "ping failed (exit $exitCode)")
    }

    fun ping(ip: String): Result {
        if (!isIpv4(ip)) {
            return Result.Failure("not an IPv4 address")
        }
        val process = try {
            // `-W 3 -c 1` bounds the run, so reading to EOF is the timeout.
            ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "3", ip)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            return Result.Failure(e.message ?: "could not start ping")
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return parseOutput(output, process.waitFor())
    }
}
