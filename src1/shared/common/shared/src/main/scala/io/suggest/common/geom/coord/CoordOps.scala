package io.suggest.common.geom.coord

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 14:34
 * Description: TypeClass'ы для операций над координатами.
 */

object CoordOps {

  def deltaX[T, Coord_t](a: T, b: T)(implicit helper: ICoord1dHelper[T, Coord_t], maths: Numeric[Coord_t]): Coord_t = {
    import maths._
    helper.getX(a) - helper.getX(b)
  }

  def deltaY[T, Coord_t](a: T, b: T)(implicit helper: ICoord2dHelper[T, Coord_t], maths: Numeric[Coord_t]): Coord_t = {
    import maths._
    helper.getY(a) - helper.getY(b)
  }

  def deltaZ[T, Coord_t](a: T, b: T)(implicit helper: ICoord3dHelper[T, Coord_t], maths: Numeric[Coord_t]): Coord_t = {
    import maths._
    helper.getZ(a) - helper.getZ(b)
  }

  /** Возведение в квадрат разницы между двумя координатами. */
  private def deltaPow2[T, Coord_t](a: T, b: T)(f: T => Coord_t)
                                   (implicit maths: Numeric[Coord_t]): Coord_t = {
    import maths._
    val p = f(a) - f(b)
    p * p
  }

  /** Абстрактный рассчёт расстояния между двумя точками в двумерном пространстве. */
  def distanceXY[T, Coord_t](a: T, b: T)
                            (implicit helper: ICoord2dHelper[T, Coord_t],
                                      maths: Numeric[Coord_t]): Double = {
    import maths._
    val dp2 = deltaPow2[T, Coord_t](a, b) _
    val v2 =
      dp2(helper.getX) +
      dp2(helper.getY)

    math.sqrt( v2.toDouble() )
  }


  /** Расстояние между точками в трёхмерном пространстве. */
  def distanceXYZ[T, Coord_t](a: T, b: T)
                             (implicit helper: ICoord3dHelper[T, Coord_t], maths: Numeric[Coord_t]): Double = {
    // Теорема пифагора по трём осям:
    import maths._
    val dp2 = deltaPow2[T, Coord_t](a, b) _
    val v2 =
      dp2(helper.getX) +
      dp2(helper.getY) +
      dp2(helper.getZ)

    math.sqrt( v2.toDouble() )
  }

}

/** Интерфейс typeclass'а для доступа к X-координате. */
trait ICoord1dHelper[T, Coord_t] {
  def getX(t: T): Coord_t
}
/** Интерфейс typeclass'а для доступа к Y-координате. */
trait ICoord2dHelper[T, Coord_t] extends ICoord1dHelper[T, Coord_t] {
  def getY(t: T): Coord_t
}
trait ICoord3dHelper[T, Coord_t] extends ICoord2dHelper[T, Coord_t] {
  def getZ(t: T): Coord_t
}
