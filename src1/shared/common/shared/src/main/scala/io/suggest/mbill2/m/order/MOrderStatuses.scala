package io.suggest.mbill2.m.order

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:53
 * Description: Статусы ордеров.
 */
object MOrderStatuses extends StringEnum[MOrderStatus] {

  /** Черновик заказа, корзина. В т.ч. ожидание платежа. Пока заказ не оплачен, он черновик. */
  case object Draft extends MOrderStatus("a")


  /** Неоплаченный, но оплачиваемый прямо сейчас заказ, недоступный для редактирования.
    *
    * Например, Яндекс.касса прислывает check() непосредственно перед списанием средств,
    * а payment-уведомление при наступлении платежа или ошибки платежа.
    * Статус Hold живёт между этими двумя уведомлениями.
    *
    */
  case object Hold extends MOrderStatus("h")

  /** Paid, but not closed status means, that something should be done with order after all items will be processed.
    * For example, current opened deal on paysystem-side should be completed with final payout to seller. */
  case object Paid extends MOrderStatus("p")

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
    values
      .iterator
      .filter(_.canGoToPaySys)
  }


  /** Order status after successful payment processing.
    *
    * @param orderHasPaySystemDeal Is order have transaction with paysystem-level deal?
    *                              See Bill2Util.orderDealTransactionId()
    * @return Next order status after payment received.
    */
  def afterPaymentStatus(orderHasPaySystemDeal: Boolean): MOrderStatus = {
    if (orderHasPaySystemDeal) Paid else Closed
  }

}


sealed abstract class MOrderStatus(override val value: String) extends StringEnumEntry

object MOrderStatus {

  @inline implicit def univEq: UnivEq[MOrderStatus] = UnivEq.derive

  def unapplyStrId(mos: MOrderStatus): Option[String] = {
    Some( mos.value )
  }

  /** Поддержка play-json. */
  implicit def mOrderStatusFormat: Format[MOrderStatus] = {
    EnumeratumUtil.valueEnumEntryFormat( MOrderStatuses )
  }


  implicit final class OrderExt( private val orderStatus: MOrderStatus ) extends AnyVal {

    /** Is order editable?
      * @return true on Draft status.
      */
    def userCanChangeItems: Boolean =
      orderStatus ==* MOrderStatuses.Draft

    /** i18n message code about order status (singular form). */
    def singular: String = "Order.status." + orderStatus.value


    /** Is current user (order owner) can go to payment system?
      *
      * @return true - Yes, order status is payable.
      *         false - Order is already paid or not payable at all.
      */
    def canGoToPaySys: Boolean = {
      orderStatus match {
        case MOrderStatuses.Hold | MOrderStatuses.Draft => true
        case _ => false
      }
    }

  }

}
