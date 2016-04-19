package io.suggest.model.geo

import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{Geometry, LatLng, Polygon}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 14:47
  * Description: Типа географический прямоугольник в понятиях ES.
  * Прямоугольник задаётся верхней левой и нижней правой противолежащими вершинами.
  *
  * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-shape.html#_envelope]]
  */
object EnvelopeGs extends GsStatic {

  override type Shape_t = EnvelopeGs

  override val DATA_FORMAT: OFormat[EnvelopeGs] = {
    (__ \ GeoShape.COORDS_ESFN)
      .format[Seq[GeoPoint]]
      .inmap[EnvelopeGs] (
        { case Seq(c1, c3) =>
            EnvelopeGs(c1, c3)
          case other =>
            throw new IllegalArgumentException("Invalid envelope coords count: " + other)
        },
        {egs =>
          Seq(egs.topLeft, egs.bottomRight)
        }
      )
  }

}

case class EnvelopeGs(topLeft: GeoPoint, bottomRight: GeoPoint) extends GeoShapeQuerable {

  override def shapeType = GsTypes.envelope

  override def toEsShapeBuilder: ShapeBuilder = {
    ShapeBuilder.newEnvelope()
      .topLeft( topLeft.toJstCoordinate )
      .bottomRight( bottomRight.toJstCoordinate )
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    EnvelopeGs.DATA_FORMAT
      .writes(this)
      .fields
      .toList
  }

  override def firstPoint: GeoPoint = topLeft

  /** Экспорт в GeoJSON Polygon.
    * Не тестировано, но по идее должно работать. */
  override def toPlayGeoJsonGeom: Geometry[LatLng] = {
    val outer = List(
      topLeft.toLatLng,
      topLeft.copy(lon = bottomRight.lon).toLatLng,
      bottomRight.toLatLng,
      bottomRight.copy(lat = topLeft.lat).toLatLng
    )
    Polygon [LatLng] (
      List( outer )
    )
  }

}
