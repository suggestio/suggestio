package io.suggest.mbill2.m.item.status

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq
import io.suggest.enum2.EnumeratumUtil.ValueEnumEntriesOps

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:55
 * Description: Статусы обработки item'а.
 */
object MItemStatuses extends StringEnum[MItemStatus] {

  // ДЛЯ strId НАДО ПОДДЕРЖИВАТЬ АЛФАВИТНЫЙ ПОРЯДОК ЭЛЕМЕНТОВ, И ЧЁТКО соответствующий хронологическому!
  // Это связано со всякими SQL-оборотами типа "SELECT max(status) ..."

  /** Item лежит в корзине, т.е. в черновике заказа. */
  case object Draft extends MItemStatus("a") with NotBusy {

    /** Черновики обитают только в "корзине" (за искл. hold-ордера), CSS-иконки там не отображаются. */
    override def iconCssClass = None

    /** Пока товар в корзине, ничего никуда не списано. */
    override def isMoneyWithdrawed      = false
  }

  /** Item оплачен. Ожидается подтверждение со стороны suggest.io: модерация. */
  case object AwaitingMdr extends MItemStatus("b") {

    override def iconCssClass = Some( "mdr" )

    /** Деньги списаны с доступного баланса на заблокированный. */
    override def isMoneyBlockedOnBuyer  = true
  }

  /** Отказано продавцом или поставщиком услуги. Например, размещение не прошло модерацию. */
  case object Refused extends MItemStatus("c") with NotBusy with AdvInactual {
    /** Деньги уже возвращены на счет счет покупателя. */
    override def isMoneyWithdrawed      = false
    override def iconCssClass = Some("refused")
  }



  /** Купленная услуга пока в оффлайне. */
  case object Offline extends MItemStatus("f") with AdvBusyApproved {
    override def iconCssClass = Some("offline")
  }

  /** Оплаченная услуга сейчас в онлайне. */
  case object Online extends MItemStatus("o") with AdvBusyApproved {
    override def iconCssClass = Some("online")
  }

  /** Завершена обработка item'а. Например, оплаченная услуга истекла. */
  case object Finished extends MItemStatus("z") with NotBusy with AdvInactual {
    override def iconCssClass = Some("finished")
  }


  override def values = findValues


  /** Статусы, обозначающие занятость карточки для прямого размещения. */
  def advBusy = values.iterator.filter(_.isAdvBusy)
  /** Только id'шники, идентефицирующие занятость карточки. */
  def advBusyIds = advBusy.onlyIds


  /** Статусы, обозначающие текущую актуальность adv-item'а, т.е. незаконченность его ЖЦ. */
  def advActual = values.iterator.filter(_.isAdvActual)
  /** id статусов, обозначающих текущую актуальность adv-item'а. */
  def advActualIds = advBusy.onlyIds


  def advDone = values.iterator.filter { it => !it.isAdvActual }
  def advDoneIds = advDone.onlyIds

}


sealed abstract class MItemStatus(override val value: String) extends StringEnumEntry {

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

  /** Код локализованного названия по messages. */
  def nameI18n = "Item.status." + value

  /** Класс css-иконки. */
  def iconCssClass: Option[String]

}

object MItemStatus {

  implicit def univEq: UnivEq[MItemStatus] = UnivEq.derive

  def unapplyStrId(x: MItemStatus): Option[String] = {
    Some( x.value )
  }

}


/** Укороченное выставление флага isBusy = false. */
sealed protected[this] trait NotBusy extends MItemStatus {
  /** Использованные размещения, отклонённые и ещё неоплаченные не являются занятыми. */
  override def isAdvBusy = false
}

/** Трейт подмешивается для статусов, неактуальных в плане рекламного размещения, т.к. окончательных. */
sealed protected[this] trait AdvInactual extends MItemStatus {
  override def isAdvActual = false
}

/** Трейт для выставления флага isAdvBusyApproved в true. */
sealed protected[this] trait AdvBusyApproved extends MItemStatus {
  override def isAdvBusyApproved = true
}

