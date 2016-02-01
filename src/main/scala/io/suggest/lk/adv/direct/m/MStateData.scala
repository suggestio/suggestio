package io.suggest.lk.adv.direct.m

import io.suggest.sjs.interval.m.PeriodEith_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 16:27
 * Description: Модель данных состояния FSM формы прямого размещения.
 */

trait IStateData {

  /** Текущий период/интервал размещения. */
  def period      : PeriodEith_t

  /** id текущего города или None, если город не выбран. */
  def currCityId  : Option[String]

  /**
    * id текущей группы узлов.
 *
    * @return None -- нет текущей вкладки.
    *         Some(None) -- выбраны все места.
    *         Some(Some(id)) -- выбрана указанная категория.
    */
  def currNgId    : Option[Option[String]]

  /** Если послан запрос к серверу, то сюда сохранен timestamp для защиты от race conditions. */
  def getPriceTs  : Option[Long]

}


/** Реализация модели. */
case class MStateData(
  override val period         : PeriodEith_t,
  override val currCityId     : Option[String]              = None,
  override val currNgId       : Option[Option[String]]      = None,
  override val getPriceTs     : Option[Long]                = None
)
  extends IStateData
