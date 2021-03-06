package io.suggest.sjs.common.model

import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js.timers.SetTimeoutHandle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.18 18:24
  * Description: Модель представления пары чисел: timerId и доп.отметки timestamp, чтобы
  * точно быть уверенным в срабатывании ожидаемого таймера, подавляя срабатывания
  * предыдущих таймеров.
  */
object MTsTimerId {

  @inline implicit def univEq: UnivEq[MTsTimerId] = UnivEq.derive

}

case class MTsTimerId(
                       timerId    : SetTimeoutHandle,
                       timestamp  : Long = System.currentTimeMillis()
                     )
