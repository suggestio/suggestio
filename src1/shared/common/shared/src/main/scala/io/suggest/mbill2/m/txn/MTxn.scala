package io.suggest.mbill2.m.txn

import java.time.OffsetDateTime
import io.suggest.bill.Amount_t
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.pay.MPaySystem
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

  object Fields {
    final def METADATA = "metadata"
    final def PAY_SYSTEM = "pay_system"
    final def PS_DEAL_ID = "ps_deal_id"
  }

  /** Поддержка play-json. */
  implicit def mTxnFormat: OFormat[MTxn] = {
    val F = Fields
    val dtFmt = CommonDateTimeUtil.Implicits.offsetDateTimeFormat
    (
      (__ \ "b").format[Gid_t] and
      (__ \ "n").format[Amount_t] and
      (__ \ "t").format[MTxnType] and
      (__ \ "o").formatNullable[Gid_t] and
      (__ \ "d").formatNullable[Gid_t] and
      (__ \ "c").formatNullable[String] and
      (__ \ F.PAY_SYSTEM).formatNullable[MPaySystem] and
      (__ \ "u").formatNullable[String] and
      (__ \ F.PS_DEAL_ID).formatNullable[String] and
      (__ \ "a").formatNullable[OffsetDateTime](dtFmt) and
      (__ \ "p").format[OffsetDateTime](dtFmt) and
      // metadata: before 2021-12-08 was used, but these test payments does not matter.
      (__ \ F.METADATA).formatNullable[MTxnMetaData] and
      (__ \ "i").formatNullable[Gid_t]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MTxn] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  implicit final class TxnExt( private val mtxn: MTxn ) extends AnyVal {

    def toClientSide = mtxn.copy(
      metadata      = None,
      psTxnUidOpt   = None,
      psDealIdOpt   = None,
      dateProcessed = mtxn.dateProcessed.withNano(0),
      datePaid      = mtxn.datePaid.map(_.withNano(0)),
    )

  }

}


final case class MTxn(
                 balanceId         : Gid_t,
                 amount            : Amount_t,
                 txType            : MTxnType,
                 orderIdOpt        : Option[Gid_t]             = None,
                 itemId            : Option[Gid_t]             = None,
                 paymentComment    : Option[String]            = None,
                 paySystem         : Option[MPaySystem]        = None,
                 psTxnUidOpt       : Option[String]            = None,
                 psDealIdOpt       : Option[String]            = None,
                 datePaid          : Option[OffsetDateTime]    = None,
                 dateProcessed     : OffsetDateTime            = OffsetDateTime.now(),
                 metadata          : Option[MTxnMetaData]      = None,
                 override val id   : Option[Gid_t]             = None
               )
  extends OptId[Gid_t]

