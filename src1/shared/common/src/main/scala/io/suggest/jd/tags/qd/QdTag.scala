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

  implicit val QD_TAG_FORMAT: OFormat[QdTag] = {
    (__ \ "o").format[Seq[MQdOp]]
      .inmap[QdTag](apply, _.ops)
  }

  def a()(ops: MQdOp*) = apply(ops)

}


case class QdTag(
                  ops: Seq[MQdOp]
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

}
