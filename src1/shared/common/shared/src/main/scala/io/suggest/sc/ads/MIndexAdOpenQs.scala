package io.suggest.sc.ads

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.2021 11:18
  * Description: QS-модель параметров процедуры index-ad-open, когда сервер присылает index вместо карточек.
  */
object MIndexAdOpenQs {

  object Fields {
    def WITH_BLE_BEACON_ADS = "b"
  }

  /** 2021-02-12: Конвертация из старого boolean-формата. */
  def fromFocIndexAdOpenEnabled(isEnabled: Boolean): Option[MIndexAdOpenQs] = {
    Option.when( isEnabled )(
      MIndexAdOpenQs(
        withBleBeaconAds = true,
      )
    )
  }

  implicit def indexAdOpenQsJson: Format[MIndexAdOpenQs] = {
    val F = Fields
    (__ \ F.WITH_BLE_BEACON_ADS).format[Boolean]
      .inmap[MIndexAdOpenQs]( apply, _.withBleBeaconAds )
  }

  @inline implicit def univEq: UnivEq[MIndexAdOpenQs] = UnivEq.derive

}


/** Контейнер параметров index ad open.
  *
  * @param withBleBeaconAds Возвращать ли маячковые карточки в ответе index ad-open?
  *                         true - изначально так было до 2021-02-12
  *                         false - скрывать маячковые карточки при погружении в индексы.
  */
final case class MIndexAdOpenQs(
                                 // !!! Требуется хотя бы одно обязательное непустое поле, ибо qs-модель !!!
                                 withBleBeaconAds           : Boolean,
                               )
