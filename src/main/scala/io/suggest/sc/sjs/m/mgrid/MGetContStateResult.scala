package io.suggest.sc.sjs.m.mgrid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 18:36
 * Description: Модель результатов рассчета getContainerState().
 */

case class MGetContStateResult(
  cw            : Int,
  cm            : Int,
  columnsCnt    : Int
)
  extends ICwCm with IColsWidth


trait ICwCm {
  def cw: Int
  def cm: Int
}

trait IColsWidth {
  def columnsCnt    : Int
}

