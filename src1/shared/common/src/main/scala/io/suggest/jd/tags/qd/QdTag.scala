package io.suggest.jd.tags.qd

import io.suggest.jd.tags.{IDocTag, MJdTagNames}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:20
  * Description: Класс, описывающий quill delta в терминах s.io.
  */

object QdTag {

  implicit val QD_TAG_FORMAT: OFormat[QdTag] = (
    (__ \ "h").formatNullable[String] and
    (__ \ "o").format[Seq[MQdOp]]
  )(apply, unlift(unapply))


  def a(html: Option[String] = None)(ops: MQdOp*) = apply(html, ops)

}


/** Класс модели представления цельной quill-дельты.
  *
  * @param html html-текст, отрендеренный редактором.
  * @param ops Delta-операции.
  */
case class QdTag(
                  html  : Option[String],
                  ops   : Seq[MQdOp]
                )
  extends IDocTag
{

  override def jdTagName = MJdTagNames.QUILL_DELTA

  override def children = Nil

  override def deepEdgesUidsIter = {
    ops
      .iterator
      .flatMap(_.edgeInfo)
      .map(_.edgeUid)
  }

  def withOps(ops: Seq[MQdOp]) = copy(ops = ops)
  def withHtml(html: Option[String]) = copy(html = html)

}
