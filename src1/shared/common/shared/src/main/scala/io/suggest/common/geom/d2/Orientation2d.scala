package io.suggest.common.geom.d2

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 13:31
  * Description: Модель воспринимаемых вариантов двумерной ориентации плоской фигуры.
  *
  * В основном -- это для хранения статистики на сервере.
  */

/** Модель воспринимаемых двумерных ориентаций.
  * strId все являются human-readable, потому модель используется для хранения статистики и kibana.
  */
object MOrientations2d extends StringEnum[MOrientation2d] {

  case object Vertical extends MOrientation2d("vert")

  case object Horizontal extends MOrientation2d("horiz")

  case object Square extends MOrientation2d("square")


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


/** Класс одного элемента модели [[MOrientations2d]]. */
sealed abstract class MOrientation2d(override val value: String) extends StringEnumEntry {
  override final def toString = value
}


object MOrientation2d {

  /** Поддержка play-json. */
  implicit def MORIENTATION2D_FORMAT: Format[MOrientation2d] = {
    EnumeratumUtil.valueEnumEntryFormat( MOrientations2d )
  }

  @inline implicit def univEq: UnivEq[MOrientation2d] = UnivEq.derive

}

