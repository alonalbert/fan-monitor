package us.albertim.fan

import io.ktor.server.engine.*
import kotlinx.coroutines.*
import kotlinx.html.div
import kotlinx.html.h1
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import space.kscience.plotly.Plotly
import space.kscience.plotly.layout
import space.kscience.plotly.models.AxisType
import space.kscience.plotly.models.Trace
import space.kscience.plotly.plot
import space.kscience.plotly.server.pushUpdates
import space.kscience.plotly.server.serve
import us.albertim.fan.db.*
import java.io.File
import java.sql.Connection
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

@DelicateCoroutinesApi
@ExperimentalTime
fun main(args: Array<String>) {
  initializeDatabase(args[0])
  val temperatures: List<TraceHelper<SensorsEntity>> =
    listOf(
      TraceHelper("Inlet", movingAverageSize = 10) { it.tempInlet },
      TraceHelper("Exhaust", movingAverageSize = 10) { it.tempExhaust },
      TraceHelper("CPU 1", movingAverageSize = 10) { it.tempCpu1 },
      TraceHelper("CPU 2", movingAverageSize = 10) { it.tempCpu2 },
    )

  val averageFanRpm: (SensorsEntity) -> Double = {
    listOf(it.rpmFan1, it.rpmFan2, it.rpmFan3, it.rpmFan4, it.rpmFan5, it.rpmFan6).average()
  }
  val rpms: List<TraceHelper<SensorsEntity>> = listOf(
    TraceHelper("Fan RPM", color = "rgba(0, 0, 0, 0.2)", getValue = averageFanRpm),
    TraceHelper("Fan RPM Average", color = "red", movingAverageSize = 10, getValue = averageFanRpm),
  )

  val fixFanPower: (FanControlEntity) -> Double = { if (it.auto) 56.0 else it.percent }
  val fanPowers: List<TraceHelper<FanControlEntity>> = listOf(
    TraceHelper("Fan power Average (%)", color = "green", movingAverageSize = 10, getValue = fixFanPower),
    TraceHelper("Fan power (%)", color = "rgba(0, 0, 0, 0.2)", getValue = fixFanPower),
  )

  updateTraces(temperatures, rpms, fanPowers)

  GlobalScope.launch {
    while (isActive) {
      updateTraces(temperatures, rpms, fanPowers)
      delay(Duration.minutes(1))
    }
  }

  createServer(temperatures.map { it.trace }, fanPowers.map { it.trace }, rpms.map { it.trace }).start(wait = true)
}

private fun updateTraces(
  temperatures: List<TraceHelper<SensorsEntity>>,
  rpms: List<TraceHelper<SensorsEntity>>,
  fanPowers: List<TraceHelper<FanControlEntity>>
) {
  val oldest = Instant.now().minus(7, ChronoUnit.DAYS).epochSecond
  val sensors = transaction {
    SensorsEntity.find { SensorsTable.timestamp greater oldest }.map { it }
  }
  val sensorsTimestamps = getTimestamps(sensors)
  for (temperature in temperatures + rpms) {
    temperature.update(sensors, sensorsTimestamps)
  }

  val fanControl = transaction {
    FanControlEntity.find { FanControlTable.timestamp greater oldest }.map { it }
  }

  for (fanPower in fanPowers) {
    fanPower.update(fanControl, getTimestamps(fanControl))
  }
}

@ExperimentalTime
private fun createServer(
  temperatures: List<Trace>,
  fanPower: List<Trace>,
  rpms: List<Trace>
): ApplicationEngine {
  val server = Plotly.serve(host = "0.0.0.0", port = 3333) {
    embedData = true

    page { renderer ->
      h1 { +"Fan Control Status" }
      div {
        plot(renderer = renderer) {
          traces(temperatures)
          layout {
            title = "Temp's"
            xaxis.type = AxisType.date
          }
        }
      }
      div {
        plot(renderer = renderer) {
          traces(fanPower)
          layout {
            title = "Fan Power"
            xaxis.type = AxisType.date
          }
        }
      }
      div {
        plot(renderer = renderer) {
          traces(rpms)
          layout {
            title = "Fan RPM"
            xaxis.type = AxisType.date
          }
        }
      }
    }
    pushUpdates(Duration.minutes(1).inWholeMilliseconds.toInt())
  }
  return server
}

private fun initializeDatabase(filename: String) {
  Database.connect("jdbc:sqlite:${File(filename).absoluteFile}", "org.sqlite.JDBC")
  val transactionSerializable = Connection.TRANSACTION_SERIALIZABLE
  TransactionManager.manager.defaultIsolationLevel = transactionSerializable

  transaction {
    SchemaUtils.create(SensorsTable, FanControlTable)
  }
}

private fun <T : HasTimestamp> getTimestamps(list: List<T>) = list.map {
  LocalDateTime.ofInstant(
    Instant.ofEpochSecond(it.timestamp),
    ZoneId.systemDefault()
  ).format(formatter)
}

private class TraceHelper<T : HasTimestamp>(
  name: String,
  private val movingAverageSize: Int = 1,
  private val color: String? = null,
  private val getValue: (T) -> Double
) {
  val trace = Trace().apply {
    this.name = name
    if (color != null) {
      line {
        color(this@TraceHelper.color)
      }
    }
  }

  fun update(entities: List<T>, timestamps: List<String>) {
    trace.y.set(entities.map<T, Double> { getValue(it) }
      .windowed(movingAverageSize, step = 1, partialWindows = true) { it.average() })
    trace.x.set(timestamps)
  }
}

