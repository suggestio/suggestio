package io.suggest.model.n2.ad.ent.text

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT

/** Список допустимых значений для выравнивания текста. */

object TextAligns extends EnumMaybeWithName with EnumJsonReadsValT {

  protected abstract class Val(val strId: String)
    extends super.Val(strId)
  {
    def cssName: String
  }

  override type T = Val

  val Left: T = new Val("l") {
    override def cssName = "left"
  }

  val Right: T = new Val("r") {
    override def cssName = "right"
  }

  val Center: T = new Val("c") {
    override def cssName = "center"
  }

  val Justify: T = new Val("j") {
    override def cssName = "justify"
  }


  def maybeWithCssName(cssName: String): Option[TextAlign] = {
    values
      .find(_.cssName equalsIgnoreCase cssName)
      .asInstanceOf[Option[TextAlign]]
  }

}
