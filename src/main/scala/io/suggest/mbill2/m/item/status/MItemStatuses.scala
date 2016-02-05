package io.suggest.mbill2.m.item.status

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:55
 * Description: Статусы обработки item'а.
 */
object MItemStatuses extends EnumMaybeWithName with EnumApply {

  protected class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val

  /** Item лежит в корзине, т.е. в черновике заказа. */
  val Draft               : T = new Val("a")

  /** Item оплачен. Ожидается какая-то автоматическая реакция suggest.io.
    * Например, юзер оплатил размещение карточки. Sio должен разместить карточку и обновить статус. */
  val AwaitingSioAuto     : T = new Val("b")

}



