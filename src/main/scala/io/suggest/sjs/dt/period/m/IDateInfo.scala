package io.suggest.sjs.dt.period.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 21:44
  * Description: Интефейс модели инфы о какой-то дате.
  */

trait IDateInfo {

  /** Локализованная дата. */
  def date: String

  /** Локализованный день недели. */
  def dow: String

}


trait IDatesPeriodInfo {

  def start: IDateInfo

  def end: IDateInfo

}
