package io.suggest.ad.blk.ent

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 15:42
 * Description: Text Entity -- модель плавающего текстового поля рекламной карточки.
 */


object TextEnt {

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[TextEnt] = (
    (__ \ ValueEnt.VALUE_ESFN).format[String] and
    (__ \ ValueEnt.FONT_ESFN).format[EntFont]
  )(apply, unlift(unapply))

}


case class TextEnt(
  value   : String,
  font    : EntFont
)
  extends ValueEnt
