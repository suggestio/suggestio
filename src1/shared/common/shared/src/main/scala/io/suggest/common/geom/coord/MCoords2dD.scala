package io.suggest.common.geom.coord

import io.suggest.math.SimpleArithmetics
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 13:36
 * Description: Реализация координат для нужд
 */

object MCoords2dD {

  /** Поддержка JSON для экземпляров модели черех play-json. */
  implicit val MCOORD_2DD_FORMAT: OFormat[MCoords2dD] = {
    val F = MCoords2di.Fields
    (
      (__ \ F.X_FN).format[Double] and
      (__ \ F.Y_FN).format[Double]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MCoords2di] = UnivEq.derive

  implicit object MCoords2dDSimpleArithmeticHelper extends SimpleArithmetics[MCoords2dD, Double] {
    override def applyMathOp(v: MCoords2dD)(op: Double => Double): MCoords2dD = {
      v.copy(
        x = op(v.x),
        y = op(v.y)
      )
    }
  }

}


/** Двумерные double-координаты. */
case class MCoords2dD(
                       x: Double,
                       y: Double,
                     ) {

  def toInt: MCoords2di = {
    MCoords2di(
      x = x.toInt,
      y = y.toInt
    )
  }

  def withX(x: Double) = copy(x = x)
  def withY(y: Double) = copy(y = y)

}
