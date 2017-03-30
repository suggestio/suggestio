package io.suggest.ble

import io.suggest.common.radio.IDistantRadioSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:26
  * Description: Трейт наблюдаемых данных по ровно одному маячку.
  * Изначально назывался IBeacon, но это слегка конфликтовало с ябло-брендом.
  */
trait BeaconSignal extends IDistantRadioSignal {

  /** Некий внутренний уникальный id/ключ маячка, если он вообще есть.
    *
    * Option[], потому что в гугле смогли уродить маячки с каким-то кастрированными URL внутри фрейма.
    * Скорее всего, маячки без uid останутся бесполезны для s.io и в будущем.
    */
  def uid: Option[String]

}


/** Итоговые полезные данные по маячку: ключ маячка и расстояние до него. */
trait BeaconData {

  /** id маячка. */
  def uid: String

  /** Расстояние до маячка в сантиметрах. */
  def distanceCm: Int

}
