package us.albertim.fan.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SensorsEntity(id: EntityID<Int>) : IntEntity(id), HasTimestamp {
  companion object : IntEntityClass<SensorsEntity>(SensorsTable)

  override var timestamp by SensorsTable.timestamp

  var tempInlet: Double by SensorsTable.tempInlet
  var tempExhaust: Double by SensorsTable.tempExhaust
  var tempCpu1: Double by SensorsTable.tempCpu1
  var tempCpu2: Double by SensorsTable.tempCpu2
  var rpmFan1: Double by SensorsTable.rpmFan1
  var rpmFan2: Double by SensorsTable.rpmFan2
  var rpmFan3: Double by SensorsTable.rpmFan3
  var rpmFan4: Double by SensorsTable.rpmFan4
  var rpmFan5: Double by SensorsTable.rpmFan5
  var rpmFan6: Double by SensorsTable.rpmFan6
}