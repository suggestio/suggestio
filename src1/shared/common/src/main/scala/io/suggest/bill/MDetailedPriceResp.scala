package io.suggest.bill

import boopickle.Default._
import io.suggest.cal.m.MCalType
import io.suggest.dt.MYmd

/** Модель детализованных данных по стоимости размещения. */

case class MDetailedPriceResp(
                               blockModulesCount  : Int,
                               onMainScreenMult   : Option[Double],
                               geoInfo            : Option[MGeoInfo],
                               days               : Seq[MDayPriceInfo]
                             )
object MDetailedPriceResp {
  implicit val mDetailedPriceRespPickler: Pickler[MDetailedPriceResp] = {
    implicit val mGeoInfoP = MGeoInfo.mGeoInfoPickler
    implicit val mDayPriceInfoP = MDayPriceInfo.mDayPriceInfoPickler
    //implicit val mPriceP = MPrice.mPricePickler
    generatePickler[MDetailedPriceResp]
  }
}


/** Инфа по стоимости одного дня.
  *
  * @param ymd Дата.
  * @param calType Тип используемого календаря.
  * @param baseDayPrice Базовая (минимальная) цена одного дня (без учёта площади и мультипликаторов).
  * @param price Итоговая стоимости одного дня.
  */
case class MDayPriceInfo(
                          ymd           : MYmd,
                          calType       : MCalType,
                          baseDayPrice  : MPrice,
                          price         : MPrice
                        ) {
  def withPrice(price2: MPrice) = copy(price = price2)
}
object MDayPriceInfo {
  implicit val mDayPriceInfoPickler: Pickler[MDayPriceInfo] = {
    implicit val mYmdP = MYmd.mYmdPickler
    implicit val calTypeP = MCalType.mCalTypePickler
    implicit val mPriceP = MPrice.mPricePickler
    generatePickler[MDayPriceInfo]
  }
}


/** Инфа по географии, влияющая на цену.
  *
  * @param radiusKm Радиус в км, если круг.
  * @param priceMult Множитель цены за географию.
  */
case class MGeoInfo(
                     radiusKm   : Option[Double],
                     priceMult  : Double
                   )
object MGeoInfo {
  implicit val mGeoInfoPickler: Pickler[MGeoInfo] = {
    generatePickler[MGeoInfo]
  }
}