package io.suggest.jd.tags.qd

import io.suggest.jd.tags.{IBgColorOpt, IDocTag, MJdTagNames}
import io.suggest.model.n2.node.meta.colors.MColorData
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:20
  * Description: Класс, описывающий quill delta в терминах s.io.
  */

object QdTag {

  /** Поддержка play-json. */
  implicit val QD_TAG_FORMAT: OFormat[QdTag] = (
    (__ \ "o").format[Seq[MQdOp]] and
    IBgColorOpt.bgColorOptFormat
  )(apply, unlift(unapply))


  def a( bgColor: Option[MColorData] = None )(ops: MQdOp*): QdTag = {
    apply(ops, bgColor)
  }

}


/** Класс модели представления цельной quill-дельты.
  *
  * @param ops Delta-операции.
  */
case class QdTag(
                  ops                     : Seq[MQdOp],
                  override val bgColor    : Option[MColorData]  = None
                )
  extends IDocTag
  with IBgColorOpt
{

  override type T = QdTag

  override def jdTagName = MJdTagNames.QUILL_DELTA

  override def children = Nil

  override def deepEdgesUidsIter = {
    ops
      .iterator
      .flatMap(_.edgeInfo)
      .map(_.edgeUid)
  }

  def withOps(ops: Seq[MQdOp]) = copy(ops = ops)


  override def shrink: Seq[IDocTag] = {
    if (ops.isEmpty) {
      // TODO По идее, этот код никогда не вызывается, т.к. quill никогда не возвращает пустой ops[].
      Nil
    } else {
      super.shrink
    }
  }

  override def withBgColor(bgColor: Option[MColorData]) = copy(bgColor = bgColor)

}
