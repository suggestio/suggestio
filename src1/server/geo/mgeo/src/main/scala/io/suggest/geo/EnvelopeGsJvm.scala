package io.suggest.geo

import io.suggest.geo.GeoConstants.Qs
import play.api.mvc.QueryStringBindable
import au.id.jazzy.play.geojson.{LngLat, Polygon}
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._
import org.elasticsearch.geometry.{Geometry, Rectangle}
import org.locationtech.jts.geom.{Envelope => JtsEnvelope}
import org.locationtech.spatial4j.shape.Shape

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
    new AbstractQueryStringBindable[EnvelopeGs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, EnvelopeGs]] = {
        val k = key1F(key)
        for {
          topLeftEith     <- geoPointB.bind ( k(Qs.TOP_LEFT_FN),      params )
          bottomRightEith <- geoPointB.bind ( k(Qs.BOTTOM_RIGHT_FN),  params )
        } yield {
          for {
            topLeft       <- topLeftEith
            bottomRight   <- bottomRightEith
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
    val outer = (
      toLngLat( gs.topLeft ) ::
      toLngLat( (MGeoPoint.lon replace gs.bottomRight.lon)(gs.topLeft) ) ::
      toLngLat( gs.bottomRight ) ::
      toLngLat( (MGeoPoint.lat replace gs.topLeft.lat)(gs.bottomRight) ) ::
      Nil
    )
    Polygon [LngLat] (
      outer :: Nil
    )
  }

  override def toEsShapeBuilder(gs: Shape_t): Geometry = {
    new Rectangle(
      gs.topLeft.lon.doubleValue,
      gs.topLeft.lat.doubleValue,
      gs.bottomRight.lon.doubleValue,
      gs.bottomRight.lat.doubleValue,
    )
  }

  def toJtsEnvelope(gs: EnvelopeGs): JtsEnvelope = {
    new JtsEnvelope(
      GeoPoint.toJts( gs.topLeft ),
      GeoPoint.toJts( gs.bottomRight ),
    )
  }

  override def toSpatialShape(gs: EnvelopeGs): Shape = {
    GeoShapeJvm.S4J_CONTEXT
      .getShapeFactory
      .rect(
        gs.topLeft.lon.doubleValue,
        gs.bottomRight.lon.doubleValue,
        gs.bottomRight.lat.doubleValue,
        gs.topLeft.lat.doubleValue,
      )
  }

}
