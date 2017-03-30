package io.suggest.ble.eddystone

/** EddyStone-UID фрейм маячка.
  *
  * @param rssi Ощущаемая устройством мощность сигнала.
  * @param txPower Мощность.
  * @param id id UID-маячка.
  *           Т.к. на неужное разделение 16 байт id на Namespace ID и Beacon ID нам вообще параллельно,
  *           то тут hex-строка с 16 байтами полного id.
  */
case class MEddyStoneUid(
  override val rssi     : Int,
  override val txPower  : Int,
  id                    : String
  //url                   : Option[String] = None
)
  extends IEddyStoneTxSignal
{

  override def uid = Some(id)

  override def frameType = MFrameTypes.Uid

}
