package io.suggest.common.geom.d2

import io.suggest.math.{IBinaryMathOp, IntMathModifiers}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:58
 * Description: Размер двумерный целочисленный.
 */

trait ISize2di extends IWidth with IHeight {

  /** Сравнение c другим размером. */
  def sizeWhEquals(sz1: ISize2di): Boolean = {
    sz1.width == width  &&  sz1.height == height
  }

  /** Вернуть инстанс [[Size2di]]. */
  def toSize2di: Size2di = {
    Size2di(width = width, height = height)
  }

  override def toString: String = "Sz2D(w=" + width + ";h=" + height + ")"
}

/** Интерфейс для доступа к ширине. Т.е. одномерная проекция горизонтального размера. */
trait IWidth {
  /** Ширина. */
  def width: Int
  override def toString = "W(" + width + ")"
}

/** Интерфейс для доступа к высоте. Т.е. одномерная проекция вертикального размера. */
trait IHeight {
  /** Высота. */
  def height: Int
  override def toString = "H(" + height + ")"
}


object Size2di {

  import boopickle.Default._

  /** Поддержка boopickle для инстансов [[Size2di]]. */
  implicit val size2diPickler: Pickler[Size2di] = {
    generatePickler[Size2di]
  }

}

/** Дефолтовая реализация [[ISize2di]]. */
final case class Size2di(
                          override val width  : Int,
                          override val height : Int
                        )
  extends ISize2di
  with IntMathModifiers[Size2di]
{

  override def toSize2di = this

  /** Модифицировать ширину и длину одной математической операцией. */
  override protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): Size2di = {
    copy(
      width   = op(width, arg2),
      height  = op(height, arg2)
    )
  }

}


/** Именованая версия [[ISize2di]]. Полезно для enum'ов.
  * Позволяет задать допустимый размер строковым алиасом. */
trait INamedSize2di extends ISize2di {

  /** Алиас (название) размера. */
  def szAlias: String

  override def toString = "Sz2D(" + szAlias + ",w=" + width + ";h=" + height + ")"
}


/** Враппер для модели [[ISize2di]]. */
trait ISize2diWrap extends ISize2di {

  def _sz2dUnderlying: ISize2di

  override def height = _sz2dUnderlying.height
  override def width  = _sz2dUnderlying.width

}