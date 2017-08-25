package io.suggest.common.geom.coord

import io.suggest.math.{IBinaryMathOp, IntMathModifiers}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:08
 * Description: Модель двумерных координат объектов/сущностей рекламной карточки.
 */

object MCoords2di {

  val X_FN = "x"
  val Y_FN = "y"

  /** Поддержка JSON для экземпляров модели черех play-json. */
  implicit val FORMAT: OFormat[MCoords2di] = (
    (__ \ X_FN).format[Int] and
    (__ \ Y_FN).format[Int]
  )(apply, unlift(unapply))


  implicit def univEq: UnivEq[MCoords2di] = UnivEq.derive

}


/** Дефолтовая реализация модели целочисленной двумерной координаты.
  *
  * @param x Горизонтальная координата.
  * @param y Вертикальная координата.
  */
case class MCoords2di(
                      override val x: Int,
                      override val y: Int
                    )
  extends ICoord2d[Int]
  with IntMathModifiers[MCoords2di]
{

  override protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): MCoords2di = {
    copy(
      x = op(x, arg2),
      y = op(y, arg2)
    )
  }

}

