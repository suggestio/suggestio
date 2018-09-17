package io.suggest.mbill2.m.item

import java.time.OffsetDateTime

import io.suggest.bill.{IMPrice, MPrice}
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.geo.IGeoShape
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.primo.id.OptId
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.09.18 15:57
  * Description: Модель одного элемента заказа (ряда в таблице MItem).
  */


object MItem {

  /** Поддержка play-json для инстансом [[MItem]]. */
  implicit def mItemFormat: OFormat[MItem] = {
    val dtFmt = CommonDateTimeUtil.Implicits.offsetDateTimeFormat
    (
      (__ \ "o").format[Gid_t] and
      (__ \ "t").format[MItemType] and
      (__ \ "s").format[MItemStatus] and
      (__ \ "p").format[MPrice] and
      (__ \ "n").format[String] and
      (__ \ "a").formatNullable[OffsetDateTime](dtFmt) and
      (__ \ "z").formatNullable[OffsetDateTime](dtFmt) and
      (__ \ "r").formatNullable[String] and
      (__ \ "e").formatNullable[String] and
      (__ \ "g").formatNullable[IGeoShape]( IGeoShape.JsonFormats.internalMinFormat ) and
      (__ \ "f").formatNullable[String] and
      (__ \ "y").formatNullable[String] and
      (__ \ "u").format[OffsetDateTime](dtFmt) and
      (__ \ "i").formatNullable[Gid_t]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MItem] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


case class MItem(
                  orderId                 : Gid_t,
                  iType                   : MItemType,
                  status                  : MItemStatus,
                  override val price      : MPrice,
                  nodeId                  : String,
                  dateStartOpt            : Option[OffsetDateTime],
                  dateEndOpt              : Option[OffsetDateTime],
                  rcvrIdOpt               : Option[String]          = None,
                  reasonOpt               : Option[String]          = None,
                  geoShape                : Option[IGeoShape]       = None,
                  tagFaceOpt              : Option[String]          = None,
                  tagNodeIdOpt            : Option[String]          = None,
                  dateStatus              : OffsetDateTime          = OffsetDateTime.now(),
                  override val id         : Option[Gid_t]           = None
                )
  extends OptId[Gid_t]
  with IMPrice
{

  /** @return Инстанс [[MItem]] с новым статусом и датой обновления оного. */
  def withStatus(status1: MItemStatus): MItem = {
    copy(
      status      = status1,
      dateStatus  = OffsetDateTime.now()
    )
  }

}
