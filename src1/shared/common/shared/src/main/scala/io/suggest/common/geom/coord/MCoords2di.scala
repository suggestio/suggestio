package io.suggest.common.geom.coord

import io.suggest.common.geom.d2.MSize2di
import io.suggest.math.SimpleArithmetics
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:08
 * Description: Модель двумерных координат объектов/сущностей рекламной карточки.
 */

object MCoords2di {

  object Fields {
    val X_FN = "x"
    val Y_FN = "y"
  }

  /** Поддержка JSON для экземпляров модели черех play-json. */
  implicit val MCOORD_2DI_FORMAT: OFormat[MCoords2di] = (
    (__ \ Fields.X_FN).format[Int] and
    (__ \ Fields.Y_FN).format[Int]
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[MCoords2di] = UnivEq.derive

  implicit object MCoords2diSimpleArithmeticHelper extends SimpleArithmetics[MCoords2di, Int] {
    override def applyMathOp(v: MCoords2di)(op: Int => Int): MCoords2di = {
      v.copy(
        x = op(v.x),
        y = op(v.y)
      )
    }
  }


  val x = GenLens[MCoords2di](_.x)
  val y = GenLens[MCoords2di](_.y)


  implicit class CoordsExt( val c2d: MCoords2di ) extends AnyVal {

    // Алиасы координат для различных случаев.
    @inline def line = c2d.y
    @inline def column = c2d.x

    @inline def top = c2d.y
    @inline def left = c2d.x


    def toDouble: MCoords2dD = {
      MCoords2dD(
        x = c2d.x.toDouble,
        y = c2d.y.toDouble
      )
    }

    def toSize = MSize2di(width = c2d.x, height = c2d.y)

    /** Поменять X и Y местами. Бывает надо для сортировки с приоритетом на Y. */
    def swap = c2d.copy(x = c2d.y, y = c2d.x)

  }

}


/** Дефолтовая реализация модели целочисленной двумерной координаты.
  *
  * @param x Горизонтальная координата.
  * @param y Вертикальная координата.
  */
case class MCoords2di(
                       x: Int,
                       y: Int
                     )
