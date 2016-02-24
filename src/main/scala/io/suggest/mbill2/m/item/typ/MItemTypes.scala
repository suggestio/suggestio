package io.suggest.mbill2.m.item.typ

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}
import io.suggest.mbill2.m.item.status.{MItemStatuses, MItemStatus}

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
  {
    /** Новый статус item'а после оплаты заказа. */
    def orderClosedStatus   : MItemStatus = MItemStatuses.AwaitingSioAuto

    /** Какой статус выставлять item'у после получения оплаты? */
    def sioApprovedStatus   : MItemStatus = MItemStatuses.Offline

    /** Название по каталогу локализованных названий. */
    def nameI18n: String = {
      "Item.type." + strId
    }
  }

  override type T = Val

  /** Прямое размещение карточки на каком-то узле.
    * Это было самый первый тип размещения в suggest.io. */
  val AdvDirect         : T = new Val("a")

  /** Заказ геотеггинга для карточки. */
  val GeoTag            : T = new Val("t")

  /** Покупка доступа к внешнему размещению. */
  val AdvExt            : T = new Val("e")

}



