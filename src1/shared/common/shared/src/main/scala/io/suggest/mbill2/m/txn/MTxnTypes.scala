package io.suggest.mbill2.m.txn

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 20:55
  * Description: Типы транзакций, чтобы не писать однотипные комменты, а потом не парсить их.
  */
object MTxnTypes extends StringEnum[MTxnType] {

  /** Draft transaction information. Transaction is not paid yet, awaiting for changes and updates. */
  case object Draft extends MTxnType("d")

  /** Обычный платеж. Покупатель оплачивает через некую платёжную систему. */
  case object Payment extends MTxnType("p")

  /** Частичная отмена платежа за что-либо назад на баланс.
    * Например, оплаченная услуга не прошла модерацию. */
  case object ReturnToBalance extends MTxnType("r")

  /** Некий доход на баланс. Изначально - ручное рисование средств через SysBilling. */
  case object Income extends MTxnType("i")

  /** Draft is cancelled, and not paid.
    * Stored to handle cases, when cancelled, but payment is received later after cancel. */
  case object Cancelled extends MTxnType("c")


  override def values = findValues

}

sealed abstract class MTxnType(override val value: String) extends StringEnumEntry

object MTxnType {

  @inline implicit def univEq: UnivEq[MTxnType] = UnivEq.derive

  def unapplyStrId(x: MTxnType): Option[String] = {
    Some( x.value )
  }

  implicit def mTxnTypeFormat: Format[MTxnType] = (
    EnumeratumUtil.valueEnumEntryFormat( MTxnTypes )
  )

  implicit final class TxnTypeExt( private val txnType: MTxnType ) extends AnyVal {
    def i18nCode = "_transaction.type." + txnType.value
  }

}
