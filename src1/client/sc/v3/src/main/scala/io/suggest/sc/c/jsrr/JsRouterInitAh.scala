package io.suggest.sc.c.jsrr

import diode._
import io.suggest.sc.m._
import io.suggest.sc.m.in.MJsRouterS
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:53
  * Description: Обработчик событий готовности js-роутера к работе.
  */
class JsRouterInitAh[M <: AnyRef](
                                   circuit: Circuit[M],
                                   modelRW: ModelRW[M, MJsRouterS]    // TODO Сделать MJsRouterS после удаления отсюда RouteTo.
                                 )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к запуску инициализации js-роутера.
    case JsRouterInit =>
      val v0 = value
      if (v0.jsRouter.isReady || v0.jsRouter.isPending) {
        // Инициализация уже запущена или выполнена ранее.
        noChange

      } else {
        // Нужно запустить инициалиазацию js-роутера:
        val fx = Effect {
          SrvRouter
            .ensureJsRouter()
            .transform { tryRes =>
              Success(JsRouterStatus(tryRes))
            }
        }
        val v2 = MJsRouterS.jsRouter
          .modify( _.pending() )(v0)

        // silent - потому что pending никого не интересует.
        updatedSilent(v2, fx)
      }


    // Сигнал готовности и проблеме инициализации роутера.
    case m: JsRouterStatus =>
      val v0 = value

      var stateMods = MJsRouterS.jsRouter.modify { jsRouterPot0 =>
        m.payload.fold( jsRouterPot0.fail, jsRouterPot0.ready )
      }
      var fxsAcc = List.empty[Effect]

      // Если задана SPA-роута, и js-роутер готов, повторно вызвать SPA-роуту:
      for {
        routeTo <- v0.delayedRouteTo
        if m.payload.isSuccess
      } {
        fxsAcc ::= routeTo.toEffectPure
        stateMods = stateMods andThen MJsRouterS.delayedRouteTo.set(None)
      }

      // Если неудача, то надо попробовать ещё раз:
      if (m.payload.isFailure) {
        fxsAcc ::= Effect {
          DomQuick
            .timeoutPromiseT( 300 )( JsRouterInit )
            .fut
        }
      }

      // Обновить состояние и закончить.
      val v2 = stateMods( v0 )

      ah.updatedMaybeEffect( v2, fxsAcc.mergeEffects )

  }

}
