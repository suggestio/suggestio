package models.blk

import io.suggest.model.{EnumMaybeWithName, EnumValue2Val}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 10:51
 * Description: MAd.colors содержит карту цветов. Здесь перечислены ключи этой карты цветов.
 */
object AdColorFns extends Enumeration with EnumValue2Val with EnumMaybeWithName {

  protected abstract class Val(val name: String) extends super.Val(name) {
    def default: String
  }

  type AdColorFn = Val
  override type T = AdColorFn

  /** Цвет фона под картинкой. */
  val IMG_BG_COLOR_FN: AdColorFn = new Val("ibgc") {
    override def default = "EEEEEE"
  }

  /** Цвет покрывающего картинку паттерна. */
  val WIDE_IMG_PATTERN_COLOR_FN: AdColorFn = new Val("iwp") {
    override def default = "000000"
  }

}
