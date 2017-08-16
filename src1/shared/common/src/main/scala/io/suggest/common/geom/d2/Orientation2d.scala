package io.suggest.common.geom.d2

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import io.suggest.primo.IStrId
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 13:31
  * Description: Модель воспринимаемых вариантов двумерной ориентации плоской фигуры.
  *
  * В основном -- это для хранения статистики на сервере.
  */

object MOrientation2d {

  /** Поддержка play-json. */
  implicit val MORIENTATION2D_FORMAT: Format[MOrientation2d] = {
    EnumeratumUtil.enumEntryFormat( MOrientations2d )
  }

}


/** Класс одного элемента модели [[MOrientations2d]]. */
sealed abstract class MOrientation2d
  extends EnumEntry
  with IStrId
{
  override final def toString = super.toString
}


/** Модель воспринимаемых двумерных ориентаций.
  * strId все являются human-readable, потому модель используется для хранения статистики и kibana.
  */
object MOrientations2d extends Enum[MOrientation2d] {

  case object Vertical extends MOrientation2d {
    override def strId = "vert"
  }

  case object Horizontal extends MOrientation2d {
    override def strId = "horiz"
  }

  case object Square extends MOrientation2d {
    override def strId = "square"
  }

  override val values = findValues


  def forSize2d(sz2d: ISize2di): MOrientation2d = {
    if ( ISize2di.isVertical(sz2d) ) {
      Vertical
    } else if ( ISize2di.isHorizontal(sz2d) ) {
      Horizontal
    } else {
      Square
    }
  }

}
