package io.suggest.model.n2.ad.ent.text

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.Coords2d
import io.suggest.util.SioEsUtil.{DocField, FieldIndexingVariants, FieldString}
import ValueEnt._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 15:42
 * Description: Text Entity -- модель плавающего текстового поля рекламной карточки.
 */

object TextEnt extends IGenEsMappingProps {

  override def generateMappingProps: List[DocField] = {
    val fstr = FieldString(
      id              = VALUE_ESFN,
      index           = FieldIndexingVariants.no,
      include_in_all  = true,
      boost           = Some(1.1F)
    )
    fstr :: ValueEnt.generateMappingProps
  }


  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[TextEnt] = (
    (__ \ ValueEnt.VALUE_ESFN).format[String] and
    (__ \ ValueEnt.FONT_ESFN).format[EntFont] and
    (__ \ ValueEnt.COORDS_ESFN).formatNullable[Coords2d]
  )(apply, unlift(unapply))

}


case class TextEnt(
  value   : String,
  font    : EntFont,
  coords  : Option[Coords2d] = None
)
  extends ValueEnt
