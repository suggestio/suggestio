package io.suggest.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.17 15:28
  * Description:
  */

/** Базовый трейт для реализаций geoshape. */
trait IGeoShape {

  /** Используемый тип фигуры. */
  def shapeType: GsType

  def firstPoint: MGeoPoint

  /**
    * Центральная точка фигуры.
    * По идее, эта точка всегда существует, но тут Option.
    * None означает, что код поддержки вычисления центральной точки не заимплеменчен.
    */
  def centerPoint: Option[MGeoPoint] = None

  /** Отображаемое для пользователя имя шейпа. */
  def displayTypeName: String = {
    shapeType.geoJsonName
      .getOrElse( getClass.getSimpleName )
  }

}


/** Если элемент можно запрашивать в geo-shape search/filter, то об этом можно уведомить компилятор. */
trait IGeoShapeQuerable extends IGeoShape


// Реализация GeoShape'ов:

/** Гео-шейп точки. */
case class PointGs(coord: MGeoPoint) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.Point
  override def firstPoint = coord
  override def centerPoint = Some(coord)
}


/** Гео-шейп круга. */
// TODO Замёржить common-модель MGeoCircle в этот шейп.
case class CircleGs(
                     center   : MGeoPoint,
                     radiusM  : Double
                   )
  extends IGeoShapeQuerable
{
  override def shapeType = GsTypes.Circle
  override def firstPoint = center
  override def centerPoint = Some(center)
}

