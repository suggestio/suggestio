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

  val FIELDS_FN       = "fields"
  val TITLE_FN        = "title"
  val DESCR_FN        = "descr"

  /** Делаем из инстанса рекламной карточки класс, пригодный для js-контекста. */
  def fromAd(mad: MAdT): MAdContentCtx = {
    MAdContentCtx(
      fields = mad.offers
        .iterator
        .flatMap { offer => offer.text1.iterator ++ offer.text2.iterator }
        .map { field => MAdContentField(
          text = FormUtil.strTrimSanitizeF(field.value)
        )}
        .toSeq,
      title = None,
      descr = None
    )
  }

  /** mapping */
  implicit def reads: Reads[MAdContentCtx] = (
    (__ \ FIELDS_FN).readNullable[Seq[MAdContentField]].map(_ getOrElse Seq.empty) and
    (__ \ TITLE_FN).readNullable[String] and
    (__ \ DESCR_FN).readNullable[String]
  )(apply _)

  /** unmapping */
  implicit def writes: Writes[MAdContentCtx] = (
    (__ \ FIELDS_FN).write[Seq[MAdContentField]] and
    (__ \ TITLE_FN).writeNullable[String] and
    (__ \ DESCR_FN).writeNullable[String]
  )(unlift(unapply))

}

/** Сырой контент отной рекламной карточки. */
case class MAdContentCtx(fields: Seq[MAdContentField], title: Option[String], descr: Option[String])



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

