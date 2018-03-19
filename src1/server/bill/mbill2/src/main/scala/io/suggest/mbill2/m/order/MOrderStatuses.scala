package io.suggest.mbill2.m.order

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:53
 * Description: Статусы ордеров.
 */
object MOrderStatuses extends StringEnum[MOrderStatus] {

  /** Черновик заказа, корзина. В т.ч. ожидание платежа. Пока заказ не оплачен, он черновик. */
  case object Draft extends MOrderStatus("a") {
    override def userCanChangeItems = true
    override def canGoToPaySys = true
  }


  /** Неоплаченный, но оплачиваемый прямо сейчас заказ, недоступный для редактирования.
    *
    * Например, Яндекс.касса прислывает check() непосредственно перед списанием средств,
    * а payment-уведомление при наступлении платежа или ошибки платежа.
    * Статус Hold живёт между этими двумя уведомлениями.
    *
    */
  case object Hold extends MOrderStatus("h") {
    /**
      * true, т.к. юзера можно отправить на повторный платеж, если что-то повисло.
      * Платежная система увидит повторяющийся номер заказа и отработает ситуацию самостоятельно.
      */
    override def canGoToPaySys: Boolean = true
  }


  /** Оплата заказа проведена. Дальше всё происходит на уровне item'ов заказа. */
  case object Closed extends MOrderStatus("d")


  /** Статус ордера-корзины суперюзера, куда от суперюзеров сваливаются всякие мгновенные бесплатные "покупки".
    * Т.е. с использованием галочки "размещать бесплатно без подтверждения".
    * Был сделан из-за проблем при использовании обычного ордера-корзины для этого:
    * в корзину попадали online-item'ы, оттуда же их можно было и удалить случайно. */
  case object SuTrash extends MOrderStatus("s")


  override val values = findValues

  /**
    * Для бесплатного размещения суперюзерами используется особый ордер-корзина,
    * который не маячит под глазами и item'ы которого можно легко обнаружить и почистить,
    * т.к. они бесплатные.
    *
    * @param isSuFree true если суперюзер требует бесплатное размещение.
    *                 false в остальных случаях.
    * @return Draft либо SuTrash.
    */
  def cartStatusForAdvSuperUser(isSuFree: Boolean): MOrderStatus = {
    if (isSuFree) SuTrash else Draft
  }


  def canGoToPaySys: Iterator[MOrderStatus] = {
    values.iterator
      .filter(_.canGoToPaySys)
  }

}


sealed abstract class MOrderStatus(override val value: String) extends StringEnumEntry {

  /**
    * Можно ли изменять заказ?
    * @return true только на ранних этапах.
    */
  def userCanChangeItems: Boolean = false

  /**
    * Можно ли отправить юзера в платежную систему для оплаты ордера с таким статусом?
    *
    * @return true - да, ожидаются какие-то деньги.
    *         false ордер уже оплачен или его оплачивать не требуется.
    */
  def canGoToPaySys: Boolean = false

  /** Код i18n-сообщения о статусе в заказа в единственном числе. */
  def singular: String = "Order.status." + value

}

object MOrderStatus {

  implicit def univEq: UnivEq[MOrderStatus] = UnivEq.derive

  def unapplyStrId(mos: MOrderStatus): Option[String] = {
    Some( mos.value )
  }

}
