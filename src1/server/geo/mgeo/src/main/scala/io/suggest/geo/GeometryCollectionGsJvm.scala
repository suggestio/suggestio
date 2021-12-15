package io.suggest.geo

import au.id.jazzy.play.geojson.{Geometry, GeometryCollection, LngLat}
import org.locationtech.spatial4j.shape.Shape
import scala.jdk.CollectionConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.14 18:17
 * Description: Набор геометрических фигур.
 */
object GeometryCollectionGsJvm extends GsStaticJvm {

  override type Shape_t = GeometryCollectionGs

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    *
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  override def toPlayGeoJsonGeom(gs: Shape_t): Geometry[LngLat] = {
    GeometryCollection(
      gs.geoms
        .iterator
        .map( GeoShapeJvm.toPlayGeoJsonGeom )
        .toSeq
    )
  }

  /** Convert to spatial4j shape. */
  override def toSpatialShape(gs: GeometryCollectionGs): Shape = {
    GeoShapeJvm
      .S4J_CONTEXT
      .getShapeFactory
      .multiShape(
        (for {
          subGs <- gs.geoms.iterator
          subCompanion = GsTypesJvm.jvmCompanionFor( subGs.shapeType )
        } yield {
          subCompanion.toSpatialShape( subGs.asInstanceOf[subCompanion.Shape_t] )
        })
          .toList
          .asJava
      )
  }

}
