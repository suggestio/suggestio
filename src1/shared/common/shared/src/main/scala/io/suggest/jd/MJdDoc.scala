package io.suggest.jd

import diode.FastEq
import io.suggest.jd.tags.JdTag
import io.suggest.primo.id.OptId
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.09.2019 23:29
  * Description: Связка jd-шаблона и идентификатора документа.
  * Появилась для удобной передачи в индексацию MJdTagId.
  */
object MJdDoc {

  implicit object MJdTplFastEq extends FastEq[MJdDoc] {
    override def eqv(a: MJdDoc, b: MJdDoc): Boolean = {
      (a.template ===* b.template) &&
      (a.nodeId ===* b.nodeId)
    }
  }

  implicit def jdDocJson: OFormat[MJdDoc] = (
    (__ \ "t").format[Tree[JdTag]] and
    (__ \ "i").formatNullable[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MJdDoc] = UnivEq.derive

  val template = GenLens[MJdDoc](_.template)
  val nodeId   = GenLens[MJdDoc](_.nodeId)

}


/** @param template Шаблон документа.
  * @param nodeId id документа, если есть.
  */
final case class MJdDoc(
                         template     : Tree[JdTag],
                         nodeId       : Option[String],
                       )
  extends OptId[String]
{
  override def id = nodeId
}
