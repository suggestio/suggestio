package io.suggest.common.tags.search

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:30
  * Description: Инфа по одному аггрегированному тегу.
  */

object MTagFound {

  implicit def tagFoundJson: OFormat[MTagFound] = (
    (__ \ "f").format[String] and
    (__ \ "c").format[Int] and
    (__ \ "i").formatNullable[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MTagFound] = UnivEq.derive

}

case class MTagFound(
  face    : String,
  count   : Int,
  nodeId  : Option[String] = None
)



object MTagsFound {

  implicit def tagsFoundJson: OFormat[MTagsFound] = {
    (__ \ "t")
      .format[List[MTagFound]]
      .inmap[MTagsFound]( apply, _.tags )
  }

  @inline implicit def univEq: UnivEq[MTagsFound] = UnivEq.derive

}

/** Контейнер результата аггрегации тегов. */
case class MTagsFound(
                       tags  : List[MTagFound],
                     )
