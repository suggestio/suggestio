package io.suggest.grid.build

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 15:52
  * Description: Модель-контейнер переменных по одной колонке.
  */
object MColumnState {

  @inline implicit def univEq: UnivEq[MColumnState] = UnivEq.derive

  val heightUsed = GenLens[MColumnState](_.heightUsed)

}

case class MColumnState(
                         heightUsed   : Int     = 0
                       )


