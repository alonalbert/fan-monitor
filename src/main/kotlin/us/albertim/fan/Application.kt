package us.albertim.fan

import io.ktor.server.engine.*
import kotlinx.html.div
import kotlinx.html.h1
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import space.kscience.plotly.*
import space.kscience.plotly.models.AxisType
import space.kscience.plotly.models.Trace
import space.kscience.plotly.server.serve
import us.albertim.fan.db.FanControlEntity
import us.albertim.fan.db.FanControlTable
import us.albertim.fan.db.SensorsEntity
import us.albertim.fan.db.SensorsTable
import java.sql.Connection
import java.time.Instant
import java.time.temporal.ChronoUnit

//private const val DATABASE_FILENAME = "test.db"
private const val DATABASE_FILENAME = "/storage/db/dell-r730xd-fan-speed.db"

fun main() {
//  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//    configureRouting()
//  }.start(wait = true)

  initializeDatabase()
  val oldest = Instant.now().minus(7, ChronoUnit.DAYS).epochSecond

  val sensors = transaction {
    SensorsEntity.find { SensorsTable.timestamp greater oldest }.map { it }
  }
  val fanControl = transaction {
    FanControlEntity.find { FanControlTable.timestamp greater oldest }.map {
      it.apply {
        if (auto) {
          percent = 100
        }
      }
    }
  }
  val sensorTimestamps = sensors.map { it.timestamp }
  val tempTraces = listOf(
    createTrace("Inlet", sensors, sensorTimestamps) { it.tempInlet },
    createTrace("Exhaust", sensors, sensorTimestamps) { it.tempExhaust },
    createTrace("CPU 1", sensors, sensorTimestamps) { it.tempCpu1 },
    createTrace("CPU 2", sensors, sensorTimestamps) { it.tempCpu2 },
  )
  val rpmTraces = listOf(
    createTrace("Fan 1 RPM", sensors, sensorTimestamps) { it.rpmFan1 },
    createTrace("Fan 2 RPM", sensors, sensorTimestamps) { it.rpmFan2 },
    createTrace("Fan 3 RPM", sensors, sensorTimestamps) { it.rpmFan3 },
    createTrace("Fan 4 RPM", sensors, sensorTimestamps) { it.rpmFan4 },
    createTrace("Fan 5 RPM", sensors, sensorTimestamps) { it.rpmFan5 },
    createTrace("Fan 6 RPM", sensors, sensorTimestamps) { it.rpmFan6 },
  )
  val fanPowerTrace = createTrace("Fan power (%)", fanControl, fanControl.map { it.timestamp }) { it.percent }

  createServer(tempTraces, fanPowerTrace, rpmTraces).start(wait = true)
}

private fun createServer(
  tempTraces: List<Trace>,
  fanPowerTrace: Trace,
  rpmTraces: List<Trace>
): ApplicationEngine {
  val server = Plotly.serve(host = "0.0.0.0", port = 3333) {
    val tempPlot = Plotly.plot {
      traces(tempTraces)
      layout {
        title = "Temp's"
        xaxis.type = AxisType.date
      }
    }
    val fanPowerPlot = Plotly.plot {
      traces(fanPowerTrace)
      layout {
        title = "Fan Power"
        xaxis.type = AxisType.date
      }
    }
    val rpmPlot = Plotly.plot {
      traces(rpmTraces)
      layout {
        title = "Fan RPM"
        xaxis.type = AxisType.date
      }
    }
    page {
      h1 { +"Fan Control Status" }
      div {
        plot(tempPlot)
      }
      div {
        plot(fanPowerPlot)
      }
      div {
        plot(rpmPlot)
      }
    }
  }
  return server
}

private fun initializeDatabase() {
  Database.connect("jdbc:sqlite:$DATABASE_FILENAME", "org.sqlite.JDBC")
  val transactionSerializable = Connection.TRANSACTION_SERIALIZABLE
  TransactionManager.manager.defaultIsolationLevel = transactionSerializable

  transaction {
    SchemaUtils.create(SensorsTable, FanControlTable)
  }
}

private fun <T> createTrace(
  name: String,
  sensors: Iterable<T>,
  timestamps: List<Long>,
  function: (T) -> Int
) = Trace().apply {
  this.name = name
  x.set(timestamps)
  y.set(sensors.map(function))
}



