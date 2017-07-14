package io.suggest.ble

import io.suggest.common.radio.IDistantRadioSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:26
  * Description: Трейт наблюдаемых данных по ровно одному маячку.
  * Изначально назывался IBeacon, но это слегка конфликтовало с ябло-брендом.
  */
trait IBeaconSignal extends IDistantRadioSignal {

  /** Некий внутренний уникальный id/ключ маячка, если он вообще есть.
    *
    * Option[], потому что в гугле смогли уродить маячки с каким-то кастрированными URL внутри фрейма.
    * Скорее всего, маячки без uid останутся бесполезны для s.io и в будущем.
    */
  def beaconUid: Option[String]

}


/** Итоговые полезные данные по маячку: ключ маячка и расстояние до него. */
// TODO Удалить интерфейс, когда останется ровно одна реализация.
trait IBeaconData {

  /** id маячка. */
  def uid: String

  /** Расстояние до маячка в сантиметрах. */
  def distanceCm: Int

}


/**
  * Класс для инстансов модели с инфой о наблюдаемом в эфире BLE-маячке.
  * @param uid Уникальный идентификатор наблюдаемого маячка:
  *            iBeacon:   "$uuid:$major:$minor"
  *            EddyStone: "$gid$bid"
  * @param distanceCm Расстояние в сантиметрах, если известно.
  */
case class MBeaconData(
  override val uid          : String,
  override val distanceCm   : Int
)
  extends IBeaconData
{
  override def toString: String = "B(" + uid + "," + distanceCm + "cm)"
}
