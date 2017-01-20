package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.dt.interval.MRangeYmdOpt

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
}


/** Модель описания полезной нагрузки в item'ах. */
object MGeoItemInfoPayload {
  implicit val pickler: Pickler[MGeoItemInfoPayload] = {
    compositePickler[MGeoItemInfoPayload]
      .addConcreteType[OnMainScreen.type]
      .addConcreteType[InGeoTag]
  }
}
sealed trait MGeoItemInfoPayload
case object OnMainScreen extends MGeoItemInfoPayload
case class InGeoTag(tagFace: String) extends MGeoItemInfoPayload
