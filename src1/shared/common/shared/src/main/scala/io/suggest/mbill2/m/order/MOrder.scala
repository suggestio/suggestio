package io.suggest.mbill2.m.order

import java.time.OffsetDateTime

import io.suggest.dt.CommonDateTimeUtil
import io.suggest.mbill2.m.gid._
import io.suggest.primo.id.OptId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.09.18 16:58
  * Description: billing-модель одного ордера (заказа) - одного ряда таблицы order.
  */

object MOrder {

  /** Поддержка play-json для инстансов [[MOrder]]. */
  implicit def mOrderFormat: OFormat[MOrder] = {
    val dtFmt = CommonDateTimeUtil.Implicits.offsetDateTimeFormat
    (
      (__ \ "s").format[MOrderStatus] and
      (__ \ "c").format[Gid_t] and
      (__ \ "a").format[OffsetDateTime](dtFmt) and
      (__ \ "z").format[OffsetDateTime](dtFmt) and
      (__ \ "i").formatNullable[Gid_t]
    )(apply, unlift(unapply))
  }


  implicit def univEq: UnivEq[MOrder] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


case class MOrder(
                   status        : MOrderStatus,
                   contractId    : Gid_t,
                   dateCreated   : OffsetDateTime    = OffsetDateTime.now(),
                   dateStatus    : OffsetDateTime    = OffsetDateTime.now(),
                   id            : Option[Gid_t]     = None
                 )
  extends OptId[Gid_t]
{

  def withStatus(status1: MOrderStatus): MOrder = {
    copy(
      status      = status1,
      dateStatus  = OffsetDateTime.now()
    )
  }

}

