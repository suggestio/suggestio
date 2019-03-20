package io.suggest.id.login.c

import diode.data.Pot
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.m.{ExtLoginVia, ExtLoginViaTimeout}
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.routes.routes
import io.suggest.sjs.dom.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 17:44
  * Description: Контроллер данных для внешнего логина.
  */
class ExtAh[M](
                modelRW: ModelRW[M, MExtLoginFormS]
              )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск логина через внешний сервис. Надо начать запрос ссылки.
    case m: ExtLoginVia =>
      val v0 = value
      if (v0.loginUrlReq.isPending) {
        noChange
      } else {
        val tstamp = System.currentTimeMillis()
        val fx = Effect {
          DomQuick.goToLocation( routes.controllers.Ident.idViaProvider( m.service.value ).url )
          for (_ <- DomQuick.timeoutPromise( 3000 ).fut) yield
            ExtLoginViaTimeout(tstamp)
        }
        val v2 = MExtLoginFormS.loginUrlReq.modify(_.pending(tstamp))(v0)
        updated( v2, fx )
      }


    // Логин не удался - разблокировать кнопку.
    case _: ExtLoginViaTimeout =>
      val v0 = value
      val v2 = MExtLoginFormS.loginUrlReq.set( Pot.empty )(v0)
      updated( v2 )

  }

}
