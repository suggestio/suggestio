package io.suggest.common.geom.d2

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


/** Дефолтовая реализация [[ISize2di]]. */
case class Size2di(width: Int, height: Int) extends ISize2di


/** Именованая версия [[ISize2di]]. Полезно для enum'ов.
  * Позволяет задать допустимый размер строковым алиасом. */
trait INamedSize2di extends ISize2di {

  /** Алиас (название) размера. */
  def szAlias: String

  override def toString = "Sz2D(" + szAlias + ",w=" + width + ";h=" + height + ")"
}
