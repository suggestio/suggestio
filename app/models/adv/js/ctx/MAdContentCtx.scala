package models.adv.js.ctx

import _root_.util.FormUtil
import models.MAdT
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 11:30
 * Description: 
 */

object MAdContentCtx {

  val FIELDS_FN = "fields"

  /** Делаем из инстанса рекламной карточки класс, пригодный для js-контекста. */
  def apply(mad: MAdT): MAdContentCtx = {
    MAdContentCtx(
      fields = mad.offers
        .iterator
        .flatMap { offer => offer.text1.iterator ++ offer.text2.iterator }
        .map { field => MAdContentField(
          text = FormUtil.strTrimSanitizeF(field.value)
        )}
        .toSeq
    )
  }

  /** mapping */
  implicit def reads: Reads[MAdContentCtx] = {
    (__ \ FIELDS_FN)
      .readNullable[Seq[MAdContentField]]
      .map { fs => MAdContentCtx(fs getOrElse Seq.empty) }
  }

  /** unmapping */
  implicit def writes: Writes[MAdContentCtx] = {
    (__ \ FIELDS_FN)
      .writeNullable[Seq[MAdContentField]]
      .contramap { ctx => if (ctx.fields.isEmpty) None else Some(ctx.fields) }
  }

}

/** Сырой контент отной рекламной карточки. */
case class MAdContentCtx(fields: Seq[MAdContentField])



object MAdContentField {
  val TEXT_FN = "text"

  implicit def reads: Reads[MAdContentField] = {
    (__ \ TEXT_FN)
      .read[String]
      .map(apply)
  }

  implicit def writes: Writes[MAdContentField] = {
    (__ \ TEXT_FN)
      .write[String]
      .contramap(_.text)
  }

}

case class MAdContentField(text: String)

