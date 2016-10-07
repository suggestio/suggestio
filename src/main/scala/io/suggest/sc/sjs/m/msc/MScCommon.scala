package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.magent.{IMScreen, MResizeDelay}
import io.suggest.sc.sjs.m.mtags.MTagInfo
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.model.loc.IGeoLocMin

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.16 18:38
  * Description: Модель-контейнер очень общих частей FSM-состояния [[MScSd]].
  * Появилась с целью облегчения [[MScSd]] и группировки разных простых полей верхнего уровня.
  */
trait IScCommon {

  def screen       : IMScreen
  def browser      : IBrowser
  def generation   : Long
  def adnIdOpt     : Option[String]
  def resizeOpt    : Option[MResizeDelay]
  def geoLocOpt    : Option[IGeoLocMin]
  def tagOpt       : Option[MTagInfo]

}


/**
  * Класс-реализация модели.
  *
  * @param screen Данные о текущем экране.
  * @param resizeOpt Состояние отложенной реакции на ресайз окна, если есть.
  * @param browser Данные по браузеру юзера.
  * @param generation "Поколение" выдачи, т.е. random seed.
  * @param adnIdOpt id текущего узла-ресивера выдачи, если есть.
  * @param tagOpt Инфа о текущем теге, если он выбран. Выставляется уведомлением из TagsFsm.
  */
case class MScCommon(
  override val screen       : IMScreen,
  override val browser      : IBrowser,
  override val generation   : Long,
  override val adnIdOpt     : Option[String]        = None,
  override val resizeOpt    : Option[MResizeDelay]  = None,
  override val geoLocOpt    : Option[IGeoLocMin]    = None,
  override val tagOpt       : Option[MTagInfo]      = None
)
  extends IScCommon
