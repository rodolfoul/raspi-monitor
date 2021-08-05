import com.google.common.util.concurrent.RateLimiter
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File
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

	config.output.println("Timestamp,cpu-usage,cpu-temperature")
	val rateLimiter = RateLimiter.create(1 / 2.0)
	while (true) {
		rateLimiter.acquire()
		val t = LocalDateTime.now()
		val cpuUsage = try {
			fetchCpuUsage(config)
		} catch (e: Exception) {
			e.printStackTrace()
			continue
		}

		val cpuTemp = try {
			fetchTemperature(config)
		} catch (e: Exception) {
			e.printStackTrace()
			continue
		}

		config.output.println("$t,$cpuUsage,$cpuTemp")
	}
}

class Config(parser: ArgParser) {
	val remote: Boolean by parser.flagging("-r", "--remote", help = "Execute remotely?")
	private val _output: PrintStream by parser.storing("-o", "--output", help = "Output file name") {
		if (this == "-") System.out
		else PrintStream(File(this), StandardCharsets.UTF_8)
	}.default {
		return@default System.out
	}

	val output by lazy(this::_output)
}

