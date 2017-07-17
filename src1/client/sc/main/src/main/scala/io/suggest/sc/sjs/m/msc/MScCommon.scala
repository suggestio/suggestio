package io.suggest.sc.sjs.m.msc

import io.suggest.ble.MUidBeacon
import io.suggest.dev.MScreen
import io.suggest.geo.{MGeoLoc, MLocEnv}
import io.suggest.sc.sjs.m.magent.MResizeDelay
import io.suggest.sc.sjs.m.mtags.MTagInfo
import io.suggest.sjs.common.model.browser.IBrowser

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.16 18:38
  * Description: Модель-контейнер очень общих частей FSM-состояния [[MScSd]].
  * Появилась с целью облегчения [[MScSd]] и группировки разных простых полей верхнего уровня.
  */
trait IScCommon {

  def screen       : MScreen
  def browser      : IBrowser
  def generation   : Long
  def adnIdOpt     : Option[String]
  def resizeOpt    : Option[MResizeDelay]
  def geoLocOpt    : Option[MGeoLoc]
  def tagOpt       : Option[MTagInfo]
  def bleBeacons   : Seq[MUidBeacon]

  def locEnv = MLocEnv(geoLocOpt, bleBeacons)

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
  * @param bleBeacons BLE-маячки, на которые ориентирована текущая выдача.
  */
case class MScCommon(
                      override val screen       : MScreen,
                      override val browser      : IBrowser,
                      override val generation   : Long,
                      override val adnIdOpt     : Option[String]        = None,
                      override val resizeOpt    : Option[MResizeDelay]  = None,
                      override val geoLocOpt    : Option[MGeoLoc]       = None,
                      override val tagOpt       : Option[MTagInfo]      = None,
                      override val bleBeacons   : Seq[MUidBeacon]      = Nil
)
  extends IScCommon
{

  def withBeacons(beacons2: Seq[MUidBeacon]) = copy(bleBeacons = beacons2)

  def withGeoLoc(geoLocOpt2: Option[MGeoLoc]) = copy(geoLocOpt = geoLocOpt2)

  def withTagInfo(tagInfo2: Option[MTagInfo]) = copy(tagOpt = tagInfo2)

  def withAdnId(adnIdOpt2: Option[String]) = copy(adnIdOpt = adnIdOpt2)

}
