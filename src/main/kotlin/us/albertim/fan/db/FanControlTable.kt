package us.albertim.fan.db

import org.jetbrains.exposed.dao.id.IntIdTable

object FanControlTable : IntIdTable() {
  val timestamp = long("timestamp").defaultExpression(CurrentTimestampExpression())
  val percent = integer("percent")
  val auto = bool("auto")
}