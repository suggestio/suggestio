package io.suggest.common.geom.coord

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 11:16
 * Description: Сборка хранения абстрактных координат в n-мерном эвклидовом пространстве.
 */
trait ICoord[V]

trait ICoord1d[V] extends ICoord[V] {

  type This1 = ICoord1d[V]

  /** Значение по оси абсцисс. */
  def x: V

  def deltaX(p2: This1)(implicit evidence: ICoord1dOps[V, This1]): V = {
    evidence.deltaX(this, p2)
  }

}


trait ICoord2d[V] extends ICoord1d[V] {

  type This2 = ICoord2d[V]

  /** Значение по оси ординат. */
  def y: V

  def deltaY(p2: This2)(implicit evidence: ICoord2dOps[V, This2]): V = {
    evidence.deltaY(this, p2)
  }

  def distance(p2: This2)(implicit evidence: ICoord2dOps[V, This2]): V = {
    evidence.distance(this, p2)
  }

}


/** 2D int-координаты. */
trait ICoords2di
  extends ICoord2d[Int]
