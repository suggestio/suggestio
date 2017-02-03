package models.msc.resp

import io.suggest.geo.MGeoPoint
import io.suggest.geo.GeoPoint.Implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.ScConstants.Resp._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 19:00
  * Description: JSON-модель sc-resp-экшена страницы индекса.
  */
object MScRespIndex {

  /** Поддержка сериализации в JSON этой write-only модели. */
  implicit val WRITES: OWrites[MScRespIndex] = (
    (__ \ HTML_FN).write[String] and
    (__ \ ADN_ID_FN).writeNullable[String] and
    (__ \ TITLE_FN).writeNullable[String] and
    (__ \ GEO_POINT_FN).writeNullable[MGeoPoint]
  )(unlift(unapply))

}


case class MScRespIndex(
  indexHtml       : String,
  currAdnId       : Option[String],
  title           : Option[String]    = None,
  geoPoint        : Option[MGeoPoint]  = None
)
