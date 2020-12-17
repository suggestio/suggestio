package io.suggest.common.tags.edit

import io.suggest.common.empty.EmptyUtil
import io.suggest.i18n.MMessage
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2020 16:58
  * Description: Состояние строки поиска тегов.
  */
object MTagsEditQueryProps {

  @inline implicit def univEq: UnivEq[MTagsEditQueryProps] = UnivEq.derive

  implicit def tagsEditQueryPropsJson: OFormat[MTagsEditQueryProps] = (
    (__ \ "t").format[String] and
    (__ \ "e").formatNullable[Seq[MMessage]]
      .inmap[Seq[MMessage]]( EmptyUtil.opt2ImplEmptyF(Nil), x => Option.when(x.nonEmpty)(x) )
  )(apply, unlift(unapply))

  val text = GenLens[MTagsEditQueryProps]( _.text )
  def errors = GenLens[MTagsEditQueryProps]( _.errors )

}


/** Состояние поиска. */
case class MTagsEditQueryProps(
                                text    : String = "",
                                errors  : Seq[MMessage] = Nil,
                              )
