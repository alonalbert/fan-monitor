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
  val inletTemp = TraceHelper<SensorsEntity>("Inlet") { it.tempInlet }
  val exhaustTemp = TraceHelper<SensorsEntity>("Exhaust") { it.tempExhaust }
  val cpu1Temp = TraceHelper<SensorsEntity>("CPU 1") { it.tempCpu1 }
  val cpu2Temp = TraceHelper<SensorsEntity>("CPU 2") { it.tempCpu2 }
  val temperatures = listOf(inletTemp, exhaustTemp, cpu1Temp, cpu2Temp)

  val fan1Rpm = TraceHelper<SensorsEntity>("Fan 1 RPM") { it.rpmFan1 }
  val fan2Rpm = TraceHelper<SensorsEntity>("Fan 2 RPM") { it.rpmFan2 }
  val fan3Rpm = TraceHelper<SensorsEntity>("Fan 3 RPM") { it.rpmFan3 }
  val fan4Rpm = TraceHelper<SensorsEntity>("Fan 4 RPM") { it.rpmFan4 }
  val fan5Rpm = TraceHelper<SensorsEntity>("Fan 5 RPM") { it.rpmFan5 }
  val fan6Rpm = TraceHelper<SensorsEntity>("Fan 6 RPM") { it.rpmFan6 }
  val rpms = listOf(fan1Rpm, fan2Rpm, fan3Rpm, fan4Rpm, fan5Rpm, fan6Rpm)

  val fanPowers = listOf(TraceHelper<FanControlEntity>("Fan power (%)") { if (it.auto) 56 else it.percent })

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

private class TraceHelper<T : HasTimestamp>(name: String, private val getValue: (T) -> Int) {
  val trace = Trace().apply { this.name = name }

  fun update(values: List<T>, timestamps: List<String>) {
    trace.y.set(values.map { getValue(it) })
    trace.x.set(timestamps)
  }
}

