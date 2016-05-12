package io.suggest.sc.sjs.m.magent

import io.suggest.sc.sjs.m.mgrid.ICwCm
import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.16 21:59
  * Description: Модель-контейнер данных по отложенному ресайзу.
  *
  * @param timerId id таймера
  * @param timerGen generation таймера. Нужно для подавления от ложных срабатываний в момент отмены.
  * @param screen опциональный инстанс sd0.screen на момент самого начала ресайза.
  * @param gContSz опциональный инстанс sd0.grid.data.contSzOpt на момент самого начала ресайза.
  */
case class MResizeDelay(
  timerId   : Int,
  timerGen  : Long,
  screen    : Option[IMScreen],
  gContSz   : Option[ICwCm]
)


/** Сообщение от таймера о необходимости запуска ресайза. */
case class ResizeDelayTimeout(gen: Long)
  extends IFsmMsg
