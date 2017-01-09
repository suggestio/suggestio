package io.suggest.ym.model.ad

import io.suggest.common.menum.{EnumMaybeWithName, EnumValue2Val}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 10:51
 * Description: MAd.colors содержит карту цветов. Здесь перечислены ключи этой карты цветов.
 */

// TODO Эта модель отправляется на свалку истории с приходом N2-архитектуры,
// а карта цветов становится case-class'ом. Эту модель нельзя менять, а потом надо будет удалить.

object AdColorFns extends Enumeration with EnumValue2Val with EnumMaybeWithName {

  protected abstract class Val(val name: String) extends super.Val(name) {
    def default: String
  }

  override type T = Val

  /** Цвет фона под картинкой. */
  val IMG_BG_COLOR_FN: T = new Val("ibgc") {
    override def default = "EEEEEE"
  }

  /** Цвет покрывающего картинку паттерна. */
  val WIDE_IMG_PATTERN_COLOR_FN: T = new Val("iwp") {
    override def default = "000000"
  }

}
