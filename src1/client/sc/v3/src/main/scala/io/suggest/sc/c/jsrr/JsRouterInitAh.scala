package io.suggest.sc.c.jsrr

import diode._
import io.suggest.sc.c.TailAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m._
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sc.m.jsrr.MJsRouterS
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._

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
                                   modelRW: ModelRW[M, MScInternals]
                                 )
  extends ActionHandler( modelRW )
{

  private def _jsRouterPotLens =
    MScInternals.jsRouter composeLens MJsRouterS.jsRouter


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к запуску инициализации js-роутера.
    case JsRouterInit =>
      val v0 = value
      if (v0.jsRouter.jsRouter.isReady || v0.jsRouter.jsRouter.isPending) {
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
        val v2 = _jsRouterPotLens
          .modify(_.pending())(v0)

        // silent - потому что pending никого не интересует.
        updatedSilent(v2, fx)
      }


    // Сигнал готовности и проблеме инициализации роутера.
    case m: JsRouterStatus =>
      val v0 = value

      // Сохранить инфу по роутеру в состояние.
      val v1 = _jsRouterPotLens.modify { jsRouterPot0 =>
        m.payload.fold( jsRouterPot0.fail, jsRouterPot0.ready )
      }(v0)

      updated( v1 )


    // Костыль: перехват экшена инициализации выдачи из SPA-роутера, пока js-роутер не готов.
    case m: RouteTo if !value.jsRouter.jsRouter.isReady =>
      // Эффект откладывания сообщения напотом, когда роутер будет готов.
      // TODO Должно быть без такого страшного subscribe-эффекта. Можно это сделать просто на основе состояния, вынеся обработку в JsRouterStatus m.payload.success-ветвь
      val delayedRouteToFx = Effect {
        val p = Promise[None.type]()
        val unsubscribeF = circuit.subscribe(modelRW.zoom(_.jsRouter)) { jsRouterPotProxy =>
          val jsRouterPot = jsRouterPotProxy.value.jsRouter
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

      // Эффект запуска всех доступных вариантов геолокации:
      val geoLocEnableFx = GeoLocOnOff(enabled = true, isHard = false)
        .toEffectPure

      // Запустить в фоне геолокацию, если нет полезной инфы в принятом состоянии.
      // И запрос js-роутера с сервера и запрос геолокации пойдут параллельно.
      if (m.mainScreen.needGeoLoc) {
        val v0 = value
        val switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            withWelcome = true,
            geoIntoRcvr = true,
            retUserLoc  = true,
          )
        )

        val (v2, timeoutFx) = TailAh.mkGeoLocTimer( switchCtx, v0 )

        // Склеить все эффекты и обновить состояние.
        val allFxs = (delayedRouteToFx :: geoLocEnableFx :: timeoutFx :: Nil)
          .mergeEffects
          .get
        updatedSilent(v2, allFxs)

      } else {
        // Уже известны какие-то данные для запуска выдачи. Значит, просто ждём js-роутер с сервера.
        val allFxs = delayedRouteToFx + geoLocEnableFx
        effectOnly( allFxs )
      }

  }

}
