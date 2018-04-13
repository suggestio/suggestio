package io.suggest.adn.edit.m

import io.suggest.jd.MJdEdgeId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 21:55
  * Description: Модель описания картинок adn-узла.
  * Является view-частью для картинок.
  */
object MAdnResView {

  implicit def mAdnImgsFormat: OFormat[MAdnResView] = (
    (__ \ "l").formatNullable[MJdEdgeId] and
    (__ \ "w").formatNullable[MJdEdgeId]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MAdnResView] = UnivEq.derive

}


case class MAdnResView(
                        logo: Option[MJdEdgeId],
                        wcFg: Option[MJdEdgeId]
                      ) {

  def withLogo(logo: Option[MJdEdgeId]) = copy(logo = logo)
  def withWcFg(wcFg: Option[MJdEdgeId]) = copy(wcFg = wcFg)

}
