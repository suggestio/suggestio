package io.suggest.mbill2.m.item.typ

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:50
 * Description: Статическая модель типов item'ов заказа.
 */
object MItemTypes extends EnumMaybeWithName with EnumApply {

  /** Класс элементов модели. */
  protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val

  /** Прямое размещение карточки на каком-то узле.
    * Это было самый первый тип размещения в suggest.io. */
  val AdvDirect: T = new Val("a")

}



