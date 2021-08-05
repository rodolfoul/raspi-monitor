import java.math.BigDecimal

private val cpuUsageCmd = arrayOf("top", "-bn1")
private val cpuUsageRegex = Regex("%Cpu.*?((\\d+)(\\.\\d+)?)\\s+id")
private val oneHundred = BigDecimal.valueOf(100)

fun fetchCpuUsage(c: Config): Double {
	val process = if (c.remote) {
		ProcessBuilder(*remoteCmd, *cpuUsageCmd)
	} else {
		ProcessBuilder(*cpuUsageCmd)
	}.start()

	val outputString = process.inputStream.use {
		it.readAllBytes().decodeToString()
	}

	return (oneHundred - cpuUsageRegex.find(outputString)!!.groupValues[1].toBigDecimal()).toDouble()
}