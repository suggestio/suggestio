package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary
import io.suggest.adv.ext.model.ctx.MAdContentCtx._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 11:31
 * Description: JSON-модель, описывающая содержимое одной рекламной карточки.
 */

object MAdContentCtx extends FromStringT {
  override type T = MAdContentCtx

  override def fromDyn(raw: js.Dynamic): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MAdContentCtx(
      fields = d(FIELDS_FN)
        .asInstanceOf[js.Array[js.Dynamic]]
        .toSeq
        .map(MAdContentField.fromDyn),
      title = d.get(TITLE_FN)
        .map(_.toString),
      descr = d.get(DESCR_FN)
        .map(_.toString)
    )
  }
}


case class MAdContentCtx(
  fields: Seq[MAdContentField],
  title: Option[String],
  descr: Option[String]
) {

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    if (fields.nonEmpty)
      lit.updateDynamic(FIELDS_FN)(fields.map(_.toJson))
    if (title.nonEmpty)
      lit.updateDynamic(TITLE_FN)(title.get)
    if (descr.nonEmpty)
      lit.updateDynamic(DESCR_FN)(descr.get)
    lit
  }
}


import io.suggest.adv.ext.model.ctx.MAdContentField._

object MAdContentField extends FromStringT {
  override type T = MAdContentField
  override def fromDyn(raw: js.Dynamic): T = {
    val d = raw.asInstanceOf[js.Dictionary[String]] : WrappedDictionary[String]
    MAdContentField(
      text = d(TEXT_FN)
    )
  }
}

case class MAdContentField(text: String) {
  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(TEXT_FN)(text)
    lit
  }
}
