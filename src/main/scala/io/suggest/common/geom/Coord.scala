package io.suggest.common.geom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 11:16
 * Description: Сборка хранения абстрактных координат в n-мерном эвклидовом пространстве.
 */
trait ICoord {
  /** Тип значения координат. */
  type V <: AnyVal
}

trait ICoord1d extends ICoord {
  /** Значение по оси абсцисс. */
  def x: V
}

trait ICoord2d extends ICoord1d {
  /** Значение по оси ординат. */
  def y: V
}


/** Двумерные double-координаты. */
case class Coord2dD(
  override val x: Double,
  override val y: Double
) extends ICoord2d {
  override type V = Double
}
