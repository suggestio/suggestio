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

  /** Обычный платеж за что-то. От покупателя. */
  case object Payment extends MTxnType("p")

  /** Откат платежа назад.
    * Например, оплаченная услуга не прошла модерацию. */
  case object Rollback extends MTxnType("r")

  /** Входящий профит. К продавку. */
  case object Income extends MTxnType("i")

  /** Копия транзакции на стороне платежной системы.
    * Юзер оплатил на счёт CBCA в ПС, и ему закидываются деньги на баланс в S.io. */
  case object PaySysTxn extends MTxnType("s")

  /** Частичный возврат средств за прерванный, отчасти уже потраченный, item. */
  case object InterruptPartialRefund extends MTxnType("a")

  /** Возмещение денег за некачественно-оказанные услуги. */
  //val Refund   = new Val("e")

  override val values = findValues

}

sealed abstract class MTxnType(override val value: String) extends StringEnumEntry

object MTxnType {

  implicit def univEq: UnivEq[MTxnType] = UnivEq.derive

  def unapplyStrId(x: MTxnType): Option[String] = {
    Some( x.value )
  }

  implicit def mTxnTypeFormat: Format[MTxnType] = (
    EnumeratumUtil.valueEnumEntryFormat( MTxnTypes )
  )

}
