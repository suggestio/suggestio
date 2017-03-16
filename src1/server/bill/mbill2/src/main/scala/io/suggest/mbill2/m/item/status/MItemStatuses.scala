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

    /** Является ли рекламная карточка на rcvr-узле "занятой", если размещение в данном статусе?
      * Да для ожидающих модерации, одобренных к размещению и размещенных. */
    def isAdvBusy               : Boolean = true

    /** Актуальный статус для размещения, это когда item ещё не прошел свой жизненный цикл. */
    def isAdvActual             : Boolean = true

    /** Является ли карточка заапрувленной к размещению в настоящем/будущем времени?
      * @return true для Online и Offline.
      *         false -- остальные.
      */
    def isAdvBusyApproved       : Boolean = false

  }

  /** Укороченное выставление флага isBusy = false. */
  sealed protected[this] trait NotBusy extends Val {
    /** Использованные размещения, отклонённые и ещё неоплаченные не являются занятыми. */
    override def isAdvBusy = false
  }

  /** Трейт подмешивается для статусов, неактуальных в плане рекламного размещения, т.к. окончательных. */
  sealed protected[this] trait AdvInactual extends Val {
    override def isAdvActual = false
  }

  /** Трейт для выставления флага isAdvBusyApproved в true. */
  sealed protected[this] trait AdvBusyApproved extends Val {
    override def isAdvBusyApproved = true
  }


  override type T = Val


  // ДЛЯ strId НАДО ПОДДЕРЖИВАТЬ АЛФАВИТНЫЙ ПОРЯДОК ЭЛЕМЕНТОВ, И ЧЁТКО соответствующий хронологическому!
  // Это связано со всякими SQL-оборотами типа "SELECT max(status) ..."

  /** Item лежит в корзине, т.е. в черновике заказа. */
  val Draft               : T = new Val("a") with NotBusy {
    /** Пока товар в корзине, ничего никуда не списано. */
    override def isMoneyWithdrawed      = false
  }

  /** Item оплачен. Ожидается подтверждение со стороны suggest.io: модерация. */
  val AwaitingMdr     : T = new Val("b") {
    /** Деньги списаны с доступного баланса на заблокированный. */
    override def isMoneyBlockedOnBuyer  = true
  }

  /** Отказано продавцом или поставщиком услуги. Например, размещение не прошло модерацию. */
  val Refused   : T = new Val("c") with NotBusy with AdvInactual {
    /** Деньги уже возвращены на счет счет покупателя. */
    override def isMoneyWithdrawed      = false
  }

  private class _ValBusyApproved(name: String)
    extends Val(name)
    with AdvBusyApproved


  /** Купленная услуга пока в оффлайне. */
  val Offline             : T = new _ValBusyApproved("f")

  /** Оплаченная услуга сейчас в онлайне. */
  val Online              : T = new _ValBusyApproved("o")

  /** Завершена обработка item'а. Например, оплаченная услуга истекла. */
  val Finished            : T = new Val("z") with NotBusy with AdvInactual


  /** Статусы, обозначающие занятость карточки для прямого размещения. */
  def advBusy = valuesT.iterator.filter(_.isAdvBusy)
  /** Только id'шники, идентефицирующие занятость карточки. */
  def advBusyIds = onlyIds(advBusy)


  /** Статусы, обозначающие текущую актуальность adv-item'а, т.е. незаконченность его ЖЦ. */
  def advActual = valuesT.iterator.filter(_.isAdvActual)
  /** id статусов, обозначающих текущую актуальность adv-item'а. */
  def advActualIds = onlyIds(advBusy)


  def advDone = valuesT.iterator.filter { it => !it.isAdvActual }
  def advDoneIds = onlyIds( advDone )

}
