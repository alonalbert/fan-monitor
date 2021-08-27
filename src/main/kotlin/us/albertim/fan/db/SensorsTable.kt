package us.albertim.fan.db

import org.jetbrains.exposed.dao.id.IntIdTable

object SensorsTable : IntIdTable() {
  val timestamp = long("timestamp").defaultExpression(CurrentTimestampExpression())
  val tempInlet = double("temp_inlet")
  val tempExhaust = double("temp_exhaust")
  val tempCpu1 = double("temp_cpu1")
  val tempCpu2 = double("temp_cpu2")
  val rpmFan1 = double("rpm_fan1")
  val rpmFan2 = double("rpm_fan2")
  val rpmFan3 = double("rpm_fan3")
  val rpmFan4 = double("rpm_fan4")
  val rpmFan5 = double("rpm_fan5")
  val rpmFan6 = double("rpm_fan6")
}