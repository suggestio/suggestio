package io.suggest.sc.sjs.m.mtags


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.16 14:45
  * Description: Модель state data для тегов на соотв.табе.
  *
  * @param loadedCount кол-во уже загруженных и отрендеренных тегов.
  *                    Нужно для задания offset при последующем tags-search-запросах.
  * @param loadingDone Больше на сервере нет тегов для дальнейшей подгрузки.
  * @param currReqTs timestamp текущего реквеста к серверу, если есть.
  */
case class MTagsSd(
  loadedCount     : Int             = 0,
  loadingDone     : Boolean         = false,
  currReqTs       : Option[Long]    = None
) {

  /** @return true, если загружены/загружаются какие-то карточки. */
  def isLoadedSomething: Boolean = {
    loadedCount > 0 || loadingDone || currReqTs.nonEmpty
  }

}
