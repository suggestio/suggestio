package io.suggest.maps.c

import diode._
import diode.Implicits._
import diode.data.Pot
import io.suggest.maps.m.RcvrMarkersInit
import io.suggest.maps.nodes.{MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.maps.u.IAdvRcvrsMapApi
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 16:36
  * Description: Утиль для контроллера [[RcvrMarkersInitAh]].
  */
object RcvrMarkersInitAh {

  /** Запуск инициализации карты. */
  def startInitFx(args: MRcvrsMapUrlArgs, api: IAdvRcvrsMapApi, reason: RcvrMarkersInit): Effect = {
    Effect {
      api.advRcvrsMapJson(args)
        .transform { tryResp =>
          val r = RcvrMarkersInit.resp.modify(_ withTry tryResp)(reason)
          Success( r )
        }
    }
  }

}


/** Diode action handler для обслуживания карты ресиверов. */
class RcvrMarkersInitAh[M](
                            api             : IAdvRcvrsMapApi,
                            argsRO          : ModelRO[MRcvrsMapUrlArgs],
                            modelRW         : ModelRW[M, Pot[MGeoNodesResp]],
                            isOnlineRoOpt   : Option[ModelRO[Boolean]]        = None,
                          )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Инициализация маркеров с сервера. Циклическая, чтобы подавлять возможные ошибки.
    case m: RcvrMarkersInit =>
      val v0 = value

      // Тут три варианта: пусто, exception, ответ.
      m.resp.fold {
        m.resp
          .exceptionOption
          .orElse {
            // Нет соединения? Вернуть ошибку сразу.
            Option.when( isOnlineRoOpt.fold(false)(!_.value) ) {
              new NoSuchElementException
            }
          }
          .fold {
            // Нет значения и нет ошибок. Организовать бы запуск запроса...
            // Есть связь с инетом. Запуск запроса на исполнение.
            val v2 = v0.pending()
            val m2 = RcvrMarkersInit.resp.set( v2 )(m)
            val fx = RcvrMarkersInitAh.startInitFx( argsRO.value, api, m2 )
            updatedSilent( v2, fx )

          } { ex =>
            logger.error( ErrorMsgs.INIT_RCVRS_MAP_FAIL, msg = m, ex = ex )
            // Ошибка запроса/ответа. Сохранить ошибку, запланировать повтор через какое-то время.
            // Нет связи с инетом. Сразу запустить таймер повтора позже, сохранить ошибку в состояние.
            val v2 = v0 fail ex

            // Повторные ошибочные запросы отработать по долгому таймеру.
            val fx = Effect
              .action( m )
              .after( (if (v0.isFailed) 120 else 10).seconds )

            updatedSilent( v2, fx )
          }

      } { gnResp =>
        // Ожидаемый ответ сервера по запросу. Сохранить в состояние.
        val v2 = v0.ready( gnResp )

        // Перезакачать данные через часик...
        val fx = Effect
          .action( RcvrMarkersInit() )
          .after( 1.hours )

        updated( v2, fx )
      }

  }

}
