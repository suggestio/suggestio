package io.suggest.ble.beaconer.m.beacon.google

import io.suggest.common.radio.Beacon

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:36
  * Description: Модель payload'а по одному eddystone-маячку.
  *
  * Для упрощения, поддержка URL пока отброшена.
 *
  * @param uid id маячка, если есть.
  *            Т.к. на неужное разделение 16 байт id на Namespace ID и Beacon ID нам вообще параллельно,
  *            то тут base64-строка с 16 байтами полного id.
  */
case class EddyStone(
  override val rssi     : Int,
  txPower               : Int,
  override val uid      : Option[String]
  //url                   : Option[String] = None
)
  extends Beacon
{

  override def distance0m = 0
  override def rssi0 = txPower

}
