package io.suggest.common.geom.coord

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 14:34
 * Description: TypeClass'ы для операций над координатами.
 */

trait ICoord1dOps[V, T <: ICoord1d[V]] {
  /** Растояние по X. */
  def deltaX(p1: T, p2: T): V
}

/** Интерфейс typeclass'а для двумерных координат. */
trait ICoord2dOps[V, T <: ICoord2d[V]] {
  /** Расстояние между точками. */
  def distance(p1: T, p2: T): V
  /** Расстояние по Y. */
  def deltaY(p1: T, p2: T): V
}

object CoordOps {

  sealed class Coord1dDOpsImpl extends ICoord1dOps[Double, ICoord1d[Double]] {
    override def deltaX(p1: ICoord1d[Double], p2: ICoord1d[Double]): Double = {
      p1.x - p2.x
    }
  }

  /** typeclass для операций над одномерными Double координатами. */
  implicit lazy val coord1dDoubleOps = new Coord1dDOpsImpl

  /** typeclass для операций над 2-мерными Double координатами. */
  implicit lazy val coord2dDoubleOps = new Coord1dDOpsImpl with ICoord2dOps[Double, ICoord2d[Double]] {

    override def distance(p1: ICoord2d[Double], p2: ICoord2d[Double]): Double = {
      // Теорема пифагора.
      Math.sqrt( Math.pow(deltaX(p2, p1), 2.0)  +  Math.pow(deltaY(p2, p1), 2.0) )
    }

    override def deltaY(p1: ICoord2d[Double], p2: ICoord2d[Double]): Double = {
      p1.y - p2.y
    }
  }

}
