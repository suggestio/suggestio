package io.suggest.sc.sjs.m.mgrid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:45
 * Description: Модель состояния cbca_grid.
 */
case class MGridState(
  var cellSize      : Int = 140,
  var cellPadding   : Int = 20,
  var topOffset     : Int = 70,
  var bottomOffset  : Int = 20,
  var maxCellWidth  : Int = 4,
  var leftOffset    : Int = 0,
  var rightOffset   : Int = 0,
  var columnsCnt    : Int = -1,
  var blocks        : List[_] = Nil,
  var spacers       : List[_] = Nil
) {

  def margin(colCnt1: Int): Int = {
    (colCnt1 - 1) * (cellSize + cellPadding) + cellSize
  }

}
