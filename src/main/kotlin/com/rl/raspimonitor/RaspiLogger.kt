package com.rl.raspimonitor

import com.google.common.util.concurrent.RateLimiter
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

val remoteCmd = arrayOf("ssh", "pi@racintus")

fun main(args: Array<String>) {
	val config = mainBody {
		ArgParser(args).parseInto(::Config)
	}

	if (!config.fileExists()) {
		config.useCsvOutputStream { output ->
			output.println("Timestamp,cpu-usage,cpu-temperature")
		}
	}

	val rateLimiter = RateLimiter.create(1 / 2.0)
	val processListingLimiter = RateLimiter.create(1 / (5 * 60.0))

	while (true) {
		rateLimiter.acquire()
		outputCsvStats(config)

		if (processListingLimiter.tryAcquire()) {
			outputProcessStats(config)
		}
	}
}

fun outputProcessStats(config: Config) {
	val processListing: List<ProcessInfo> = try {
		fetchTopListing(config)
	} catch (e: Exception) {
		config.useProcessListingOutputStream { stream ->
			e.printStackTrace(stream)
		}
		return
	}

	config.useProcessListingOutputStream { stream ->
		stream.println("${LocalDateTime.now()},$processListing")
	}
}

private fun outputCsvStats(config: Config) {
	val t = LocalDateTime.now()
	val cpuUsage = try {
		fetchCpuUsage(config)
	} catch (e: Exception) {
		e.printStackTrace()
		return
	}

	val cpuTemp = try {
		fetchTemperature(config)
	} catch (e: Exception) {
		e.printStackTrace()
		return
	}

	config.useCsvOutputStream { output ->
		output.println("$t,$cpuUsage,$cpuTemp")
	}
}

class Config(parser: ArgParser) {
	val remote: Boolean by parser.flagging("-r", "--remote", help = "Execute remotely?")

	private val outputName: String by parser.storing("-o", "--output", help = "Output file name").default { "-" }

	fun useCsvOutputStream(block: (PrintStream) -> Unit) {
		if (isSystemOut(outputName)) {
			block(System.out)
			return
		}
		block(PrintStream(FileOutputStream("$outputName.csv", true), true, StandardCharsets.UTF_8))
	}

	fun useProcessListingOutputStream(block: (PrintStream) -> Unit) {
		if (isSystemOut(outputName)) {
			block(System.out)
			return
		}
		block(PrintStream(FileOutputStream("$outputName.process-listing", true), true, StandardCharsets.UTF_8))
	}

	private fun isSystemOut(outName: String): Boolean {
		return outName.isBlank() || outName == "-"
	}


	fun fileExists(): Boolean {
		if (isSystemOut(outputName)) {
			return false
		}

		return Files.exists(Paths.get(outputName))
	}
}