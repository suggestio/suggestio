package io.suggest.mbill2.m.balance

import io.suggest.bill.{Amount_t, IMCurrency, MPrice}
import io.suggest.mbill2.m.gid._
import io.suggest.primo.id.OptId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.09.18 18:02
  * Description: Модель балансов, рядов таблицы balance.
  */

object MBalance {

  /** Поддержка play-json. */
  implicit def mBalanceFormat: OFormat[MBalance] = (
    (__ \ "c").format[Gid_t] and
    (__ \ "p").format[MPrice] and
    (__ \ "b").format[Amount_t] and
    (__ \ "l").formatNullable[Amount_t] and
    (__ \ "i").formatNullable[Gid_t]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MBalance] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def price = GenLens[MBalance](_.price)
  def blocked = GenLens[MBalance](_.blocked)

}


/** Экземпляр модели одного баланса-кошелька.
  * blocked-средства живут ВНЕ price. Т.е. при блокировке денег, средства списываются с price.amount
  * и добавляются в blocked.
  * Т.е. средства НЕ живут одновременно в двух полях.
  *
  * @param price Все ДОСТУПНЫЕ к расходованию средства.
  * @param blocked Заблокированный объем средств (средства вне price).
  * @param lowOpt Допустимый овердрафт по доступным средствам. None значит 0.
  */
case class MBalance(
                     contractId        : Gid_t,
                     price             : MPrice,
                     blocked           : Amount_t          = 0L,
                     lowOpt            : Option[Amount_t]  = None,
                     override val id   : Option[Gid_t]     = None
                   )
  extends OptId[Gid_t]
  with IMCurrency
{

  def low: Amount_t = lowOpt.getOrElse( 0L )

  def withPrice(price2: MPrice) = copy(price = price2)

  override def currency = price.currency

  def blockedPrice: MPrice = {
    price.withAmount( blocked )
  }

  def allPrice: MPrice = {
    price.plusAmount( blocked )
  }

  def withPriceBlocked(price2: MPrice, blocked2: Amount_t = blocked) = copy(
    price   = price2,
    blocked = blocked2
  )

  def plusAmount(amount: Amount_t) = withPriceBlocked(
    price2 = price.plusAmount(amount)
  )

  def unblockAmount(amount2: Amount_t) = withPriceBlocked(
    price2   = price.plusAmount(amount2),
    blocked2 = blocked - amount2
  )

  def blockAmount(amount2: Amount_t) = unblockAmount(-amount2)

  //override def toString: String = {
  //  s"${getClass.getSimpleName}(#${id.orNull},c$contractId,$price+$blocked${lowOpt.fold("")(",low=" + _)})"
  //}

  def realBlocked: Double =
    MPrice.amountToReal( blocked, currency )

  def realLow: Double =
    MPrice.amountToReal( low, currency )

}
