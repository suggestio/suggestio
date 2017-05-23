package io.suggest.mbill2.m.txn

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 20:55
  * Description: Типы транзакций, чтобы не писать однотипные комменты, а потом не парсить их.
  */
object MTxnTypes extends EnumMaybeWithName with EnumApply {

  sealed protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val


  /** Обычный платеж за что-то. От покупателя. */
  val Payment = new Val("p")

  /** Откат платежа назад.
    * Например, оплаченная услуга не прошла модерацию. */
  val Rollback = new Val("r")

  /** Входящий профит. К продавку. */
  val Income   = new Val("i")

  /** Копия транзакции на стороне платежной системы.
    * Юзер оплатил на счёт CBCA в ПС, и ему закидываются деньги на баланс в S.io. */
  val PaySysTxn = new Val("s")

  /** Частичный возврат средств за прерванный, отчасти уже потраченный, item. */
  val InterruptPartialRefund = new Val("a")

  /** Возмещение денег за некачественно-оказанные услуги. */
  //val Refund   = new Val("e")

}
