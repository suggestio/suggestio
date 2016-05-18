package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.magent.MResizeDelay

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
}


/**
  * Класс-контейнер модели.
  * @param resizeOpt Состояние отложенной реакции на ресайз окна, если есть.
  */
case class MScCommon(
  resizeOpt    : Option[MResizeDelay]  = None
)
