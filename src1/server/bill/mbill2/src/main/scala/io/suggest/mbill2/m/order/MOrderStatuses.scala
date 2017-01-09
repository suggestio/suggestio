package io.suggest.mbill2.m.order

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:53
 * Description: Статусы ордеров.
 */
object MOrderStatuses extends EnumMaybeWithName with EnumApply {

  protected[this] class Val(val strId: String)
    extends super.Val(strId)
    with ValT
  {

    /**
     * Можно ли изменять заказ?
     * @return true только на ранних этапах.
     */
    def userCanChangeItems: Boolean = false

  }

  override type T = Val


  /** Черновик заказа, корзина. В т.ч. ожидание платежа. Пока заказ не оплачен, он черновик. */
  val Draft = new Val("a") {
    override def userCanChangeItems = true
  }

  /** Оплата заказа проведена. Дальше всё происходит на уровне item'ов заказа. */
  val Closed = new Val("d")

  /** Статус ордера-корзины суперюзера, куда от суперюзеров сваливаются всякие мгновенные бесплатные "покупки".
    * Т.е. с использованием галочки "размещать бесплатно без подтверждения".
    * Был сделан из-за проблем при использовании обычного ордера-корзины для этого:
    * в корзину попадали online-item'ы, оттуда же их можно было и удалить случайно. */
  val SuTrash = new Val("s")


  /**
    * Для бесплатного размещения суперюзерами используется особый ордер-корзина,
    * который не маячит под глазами и item'ы которого можно легко обнаружить и почистить,
    * т.к. они бесплатные.
    *
    * @param isSuFree true если суперюзер требует бесплатное размещение.
    *                 false в остальных случаях.
    * @return Draft либо SuTrash.
    */
  def cartStatusForAdvSuperUser(isSuFree: Boolean): T = {
    if (isSuFree) SuTrash else Draft
  }

}
