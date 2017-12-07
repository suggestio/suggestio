package io.suggest.sc.router.c

import diode._
import diode.data.Pot
import io.suggest.routes.scRoutes
import io.suggest.sc.root.m.{JsRouterInit, JsRouterStatus, RouteTo}
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Promise
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:53
  * Description: Обработчик событий готовности js-роутера к работе.
  */
class JsRouterInitAh[M <: AnyRef](
                                   circuit: Circuit[M],
                                   modelRW: ModelRW[M, Pot[scRoutes.type]]
                                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к запуску инициализации js-роутера.
    case JsRouterInit =>
      val v0 = value
      if (v0.isReady || v0.isPending) {
        // Инициализация уже запущена или выполнена ранее.
        noChange

      } else {
        // Нужно запустить инициалиазацию js-роутера:
        val fx = Effect {
          SrvRouter.ensureJsRouter()
            .transform { tryRes => Success(JsRouterStatus(tryRes)) }
        }
        val v2 = v0.pending()

        // silent - потому что pending никого не интересует.
        updatedSilent(v2, fx)
      }


    // Сигнал готовности и проблеме инициализации роутера.
    case m: JsRouterStatus =>
      val v0 = value
      // Сохранить инфу по роутеру в состояние.
      val v1 = m.payload.fold( v0.fail, v0.ready )
      updated( v1 )


    // Костыль: перехват экшена инициализации выдачи из SPA-роутера, пока js-роутер не готов.
    case m: RouteTo if !value.isReady =>
      // Эффект откладывания сообщения напотом, когда роутер будет готов.
      val fx = Effect {
        val p = Promise[None.type]()
        val unsubscribeF = circuit.subscribe(modelRW) { jsRouterPotProxy =>
          val jsRouterPot = jsRouterPotProxy.value
          if (jsRouterPot.nonEmpty) {
            p.success(None)
          } else if (jsRouterPot.isFailed) {
            p.failure(jsRouterPot.exceptionOption.get)
          }
        }
        p.future.transform { _ =>
          unsubscribeF()
          Success(m)
        }
      }

      effectOnly(fx)

  }

}
