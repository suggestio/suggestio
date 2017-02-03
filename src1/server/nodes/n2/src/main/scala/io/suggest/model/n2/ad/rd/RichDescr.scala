package io.suggest.model.n2.ad.rd

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, FieldIndexingVariants, FieldString}
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 19:17
 * Description: JSON-модель для хранения RichDescription, т.е. некоего HTML-описания.
 */

object RichDescr extends IGenEsMappingProps {

  val BG_COLOR_ESFN     = "bgColor"
  val TEXT_ESFN         = "text"

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(BG_COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(TEXT_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
    )
  }

  implicit val FORMAT: OFormat[RichDescr] = (
    (__ \ BG_COLOR_ESFN).format[String] and
    (__ \ TEXT_ESFN).format[String]
  )(apply, unlift(unapply))

}



case class RichDescr(
  bgColor : String,
  text    : String
)
