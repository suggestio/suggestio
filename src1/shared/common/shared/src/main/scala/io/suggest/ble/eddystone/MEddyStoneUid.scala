package io.suggest.ble.eddystone

/** EddyStone-UID фрейм маячка.
  *
  * @param rssi Ощущаемая устройством мощность сигнала.
  *             None значит, что не удалось узнать мощность сигнала.
  * @param txPower Мощность.
  * @param uid id UID-маячка.
  *           Т.к. на неужное разделение 16 байт id на Namespace ID и Beacon ID нам вообще параллельно,
  *           то тут hex-строка с 16 байтами полного id.
  */
case class MEddyStoneUid(
                          override val rssi     : Option[Int],
                          override val txPower  : Int,
                          uid                   : String
                          //url                   : Option[String] = None
                        )
  extends IEddyStoneTxSignal
{

  override def beaconUid = Some(uid)

  override def frameType = MFrameTypes.UID

}
