package io.suggest.model.n2.ad.rd

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 19:17
 * Description: JSON-модель для хранения RichDescription, т.е. некоего HTML-описания.
 */

object RichDescr {

  val BG_COLOR_ESFN     = "bgColor"
  val TEXT_ESFN         = "text"

  implicit val FORMAT: OFormat[RichDescr] = (
    (__ \ BG_COLOR_ESFN).format[String] and
    (__ \ TEXT_ESFN).format[String]
  )(apply, unlift(unapply))

}



case class RichDescr(
  bgColor : String,
  text    : String
)
