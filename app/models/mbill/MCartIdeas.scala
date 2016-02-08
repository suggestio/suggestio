package models.mbill

import io.suggest.mbill2.m.order.IOrderWithItems
import io.suggest.mbill2.m.balance.{MBalance => MBalance2}
import models.MPrice

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 22:59
  * Description: Статическая модель возможных "идей" по обработке товарной корзины.
  */
object MCartIdeas {

  /** Интерфейс каждого инстанса модели. */
  sealed trait Idea

  /** Ничего делать не требуется. */
  case object NothingToDo extends Idea

  /** Ордер был проведён, всё оплачено или бесплатно.  */
  final case class OrderClosed(cart: IOrderWithItems, newBalances: Seq[MBalance2]) extends Idea

  /** Недостаточно бабла для проведения платежа в корзине. */
  final case class NeedMoney(howMany: Seq[MPrice]) extends Idea

}
