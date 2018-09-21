package io.suggest.bill.cart.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.bill.cart.m.{CartDeleteBtnClick, CartSelectItem, MBillData}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:12
  * Description: Контроллер для корзинных экшенов.
  */
class CartAh[M](
                 modelRW: ModelRW[M, MBillData]
               )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по галочке item'а.
    case m: CartSelectItem =>
      ???

    // Нажата кнопка удаления.
    case CartDeleteBtnClick =>
      ???

  }

}
