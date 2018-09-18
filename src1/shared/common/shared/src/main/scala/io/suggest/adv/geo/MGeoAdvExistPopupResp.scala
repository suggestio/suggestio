package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.dt.interval.MRangeYmdOpt
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 15:23
  * Description: Модель ответа сервера на запрос данных попапа над геошейпом гео-размещения.
  */
object MGeoAdvExistPopupResp {

  implicit val pickler: Pickler[MGeoAdvExistPopupResp] = {
    implicit val rowP = MGeoAdvExistRow.pickler
    generatePickler[MGeoAdvExistPopupResp]
  }

  @inline implicit def univEq: UnivEq[MGeoAdvExistPopupResp] = UnivEq.derive

}

/**
  * Модель ответа сервера на запрос данных для рендера попапа по поводу георазмещений.
  *
  * @param rows Ряды в списке попапа.
  * @param haveMore Есть ли ещё элементы, которые не были переданы?
  */
case class MGeoAdvExistPopupResp(
                                  rows      : Seq[MGeoAdvExistRow],
                                  haveMore  : Boolean
                                )



object MGeoAdvExistRow {
  implicit val pickler: Pickler[MGeoAdvExistRow] = {
    implicit val dateRangeOptP = MRangeYmdOpt.mRangeYmdOptPickler
    implicit val geoItemInfoP = MGeoItemInfo.mGeoItemInfoPickler
    generatePickler[MGeoAdvExistRow]
  }
  @inline implicit def univEq: UnivEq[MGeoAdvExistRow] = UnivEq.derive
}
/** Кроссплатформенная модель данных по одному ряду в списке рядов просматриваемого шейпа. */
case class MGeoAdvExistRow(
                            dateRange       : MRangeYmdOpt,
                            items           : Seq[MGeoItemInfo]
                          )


/** Модель инфы по одному гео-item'у. */
case class MGeoItemInfo(
  itemId          : Long,
  isOnlineNow     : Boolean,
  payload         : MGeoItemInfoPayload
)
object MGeoItemInfo {
  implicit val mGeoItemInfoPickler: Pickler[MGeoItemInfo] = {
    implicit val payloadP = MGeoItemInfoPayload.pickler
    generatePickler[MGeoItemInfo]
  }
  @inline implicit def univEq: UnivEq[MGeoItemInfo] = UnivEq.derive
}


/** Модель описания полезной нагрузки в item'ах. */
object MGeoItemInfoPayload {
  implicit val pickler: Pickler[MGeoItemInfoPayload] = {
    compositePickler[MGeoItemInfoPayload]
      .addConcreteType[OnMainScreen.type]
      .addConcreteType[InGeoTag]
      .addConcreteType[OnAdvsMap.type]
      .addConcreteType[OnGeoCapturing.type]
  }
  @inline implicit def univEq: UnivEq[MGeoItemInfoPayload] = UnivEq.derive
}
sealed trait MGeoItemInfoPayload

// lk-adv-geo
case object OnMainScreen extends MGeoItemInfoPayload
final case class InGeoTag(tagFace: String) extends MGeoItemInfoPayload

// lk-adn-map
case object OnAdvsMap extends MGeoItemInfoPayload
case object OnGeoCapturing extends MGeoItemInfoPayload
