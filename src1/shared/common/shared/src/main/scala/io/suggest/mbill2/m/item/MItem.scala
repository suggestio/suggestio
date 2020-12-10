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
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.09.18 15:57
  * Description: Модель одного элемента заказа (ряда в таблице MItem).
  */


object MItem {

  object Fields {
    final def ORDER_ID = "o"
    final def ITEM_TYPE = "t"
    final def STATUS = "s"
    final def PRICE = "p"
    final def NODE_ID = "n"
    final def DATE_START = "a"
    final def DATE_END = "z"
    final def RCVR_ID = "r"
    final def REASON = "e"
    final def GEO_SHAPE = "g"
    final def TAG_FACE = "f"
    final def TAG_NODE_ID = "y"
    final def DATE_STATUS = "u"
    final def ID = "i"
  }

  /** Поддержка play-json для инстансом [[MItem]]. */
  implicit def mItemFormat: OFormat[MItem] = {
    val F = Fields
    val dtFmt = CommonDateTimeUtil.Implicits.offsetDateTimeFormat
    (
      (__ \ F.ORDER_ID).format[Gid_t] and
      (__ \ F.ITEM_TYPE).format[MItemType] and
      (__ \ F.STATUS).format[MItemStatus] and
      (__ \ F.PRICE).format[MPrice] and
      (__ \ F.NODE_ID).format[String] and
      (__ \ F.DATE_START).formatNullable[OffsetDateTime](dtFmt) and
      (__ \ F.DATE_END).formatNullable[OffsetDateTime](dtFmt) and
      (__ \ F.RCVR_ID).formatNullable[String] and
      (__ \ F.REASON).formatNullable[String] and
      (__ \ F.GEO_SHAPE).formatNullable[IGeoShape]( IGeoShape.JsonFormats.minimalFormat ) and
      (__ \ F.TAG_FACE).formatNullable[String] and
      (__ \ F.TAG_NODE_ID).formatNullable[String] and
      (__ \ F.DATE_STATUS).format[OffsetDateTime](dtFmt) and
      (__ \ F.ID).formatNullable[Gid_t]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MItem] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  def price = GenLens[MItem]( _.price )

}


final case class MItem(
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
