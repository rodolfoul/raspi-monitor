package com.rl.raspimonitor

import java.nio.file.Files
import java.nio.file.Paths

private const val temperatureFile = "/sys/class/thermal/thermal_zone0/temp"
private val temperatureCmd = arrayOf("cat", temperatureFile)

fun fetchTemperature(c: Config): Double {
	val outputString = if (c.remote) {
		val process = ProcessBuilder(*remoteCmd, *temperatureCmd).start()
		process.inputStream.readAllBytes().decodeToString()

	} else {
		Files.readString(Paths.get(temperatureFile))
	}
	return outputString.trim().toDouble() / 1000
}
