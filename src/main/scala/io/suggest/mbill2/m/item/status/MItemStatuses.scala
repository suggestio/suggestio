package io.suggest.mbill2.m.item.status

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:55
 * Description: Статусы обработки item'а.
 */
object MItemStatuses extends EnumMaybeWithName with EnumApply {

  protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT
  {
    /** Деньги заблокированы на счете покупателя? */
    def isMoneyBlockedOnBuyer   : Boolean = false

    /** Списаны ли деньги с основного баланса покупателя? */
    def isMoneyWithdrawed       : Boolean = true

    /** Деньги ушли от байера к селлеру и заблокированы на балансе селлера? */
    def isMoneyBlockedOnSeller  : Boolean = false
  }

  override type T = Val

  /** Item лежит в корзине, т.е. в черновике заказа. */
  val Draft               : T = new Val("a") {
    /** Пока товар в корзине, ничего никуда не списано. */
    override def isMoneyWithdrawed      = false
  }

  /** Item оплачен. Ожидается какая-то автоматическая реакция suggest.io.
    * Например, юзер оплатил размещение карточки. Sio должен разместить карточку и обновить статус. */
  val AwaitingSioAuto     : T = new Val("b") {
    /** Деньги списаны с доступного баланса на заблокированный. */
    override def isMoneyBlockedOnBuyer  = true
  }

  /** Отказано продавцом или поставщиком услуги. Например, размещение не прошло модерацию. */
  val CancelledBySeller   : T = new Val("c") {
    /** Деньги уже возвращены на счет счет покупателя. */
    override def isMoneyWithdrawed      = false
  }


  /** Купленная услуга пока в оффлайне. */
  val Offline             : T = new Val("f")

  /** Оплаченная услуга сейчас в онлайне. */
  val Online              : T = new Val("o")

  /** Завершена обработка item'а. Например, оплаченная услуга истекла. */
  val Finished            : T = new Val("z")


}



