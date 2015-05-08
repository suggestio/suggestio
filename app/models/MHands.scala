package models

import io.suggest.model.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 15:55
 * Description: Модель лева и права. Используется там где нужны эти два значения.
 * Называется модель моделью рук, но к самим рукам она отношения не имеет.
 */
object MHands extends Enumeration with EnumMaybeWithName {

  /** Экземпляр модели. */
  protected abstract sealed class Val(val strId: String) extends super.Val(strId) {
    def name: String
    override def toString() = strId
  }

  override type T = Val

  /** Лево. */
  val Left: T = new Val("l") {
    override def name = "left"
  }

  /** Право. */
  val Right: T = new Val("r") {
    override def name = "right"
  }

}
