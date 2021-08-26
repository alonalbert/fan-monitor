package us.albertim.fan.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class FanControlEntity(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<FanControlEntity>(FanControlTable)

  var timestamp by FanControlTable.timestamp
  var percent by FanControlTable.percent
  var auto by FanControlTable.auto
}