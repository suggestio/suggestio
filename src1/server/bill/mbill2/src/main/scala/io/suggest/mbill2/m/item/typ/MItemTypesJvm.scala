package io.suggest.mbill2.m.item.typ

import io.suggest.enum2.EnumeratumJvmUtil
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.15 12:50
  * Description: JVM поддержки статической модели типов item'ов заказа.
  * Сама модель уехала в common в одноимённый класс (без суффикса Jvm).
  */
object MItemTypesJvm {

  /** Поддержка маппинга для play router. */
  implicit def mItemTypeQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MItemType] = {
    EnumeratumJvmUtil.valueEnumQsb( MItemTypes )
  }

}



