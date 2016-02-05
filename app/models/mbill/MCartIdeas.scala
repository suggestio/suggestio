package models.mbill

import io.suggest.mbill2.m.order.IOrderWithItems

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

  /** Ордер был проведён.  */
  sealed case class OrderClosed(cart: IOrderWithItems) extends Idea

}
