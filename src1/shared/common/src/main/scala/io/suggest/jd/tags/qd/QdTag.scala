package io.suggest.jd.tags.qd

import io.suggest.common.html.HtmlConstants
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.{IDocTag, MJdTagNames}
import io.suggest.model.n2.edge.EdgeUid_t
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:20
  * Description: Класс, описывающий quill delta в терминах s.io.
  */

object QdTag {

  implicit val QD_TAG_FORMAT: OFormat[QdTag] = {
    (__ \ "o").format[Seq[MQdOp]]
      .inmap(apply, _.ops)
  }


  def a()(ops: MQdOp*) = apply(ops)

}


/** Класс модели представления цельной quill-дельты.
  *
  * @param ops Delta-операции.
  */
case class QdTag(
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


  override def shrink: Seq[IDocTag] = {
    if (ops.isEmpty) {
      // TODO По идее, этот код никогда не вызывается, т.к. quill никогда не возвращает пустой ops[].
      Nil
    } else {
      super.shrink
    }
  }

  /** Является ли дельта пустой? */
  def isEmpty(edgesMap: Map[EdgeUid_t, MJdEditEdge]): Boolean = {
    // Обычно, пустая дельта выглядит так: {"ops":[{"insert":"\n"}]}
    val l = ops.length
    l == 0 || (l == 1 && {
      ops.head
        .edgeInfo
        .flatMap { ei =>
          edgesMap.get(ei.edgeUid)
        }
        .flatMap(_.text)
        .contains( HtmlConstants.NEWLINE_UNIX.toString )
    })
  }

}
