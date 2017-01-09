package io.suggest.model.geom.d2

import io.suggest.common.geom.d2.Orientations2d
import io.suggest.common.geom.d2.Orientation2d._
import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 13:53
  * Description: Реализация модели двумерных ориентаций.
  */
object MOrientations2d extends EnumMaybeWithName with Orientations2d with EnumJsonReadsValT {

  protected class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val


  override val Vertical    : T = new Val(VERTICAL)
  override val Horizontal  : T = new Val(HORIZONTAL)
  override val Square      : T = new Val(SQUARE)

}
