package io.suggest.common.tags.edit

import io.suggest.common.empty.EmptyUtil
import io.suggest.i18n.MMessage
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._
import scalaz.std.iterable._
import scalaz.std.string._
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 17:37
  * Description: Клиент-серверная модель js-состояния формы редактирования/выставления тегов.
  * Props -- потому что это состояние, которое ближе по смыслу и использованию к react props,
  * а так же сериализуются на сервер.
  */

object MTagsEditProps {

  def validate(tep: MTagsEditProps): ValidationNel[String, MTagsEditProps] = {
    var vld = Validation.liftNel(tep)(_.tagsExists.size > TagsEditConstants.Constraints.TAGS_PER_ADD_MAX, "e.tags.too.many" )

    if (tep.tagsExists.nonEmpty) {
      val x2 = ScalazUtil.validateAll(tep.tagsExists)(TagsEditConstants.Constraints.tagFaceV)
      vld = (vld |@| x2) { (_, _) => tep }
    }

    vld
  }

  @inline implicit def univEq: UnivEq[MTagsEditProps] = UnivEq.derive

  implicit def tagsEditPropsJson: OFormat[MTagsEditProps] = {
    (
      (__ \ "q").format[MTagsEditQueryProps] and
      (__ \ "e").formatNullable[Set[String]]
        .inmap[Set[String]]( EmptyUtil.opt2ImplEmptyF(Set.empty), x => Option.when(x.nonEmpty)(x) )
    )(apply, unlift(unapply))
  }

}


// TODO Надо бы реализовать diode FastEq.
case class MTagsEditProps(
                           query       : MTagsEditQueryProps    = MTagsEditQueryProps(),
                           tagsExists  : Set[String]            = Set.empty
                         ) {

  def withQuery(q: MTagsEditQueryProps) = copy( query = q )
  def withTagsExists(te: Set[String]) = copy( tagsExists = te )

}


object MTagsEditQueryProps {
  @inline implicit def univEq: UnivEq[MTagsEditQueryProps] = UnivEq.derive

  implicit def tagsEditQueryPropsJson: OFormat[MTagsEditQueryProps] = (
    (__ \ "t").format[String] and
    (__ \ "e").formatNullable[Seq[MMessage]]
      .inmap[Seq[MMessage]]( EmptyUtil.opt2ImplEmptyF(Nil), x => Option.when(x.nonEmpty)(x) )
  )(apply, unlift(unapply))

}

/** Состояние поиска. */
case class MTagsEditQueryProps(
  text    : String = "",
  errors  : Seq[MMessage] = Nil,
) {

  def withText(t: String) = copy(text = t)
  def withErrors(errs: Seq[MMessage]) = copy(errors = errs)

}
