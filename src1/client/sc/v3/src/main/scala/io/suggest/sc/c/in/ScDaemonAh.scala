package io.suggest.sc.c.in

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.m.DaemonActivated
import io.suggest.sc.m.in.MScDaemonInfo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 20:42
  * Description: Контроллер интеграции выдачи и подсистем дял демонизации.
  */
class ScDaemonAh[M](
                     modelRW : ModelRW[M, MScDaemonInfo],
                   )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: DaemonActivated =>
      if (m.isActive) {
        // Активация демона. TODO Надо запустить таймер BLE-сканирования.
        ???
      } else {
        // Деактивация демона. TODO Остановить демонический таймер сканирования BLE.
        ???
      }

  }

}
