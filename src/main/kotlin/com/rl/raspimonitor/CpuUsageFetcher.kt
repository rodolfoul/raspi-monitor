package com.rl.raspimonitor

import java.math.BigDecimal

private val cpuUsageCmd = arrayOf("top", "-bn1")
private val cpuUsageRegex = Regex("%Cpu.*?((\\d+)(\\.\\d+)?)\\s+id")
private val topProcessCmd = arrayOf("top", "-bc", "-n1", "-w512")
private val spacesRegex = Regex("\\s+")

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

fun fetchTopListing(c: Config): List<ProcessInfo> {
	val process = if (c.remote) {
		ProcessBuilder(*remoteCmd, *topProcessCmd)
	} else {
		ProcessBuilder(*topProcessCmd)
	}.start()

	val outputString = process.inputStream.use {
		it.readAllBytes().decodeToString()
	}

	return outputString.lines().subList(7, 11).map {
		val columns = it.substring(0, 69).trim().split(spacesRegex)
		val commandLine = it.substring(69).trim()
		ProcessInfo(
			columns[0].toInt(),
			columns[5].toInt(),
			columns[8].toDouble(),
			commandLine
		)
	}
}

data class ProcessInfo(
	val pid: Int,
	val memory: Int,
	val percentCpu: Double,
	val commandLine: String
)