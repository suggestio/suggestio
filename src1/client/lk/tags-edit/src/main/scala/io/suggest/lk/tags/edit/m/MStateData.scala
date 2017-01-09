package io.suggest.lk.tags.edit.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.15 16:36
  * Description: Контейнер данных состояния tags edit fsm.
  * @param startSearchTimerId id таймера запуска запроса поиска тегов по имени.
  * @param lastSearchReqTs таймптамп свежайшего поискового запроса к серверу.
  */
case class MStateData(
  startSearchTimerId    : Option[Int]   = None,
  startSearchTimerTs    : Option[Long]  = None,
  lastSearchReqTs       : Option[Long]  = None
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
