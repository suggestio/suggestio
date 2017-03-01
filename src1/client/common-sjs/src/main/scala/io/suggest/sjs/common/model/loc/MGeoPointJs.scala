package io.suggest.sjs.common.model.loc

import io.suggest.geo.{IGeoPoint, Lat, Lon, MGeoPoint}
import io.suggest.geo.GeoConstants.Qs
import io.suggest.sjs.common.geo.json.{GjGeometry, GjTypes}
import org.scalajs.dom.Coordinates

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

  /** Массив координат в стандартной нотации: [lon,lat] т.е. [x,y]. */
  def toArray(gp: IGeoPoint) = js.Array[Double](gp.lon, gp.lat)

  def toJsArray(gp: IGeoPoint) = toArray(gp).asInstanceOf[ js.Array[js.Any] ]

  def fromGjArray(arr: js.Array[Double]): MGeoPoint = {
    MGeoPoint(
      lon = arr(0),
      lat = arr(1)
    )
  }

  /** leaflet использовать массивы в традиционной нотации: [y, x] то бишь [lat, lon]. */
  def toLatLngArray(gp: IGeoPoint) = js.Array[Double](gp.lat, gp.lon)

  def toGjPoint(gp: IGeoPoint): GjGeometry = {
    GjGeometry(
      gtype        = GjTypes.Geom.POINT,
      gcoordinates = toJsArray(gp)
    )
  }

  def toJsObject(gp: IGeoPoint) = js.Dictionary[Double](
    Lat.QS_FN -> gp.lat,
    Lon.QS_FN -> gp.lon
  )

}
