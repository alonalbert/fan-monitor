package us.albertim.fan.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SensorsEntity(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<SensorsEntity>(SensorsTable)

  var timestamp by SensorsTable.timestamp
  var tempInlet by SensorsTable.tempInlet
  var tempExhaust by SensorsTable.tempExhaust
  var tempCpu1 by SensorsTable.tempCpu1
  var tempCpu2 by SensorsTable.tempCpu2
  var rpmFan1 by SensorsTable.rpmFan1
  var rpmFan2 by SensorsTable.rpmFan2
  var rpmFan3 by SensorsTable.rpmFan3
  var rpmFan4 by SensorsTable.rpmFan4
  var rpmFan5 by SensorsTable.rpmFan5
  var rpmFan6 by SensorsTable.rpmFan6
}