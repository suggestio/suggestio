package io.suggest.mbill2.m.geo.shape

import io.suggest.common.slick.driver.IDriver
import io.suggest.model.geo.GeoShape
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:32
  * Description: Поддержка поля, содержащего строковой геошейп.
  */
object GeoShapeOptSlick {

  def applyOpt(gsStrOpt: Option[String]): Option[GeoShape] = {
    for (gsStr <- gsStrOpt) yield {
      Json.parse(gsStr)
          .as[GeoShape]
    }
  }

  def unapplyOpt(gsOpt: Option[GeoShape]): Option[Option[String]] = {
    val jsonOpt = for (gs <- gsOpt) yield {
      Json.toJson(gs).toString()
    }
    Some(jsonOpt)
  }

}

trait GeoShapeOptSlick extends IDriver {

  import driver.api._

  def GEO_SHAPE_FN = "geo_shape"

  trait GeoShapeStrOptColumn { that: Table[_] =>
    def geoShapeStrOpt = column[Option[String]](GEO_SHAPE_FN, O.Length(65535))
  }

  trait GeoShapeOptColumn extends GeoShapeStrOptColumn { that: Table[_] =>
    import GeoShapeOptSlick._
    def geoShapeOpt = geoShapeStrOpt <> (applyOpt, unapplyOpt)
  }

}
