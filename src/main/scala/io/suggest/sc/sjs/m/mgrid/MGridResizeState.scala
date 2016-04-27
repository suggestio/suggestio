package io.suggest.sc.sjs.m.mgrid

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.16 21:59
  * Description: Модель-контейнер данных по ресайзу плитки.
  * @param timerId id таймера
  * @param timerGen generation таймера. Нужно для подавления от ложных срабатываний в момент отмены.
  */
case class MGridResizeState(
  timerId   : Int,
  timerGen  : Long
)
