package io.suggest.sjs.common.model.loc

import io.suggest.geo.IGeoPoint
import io.suggest.geo.GeoConstants.Qs
import io.suggest.sjs.common.geo.json.{GjGeometry, GjTypes}
import org.scalajs.dom.Coordinates
import io.suggest.geo.MGeoPoint

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.15 9:48
  * Description: js-поддержка модели географической точки.
  *
  * 2016.dec.14: Из-за внедрения boopicke в lk-adv-geo-form-sjs возникла необходимость дедублицировать
  * case class модели геоточки с [common] IGeoPoint.
  * Теперь тут только JS-утиль для MGeoPoint, сама же модель окончательно переехала в [common]
  * и пошарена между клиентом и сервером на уровне реализации.
  */

object MGeoPointJs {

  def apply(domCoords: Coordinates): MGeoPoint = {
    MGeoPoint(
      lat = domCoords.latitude,
      lon = domCoords.longitude
    )
  }

  def fromString(str: String): Option[MGeoPoint] = {
    str.split(Qs.LAT_LON_DELIM_FN) match {
      case Array(latStr, lonStr) =>
        try {
          val gp = MGeoPoint(
            lat = latStr.toDouble,
            lon = lonStr.toDouble
          )
          Some(gp)
        } catch { case ex: Throwable =>
          None
        }

      case other =>
        None
    }
  }

  def toArray(gp: IGeoPoint) = js.Array[Double](gp.lon, gp.lat)

  def toJsArray(gp: IGeoPoint) = toArray(gp).asInstanceOf[ js.Array[js.Any] ]

  def toGjPoint(gp: IGeoPoint) = GjGeometry(
    gtype       = GjTypes.Geom.POINT,
    coordinates = toJsArray(gp)
  )

  def toJsObject(gp: IGeoPoint) = js.Dictionary[Double](
    Qs.LAT_FN -> gp.lat,
    Qs.LON_FN -> gp.lon
  )

}
