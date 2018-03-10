package io.suggest.geo

import io.suggest.geo.GeoConstants.Qs
import io.suggest.model.play.qsb.QueryStringBindableImpl
import org.elasticsearch.common.geo.builders.{ShapeBuilder, ShapeBuilders}
import play.api.mvc.QueryStringBindable
import au.id.jazzy.play.geojson.{LngLat, Polygon}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 14:47
  * Description: Типа географический прямоугольник в понятиях ES.
  * Прямоугольник задаётся верхней левой и нижней правой противолежащими вершинами.
  *
  * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-shape.html#_envelope]]
  */
object EnvelopeGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = EnvelopeGs


  /** Поддержка биндинга этой простой фигуры в play router. */
  implicit def envelopeGsQsb(implicit geoPointB: QueryStringBindable[MGeoPoint]): QueryStringBindable[EnvelopeGs] = {
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
        val k = key1F(key)
        _mergeUnbinded1(
          geoPointB.unbind( k(Qs.TOP_LEFT_FN),      value.topLeft ),
          geoPointB.unbind( k(Qs.BOTTOM_RIGHT_FN),  value.bottomRight )
        )
      }
    }
  }

  /** Экспорт в GeoJSON Polygon.
    * Не тестировано, но по идее должно работать. */
  override def toPlayGeoJsonGeom(gs: Shape_t): Polygon[LngLat] = {
    import GeoPoint.toLngLat
    val outer = List(
      toLngLat( gs.topLeft ),
      toLngLat( gs.topLeft.copy(lon = gs.bottomRight.lon) ),
      toLngLat( gs.bottomRight ),
      toLngLat( gs.bottomRight.copy(lat = gs.topLeft.lat) )
    )
    Polygon [LngLat] (
      outer :: Nil
    )
  }

  override def toEsShapeBuilder(gs: Shape_t): ShapeBuilder = {
    ShapeBuilders.newEnvelope(
      GeoPoint.toJtsCoordinate(gs.topLeft),
      GeoPoint.toJtsCoordinate(gs.bottomRight)
    )
  }

}
