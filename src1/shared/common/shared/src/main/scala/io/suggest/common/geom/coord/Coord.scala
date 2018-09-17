package io.suggest.common.geom.coord

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 11:16
 * Description: Сборка хранения абстрактных координат в n-мерном эвклидовом пространстве.
 */
object ICoord {

  val X_FN = "x"
  val Y_FN = "y"

}


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

  def distance2dTo(p2: This2)(implicit evidence: ICoord2dOps[V, This2]): V = {
    evidence.distance(this, p2)
  }

  def tuple2 = (x, y)
  def tuple2swap = (y, x)

}



object ICoord3d {

  /** Дистанция между точками в трехмерном пространстве цветов. Считаем по теореме Пифагора. */
  def distance[V](p1: ICoord3d[V], p2: ICoord3d[V])(implicit n: Numeric[V]): Double = {
    import n._
    val exp = 2d
    val expDst = Math.pow( (p2.x - p1.x).toDouble(), exp) +
      Math.pow((p2.y - p1.y).toDouble(), exp) +
      Math.pow((p2.z - p1.z).toDouble(), exp)
    Math.pow(expDst, 1d / exp)
  }

}


trait ICoord3d[V] extends ICoord2d[V] {

  type This3 = ICoord3d[V]

  def z: V

  def distance3dTo(p2: This3)(implicit n: Numeric[V]): Double = {
    ICoord3d.distance(this, p2)
  }

}
