package io.suggest.common.geom.d2

import io.suggest.common.menum.{ILightEnumeration, StrIdValT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 13:31
  * Description: Модель двумерной ориентации.
  */

object Orientation2d {

  def VERTICAL    = "vert"

  def HORIZONTAL  = "horiz"

  def SQUARE      = "square"

}


/** Заготовка для моделей 2d-ориентации. */
trait Orientations2d extends ILightEnumeration with StrIdValT {

  protected trait ValT extends super.ValT

  override type T <: ValT

  /** Вертикальна ориентация. */
  val Vertical    : T

  /** Горизонтальный ориентация. */
  val Horizontal  : T

  /** Квадратная ориентация. */
  val Square      : T

}
