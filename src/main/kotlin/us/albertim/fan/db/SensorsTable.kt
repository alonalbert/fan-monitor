package us.albertim.fan.db

import org.jetbrains.exposed.dao.id.IntIdTable

object SensorsTable : IntIdTable() {
  val timestamp = long("timestamp").defaultExpression(CurrentTimestampExpression())
  val tempInlet = integer("temp_inlet")
  val tempExhaust = integer("temp_exhaust")
  val tempCpu1 = integer("temp_cpu1")
  val tempCpu2 = integer("temp_cpu2")
  val rpmFan1 = integer("rpm_fan1")
  val rpmFan2 = integer("rpm_fan2")
  val rpmFan3 = integer("rpm_fan3")
  val rpmFan4 = integer("rpm_fan4")
  val rpmFan5 = integer("rpm_fan5")
  val rpmFan6 = integer("rpm_fan6")
}