package io.suggest.common.geom.coord

import io.suggest.math.{DoubleMathModifiers, IBinaryMathOp}
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
  implicit val MCOORD_2DD_FORMAT: OFormat[MCoords2dD] = (
    (__ \ ICoord.X_FN).format[Double] and
    (__ \ ICoord.Y_FN).format[Double]
  )(apply, unlift(unapply))


  implicit def univEq: UnivEq[MCoords2di] = UnivEq.derive

}


/** Двумерные double-координаты. */
case class MCoords2dD(
  override val x: Double,
  override val y: Double
)
  extends ICoord2d[Double]
  with DoubleMathModifiers[MCoords2dD]
{

  override protected[this] def applyMathOp(op: IBinaryMathOp[Double], arg2: Double): MCoords2dD = {
    copy(
      x = op(x, arg2),
      y = op(y, arg2)
    )
  }

  def toInt: MCoords2di = {
    MCoords2di(
      x = x.toInt,
      y = y.toInt
    )
  }

  def withX(x: Double) = copy(x = x)
  def withY(y: Double) = copy(y = y)

}
