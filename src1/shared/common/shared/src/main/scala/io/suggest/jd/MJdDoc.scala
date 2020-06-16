package io.suggest.jd

import diode.FastEq
import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.tags.JdTag
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

  implicit object MJdDocFastEq extends FastEq[MJdDoc] {
    override def eqv(a: MJdDoc, b: MJdDoc): Boolean = {
      (a.template ===* b.template) &&
      // Инстанс JdID собирается динамически при рендере JdCss и JdR, поэтому его следует сравнивать изнутри.
      ((a.tagId ===* b.tagId) || MJdTagId.MJdTagIdFastEq.eqv(a.tagId, b.tagId))
    }
  }

  implicit def jdDocJson: OFormat[MJdDoc] = (
    (__ \ "t").format[Tree[JdTag]] and
    (__ \ "i").formatNullable[MJdTagId]
      .inmap[MJdTagId](
        EmptyUtil.opt2ImplMEmptyF( MJdTagId ),
        EmptyUtil.implEmpty2OptF,
      )
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MJdDoc] = UnivEq.derive

  def template  = GenLens[MJdDoc](_.template)
  def jdId      = GenLens[MJdDoc](_.tagId)

}


/** @param template Шаблон документа.
  * @param tagId id документа внутри MJdTagId, если есть.
  */
final case class MJdDoc(
                         template     : Tree[JdTag],
                         tagId        : MJdTagId,
                       )
