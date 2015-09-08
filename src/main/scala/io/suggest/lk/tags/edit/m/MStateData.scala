package io.suggest.lk.tags.edit.m

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:36
 * Description: Контейнер данных состояния tags edit fsm.
 * @param lastName Последние отработанное и почищенное название тега из input'а.
 * @param startSearchTimerId id таймера запуска запроса поиска тегов по имени.
 */
case class MStateData(
  lastName              : String        = "",
  startSearchTimerId    : Option[Int]   = None
) {

  def clearTimerId(): MStateData = {
    copy(
      startSearchTimerId = None
    )
  }

  def maybeClearTimerId(): MStateData = {
    if (startSearchTimerId.nonEmpty)
      clearTimerId()
    else
      this
  }

}
