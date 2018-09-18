package io.suggest.mbill2.m.txn

import java.time.OffsetDateTime

import io.suggest.bill.Amount_t
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.primo.id.OptId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.09.18 17:18
  * Description: Модель одной транзакции.
  */

object MTxn {

  /** Поддержка play-json. */
  implicit def mTxnFormat: OFormat[MTxn] = {
    val dtFmt = CommonDateTimeUtil.Implicits.offsetDateTimeFormat
    (
      (__ \ "b").format[Gid_t] and
      (__ \ "n").format[Amount_t] and
      (__ \ "t").format[MTxnType] and
      (__ \ "o").formatNullable[Gid_t] and
      (__ \ "d").formatNullable[Gid_t] and
      (__ \ "c").formatNullable[String] and
      (__ \ "u").formatNullable[String] and
      (__ \ "a").formatNullable[OffsetDateTime](dtFmt) and
      (__ \ "p").format[OffsetDateTime](dtFmt) and
      (__ \ "i").formatNullable[Gid_t]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MTxn] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


case class MTxn(
                 balanceId         : Gid_t,
                 amount            : Amount_t,
                 txType            : MTxnType,
                 orderIdOpt        : Option[Gid_t]             = None,
                 itemId            : Option[Gid_t]             = None,
                 paymentComment    : Option[String]            = None,
                 psTxnUidOpt       : Option[String]            = None,
                 datePaid          : Option[OffsetDateTime]    = None,
                 dateProcessed     : OffsetDateTime            = OffsetDateTime.now(),
                 override val id   : Option[Gid_t]             = None
               )
  extends OptId[Gid_t]

