package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js
import js.JSConverters._
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

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MAdContentCtx(
      fields = d(FIELDS_FN)
        .asInstanceOf[js.Array[js.Dynamic]]
        .toSeq
        .map(MAdContentField.fromJson),
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
) extends IToJsonDict {

  def toJson = {
    val d = js.Dictionary.empty[js.Any]
    if (fields.nonEmpty)
      d.update(FIELDS_FN, fields.iterator.map(_.toJson).toJSArray)
    if (title.nonEmpty)
      d.update(TITLE_FN, title.get)
    if (descr.nonEmpty)
      d.update(DESCR_FN, descr.get)
    d
  }
}


import io.suggest.adv.ext.model.ctx.MAdContentField._

object MAdContentField extends FromStringT {
  override type T = MAdContentField
  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[String]] : WrappedDictionary[String]
    MAdContentField(
      text = d(TEXT_FN)
    )
  }
}

case class MAdContentField(text: String) extends IToJsonDict {
  def toJson = js.Dictionary[js.Any](
    TEXT_FN -> text
  )
}
