package io.suggest.geo

import io.suggest.util.logs.MacroLogsDyn
import au.id.jazzy.play.geojson.{Geometry, LngLat}
import org.elasticsearch.geometry.{Geometry => EsGeometry}
import org.locationtech.spatial4j.context.jts.JtsSpatialContext
import org.locationtech.spatial4j.shape.{Shape => Spatial4jShape}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.14 9:43
  * Description: GeoShape - система задания'ов, которые могут быть различными геометрическими фиругами.
  * В функциях модуля избегаем использования jts из-за возможных проблем с XYZ-координатами в будущем.
  *
  * Эта модель служит цели
  */

object GeoShapeJvm extends MacroLogsDyn {

  def S4J_CONTEXT = JtsSpatialContext.GEO
  def S4J_FACTORY = S4J_CONTEXT.getShapeFactory.getGeometryFactory

  def toPlayGeoJsonGeom(gs: IGeoShape): Geometry[LngLat] = {
    val c = GsTypesJvm.jvmCompanionFor(gs.shapeType)
    // TODO Нужна higher-kinds метод, занимающийся этим без asInstanceOf.
    c.toPlayGeoJsonGeom( gs.asInstanceOf[c.Shape_t] )
  }

  /**
    * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  def toEsShapeBuilder(gs: IGeoShapeQuerable): EsGeometry = {
    val c = GsTypesJvm.jvmCompanionFor( gs.shapeType )
      .asInstanceOf[GsStaticJvmQuerable]
    c.toEsShapeBuilder( gs.asInstanceOf[c.Shape_t] )
  }


  def toSpatialShape(geoShape: IGeoShape): Spatial4jShape = {
    val gsCompanion = GsTypesJvm
      .jvmCompanionFor( geoShape.shapeType )
    gsCompanion.toSpatialShape( geoShape.asInstanceOf[gsCompanion.Shape_t] )
  }

}



/** Интерфейс для объекта-компаньона на стороне JVM. */
trait GsStaticJvm {

  type Shape_t <: IGeoShape

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  def toPlayGeoJsonGeom(gs: Shape_t): Geometry[LngLat]

  /** Convert to spatial4j shape. */
  def toSpatialShape(gs: Shape_t): Spatial4jShape

}


/** Статическая поддержка querable-шейпов, пригодных для сборки search query запроса в ES.
  * Примитивные гео-шейпы являются пригодными для es query, но вот [[GeometryCollectionGs]] -- нет.  */
trait GsStaticJvmQuerable extends GsStaticJvm {

  override type Shape_t <: IGeoShapeQuerable

  /**
    * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  def toEsShapeBuilder(gs: Shape_t): EsGeometry

}
