package io.suggest.sc.router.c

import diode.{ActionHandler, ActionResult, ModelRW}
import diode.data.Pot
import io.suggest.sc.root.m.JsRouterStatus
import io.suggest.sc.router.routes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:53
  * Description: Обработчик событий готовности js-роутера к работе.
  */
class JsRouterInitAh[M](
                         modelRW: ModelRW[M, Pot[routes.type]]
                       )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал готовности и проблеме инициализации роутера.
    case m: JsRouterStatus =>
      val v0 = value
      // Сохранить инфу по роутеру в состояние.
      val v1 = m.payload.fold( v0.fail, v0.ready )
      updated( v1 )

  }

}
