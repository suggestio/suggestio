package io.suggest.bill.cart.m

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 11:13
  * Description: Экшены компонента корзины.
  */

sealed trait ICartAction extends DAction


/** Управление состояния галочки для элементов заказа.
  * @param itemId id элемента. Или None, если окучиваем сразу все.
  * @param checked Новое состояние галочки.
  */
case class CartSelectItem(itemId: Option[Gid_t], checked: Boolean) extends ICartAction

/** Клик по кнопке удаления выделенных элементов. */
case object CartDeleteBtnClick extends ICartAction
