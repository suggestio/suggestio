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

  val occupiedRev = GenLens[MColumnState](_.occupiedRev)

}


/** Состояние заполненности одной колонки.
  *
  * @param occupiedRev Сколько занято в колонке.
  */
case class MColumnState(
                         occupiedRev     : List[MPxInterval]     = Nil,
                       ) {

  def heightUsed: Int = {
    occupiedRev
      .headOption
      .fold(0) { ivl => ivl.startPx + ivl.sizePx }
  }

}


/** Пиксельный интервал.
  *
  * @param startPx Начало промежутка.
  * @param sizePx Размер промежутка.
  * @param block Данные по блоку
  */
case class MPxInterval(
                        startPx  : Int,
                        sizePx   : Int,
                        block    : MGbBlock,
                      )
object MPxInterval {

  @inline implicit def univEq: UnivEq[MPxInterval] = UnivEq.derive

}

