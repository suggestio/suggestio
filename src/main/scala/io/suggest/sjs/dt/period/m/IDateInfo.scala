package io.suggest.sjs.dt.period.m

import io.suggest.dt.interval.DatesIntervalConstants.Json._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 21:44
  * Description: Интефейс модели инфы о какой-то дате.
  */

@js.native
trait IDateInfo extends js.Object {

  /** Локализованная дата. */
  @JSName( DATE_FN )
  def date: String

  /** Локализованный день недели. */
  @JSName( DOW_FN )
  def dow: String

}


@js.native
trait IDatesPeriodInfo extends js.Object {

  /** Описание даты начала размещения. */
  @JSName( START_FN )
  def start: IDateInfo

  /** Описание даты окончания размещения. */
  @JSName( END_FN )
  def end: IDateInfo

}
