package io.suggest.geo

import io.suggest.geo.GeoConstants.Qs
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable
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
      .format[Seq[MGeoPoint]]
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


  /** Поддержка биндинга этой простой фигуры в play router. */
  implicit def qsb(implicit geoPointB: QueryStringBindable[MGeoPoint]): QueryStringBindable[EnvelopeGs] = {
    new QueryStringBindableImpl[EnvelopeGs] {

      override def KEY_DELIM = Qs.DELIM

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, EnvelopeGs]] = {
        val k = key1F(key)
        for {
          topLeftEith     <- geoPointB.bind ( k(Qs.TOP_LEFT_FN),      params )
          bottomRightEith <- geoPointB.bind ( k(Qs.BOTTOM_RIGHT_FN),  params )
        } yield {
          for {
            topLeft       <- topLeftEith.right
            bottomRight   <- bottomRightEith.right
          } yield {
            EnvelopeGs(
              topLeft     = topLeft,
              bottomRight = bottomRight
            )
          }
        }
      }

      override def unbind(key: String, value: EnvelopeGs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            geoPointB.unbind( k(Qs.TOP_LEFT_FN),      value.topLeft ),
            geoPointB.unbind( k(Qs.BOTTOM_RIGHT_FN),  value.bottomRight )
          )
        }
      }
    }
  }

}

case class EnvelopeGs(
  topLeft: MGeoPoint,
  bottomRight: MGeoPoint
)
  extends GeoShapeQuerable {

  override def shapeType = GsTypes.envelope

  override def toEsShapeBuilder: ShapeBuilder = {
    ShapeBuilder.newEnvelope()
      .topLeft( GeoPoint.toJstCoordinate(topLeft) )
      .bottomRight( GeoPoint.toJstCoordinate(bottomRight) )
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    EnvelopeGs.DATA_FORMAT
      .writes(this)
      .fields
      .toList
  }

  override def firstPoint: MGeoPoint = topLeft

  /** Экспорт в GeoJSON Polygon.
    * Не тестировано, но по идее должно работать. */
  override def toPlayGeoJsonGeom: Geometry[LatLng] = {
    import GeoPoint.toLatLng
    val outer = List(
      toLatLng( topLeft ),
      toLatLng( topLeft.copy(lon = bottomRight.lon) ),
      toLatLng( bottomRight ),
      toLatLng( bottomRight.copy(lat = topLeft.lat) )
    )
    Polygon [LatLng] (
      outer :: Nil
    )
  }

  override def centerPoint: Some[MGeoPoint] = {
    // TODO Код не тестирован и не использовался с момента запиливания
    // Тут чисто-арифметическое определение центра, без [возможных] поправок на форму геойда и прочее.
    val c = MGeoPoint(
      lat = (bottomRight.lat + topLeft.lat) / 2,
      lon = (bottomRight.lon + topLeft.lon) / 2
    )
    Some(c)
  }

}
