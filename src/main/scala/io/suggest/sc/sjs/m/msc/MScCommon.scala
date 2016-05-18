package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.magent.{IMScreen, MResizeDelay}
import io.suggest.sjs.common.model.browser.{IBrowser, MBrowser}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.16 18:38
  * Description: Модель-контейнер очень общих частей FSM-состояния [[MScSd]].
  * Появилась с целью облегчения [[MScSd]] и группировки разных простых полей верхнего уровня.
  *
  * Укорачивание [[MScSd.copy()]] также благоприятно скажется на размере скомпиленного результата.
  */
object MScCommon {

  def empty = apply()

  /** Генератор дефолтовых значений для поля generation. */
  def generationDflt = (js.Math.random() * 1000000000).toLong

  /** Генератор дефолтовых значений для поля browser. */
  def browserDflt    = MBrowser.detectBrowser
}


/**
  * Класс-контейнер модели.
  * @param resizeOpt Состояние отложенной реакции на ресайз окна, если есть.
  */
case class MScCommon(
  adnIdOpt     : Option[String]        = None,
  screen       : Option[IMScreen]      = None,
  browser      : IBrowser              = MScCommon.browserDflt,
  generation   : Long                  = MScCommon.generationDflt,
  resizeOpt    : Option[MResizeDelay]  = None
)
