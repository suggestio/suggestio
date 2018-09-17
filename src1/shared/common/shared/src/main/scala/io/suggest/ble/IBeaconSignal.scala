package io.suggest.ble

import io.suggest.common.radio.IDistantRadioSignal
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.07.17 14:31
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


object IBeaconSignal {

  implicit def univEq: UnivEq[IBeaconSignal] = UnivEq.force

}
