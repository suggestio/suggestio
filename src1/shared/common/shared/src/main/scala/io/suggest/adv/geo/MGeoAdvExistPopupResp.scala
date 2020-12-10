package io.suggest.adv.geo

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 15:23
  * Description: Модель ответа сервера на запрос данных попапа над геошейпом гео-размещения.
  */
object MGeoAdvExistPopupResp {

  implicit def geoAdvExistRowJson: OFormat[MGeoAdvExistPopupResp] = (
    (__ \ "r").format[Seq[MGeoAdvExistRow]] and
    (__ \ "m").format[Boolean]
  )(apply, unlift(unapply))

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


// TODO Заменить row на MItem. Группировку по датам вынести с сервера на клиент.

object MGeoAdvExistRow {
  implicit def geoAdvExistRowJson: OFormat[MGeoAdvExistRow] = (
    (__ \ "d").format[MRangeYmdOpt] and
    (__ \ "i").format[Seq[MGeoItemInfo]]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MGeoAdvExistRow] = UnivEq.derive
}
/** Кроссплатформенная модель данных по одному ряду в списке рядов просматриваемого шейпа. */
case class MGeoAdvExistRow(
                            dateRange       : MRangeYmdOpt,
                            items           : Seq[MGeoItemInfo]
                          )


/** Модель инфы по одному гео-item'у. */
case class MGeoItemInfo(
                         // TODO Эту модель надо унифицировать с MItem? MItem.orderId просто сбрасывать, при необходимости.
                         itemId          : Gid_t,
                         isOnlineNow     : Boolean,
                         itemType        : MItemType,
                         tagFace         : Option[String] = None,
                       )
object MGeoItemInfo {

  implicit def geoItemInfoJson: OFormat[MGeoItemInfo] = {
    val F = MItem.Fields
    (
      (__ \ F.ID).format[Gid_t] and
      (__ \ "O").format[Boolean] and
      (__ \ F.ITEM_TYPE).format[MItemType] and
      (__ \ F.TAG_FACE).formatNullable[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MGeoItemInfo] = UnivEq.derive
}
