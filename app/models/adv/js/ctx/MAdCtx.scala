package models.adv.js.ctx

import models.MAdT
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 14:27
 * Description: JS-контекст с данными по одной рекламной карточке, отрендеренной картинке и тд.
 */

object MAdCtx {

  val ID_FN       = "_id"
  val PICTURE_FN  = "rendered"
  val CONTENT_FN  = "content"
  val SC_URL_FN   = "scUrl"


  /** json-маппинг сериализованного элемента MAdCtx. */
  implicit def madCtxReads: Reads[MAdCtx] = {
    val p = (__ \ ID_FN).readNullable[String] and
      (__ \ PICTURE_FN).readNullable[MPictureCtx] and
      (__ \ CONTENT_FN).read[MAdContentCtx] and
      (__ \ SC_URL_FN).readNullable[String]
    p(apply _)
  }

  /** json-unmapping для сериализации экземпляров MAdCtx. */
  implicit def madCtxWrites: Writes[MAdCtx] = (
    (__ \ ID_FN).writeNullable[String] and
      (__ \ PICTURE_FN).writeNullable[MPictureCtx] and
      (__ \ CONTENT_FN).write[MAdContentCtx] and
      (__ \ SC_URL_FN).writeNullable[String]
  )(unlift(MAdCtx.unapply))

}


case class MAdCtx(
  id      : Option[String],
  picture : Option[MPictureCtx],
  content : MAdContentCtx,
  scUrl   : Option[String]
)


object MAdContentCtx {
  val FIELDS_FN = "fields"

  implicit def wrapMAd(mad: MAdT): MAdContentCtx = {
    // TODO Написать генератор из mad в контент контекста.
    ???
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

