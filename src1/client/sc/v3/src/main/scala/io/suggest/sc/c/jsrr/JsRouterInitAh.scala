package io.suggest.sc.c.jsrr

import diode._
import io.suggest.sc.m._
import io.suggest.sc.m.in.MJsRouterS
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:53
  * Description: Обработчик событий готовности js-роутера к работе.
  */
class JsRouterInitAh[M <: AnyRef](
                                   modelRW: ModelRW[M, MJsRouterS]
                                 )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к запуску инициализации js-роутера.
    case m: JsRouterInit =>
      val v0 = value

      if (m.status.isEmpty && !m.status.isFailed) {
        if (v0.jsRouter.isReady || v0.jsRouter.isPending) {
          // Инициализация уже запущена или выполнена ранее.
          noChange

        } else {
          // Нужно запустить инициалиазацию js-роутера:
          val fx = Effect {
            SrvRouter
              .ensureJsRouter()
              .transform { tryRes =>
                val m2 = m.copy(
                  status = m.status withTry tryRes,
                )
                Success( m2 )
              }
          }
          val v2 = MJsRouterS.jsRouter
            .set( m.status.pending() )(v0)

          // silent - потому что pending никого не интересует.
          updatedSilent(v2, fx)
        }

      } else {
        // Сигнал готовности и проблеме инициализации роутера.
        var stateMods = MJsRouterS.jsRouter.set( m.status )
        var fxsAcc = List.empty[Effect]

        // Если неудача, то надо попробовать ещё раз:
        if (m.status.isFailed || m.status.isUnavailable) {
          fxsAcc ::= Effect {
            DomQuick
              .timeoutPromiseT( 300 )( JsRouterInit() )
              .fut
          }
        }

        // Обновить состояние и закончить.
        val v2 = stateMods( v0 )

        ah.updatedMaybeEffect( v2, fxsAcc.mergeEffects )
      }

  }

}
