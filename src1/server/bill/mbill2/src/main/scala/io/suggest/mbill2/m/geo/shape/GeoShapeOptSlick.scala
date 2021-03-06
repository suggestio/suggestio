package io.suggest.mbill2.m.geo.shape

import io.suggest.slick.profile.IProfile
import io.suggest.geo.IGeoShape
import io.suggest.primo.IApplyOpt1
import play.api.libs.json.Json
import io.suggest.geo.IGeoShape.JsonFormats.allStoragesEsFormat

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:32
  * Description: Поддержка поля, содержащего строковой геошейп.
  */
object GeoShapeOptSlick extends IApplyOpt1 {

  override type ApplyArg_t = String
  override type T = IGeoShape

  override def apply(gsStr: String): IGeoShape = {
    Json
      .parse(gsStr)
      .as[IGeoShape]
  }

  def unapplyOpt(gsOpt: Option[IGeoShape]): Option[Option[String]] = {
    val jsonOpt = for (gs <- gsOpt) yield {
      Json.toJson(gs).toString()
    }
    Some(jsonOpt)
  }

}

trait GeoShapeOptSlick extends IProfile {

  import profile.api._

  def GEO_SHAPE_FN = "geo_shape"

  trait GeoShapeStrOptColumn { that: Table[_] =>
    def geoShapeStrOpt = column[Option[String]](GEO_SHAPE_FN, O.Length(65535))
  }

  trait GeoShapeOptColumn extends GeoShapeStrOptColumn { that: Table[_] =>
    import GeoShapeOptSlick._
    def geoShapeOpt = geoShapeStrOpt <> (applyOpt, unapplyOpt)
  }

}
