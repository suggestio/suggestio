package io.suggest.ws.pool

import diode.data.Pending
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.event.DomEvents
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import io.suggest.sjs.dom.DomQuick
import io.suggest.ws.MWsMsg
import io.suggest.ws.pool.m._
import org.scalajs.dom
import org.scalajs.dom.raw.WebSocket
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent}
import play.api.libs.json.Json

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 18:05
  * Description: Action handler, поддерживающий собственный пул непостоянных websocket-коннекшенов.
  * Возник из-за необходимости поддерживать недолгие ws-связи с произвольными серверами.
  */
class WsPoolAh[M](
                   modelRW      : ModelRW[M, MWsPoolS],
                   wsChannelApi : IWsChannelApi,
                   dispatcher   : diode.Dispatcher
                 )
  extends ActionHandler(modelRW)
  with Log
{

  /** Подписаться на глобальные события. Вызывается из circuita только один раз. */
  def initGlobalEvents(): Unit = {
    dom.window.addEventListener4s( DomEvents.BEFORE_UNLOAD ) { _: Event =>
      dispatcher( WsCloseAll )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сообщение из открытого сокета.
    case m: WsRawMsg =>
      val v0 = value
      v0.conns.get( m.target ).fold {
        // Внезапно, не найден коннекшен.
        LOG.warn( WarnMsgs.UNKNOWN_CONNECTION, msg = m )
        noChange

      } { _ =>
        // Есть коннекшен, значит разрешаем полученное сообщение. Парсим и отправляем наверх, чтобы кто-нибудь другой подхватил.
        val wsMsg = WsChannelMsg(
          target = m.target,
          msg = Json
            .parse(m.payload.asInstanceOf[String])
            .as[MWsMsg]
        )
        val mWsMsgFx = wsMsg.toEffectPure
        effectOnly( mWsMsgFx )
      }


    // Поступил запрос на открытие ws-коннекшена.
    case m: WsEnsureConn =>
      val v0 = value
      val key = m.target
      v0.conns.get( key ).fold {
        // Нет коннекшена с данными координатами. Просто открываем новый ws-коннекшен:
        val wsOpenFx = Effect {
          wsChannelApi
            .wsChannel(key)
            .transform { tryRes =>
              Success( WsOpenedConn(key, tryRes, m.closeAfterSec) )
            }
        }

        // Собрать состояние WS-коннекшена и сохранить.
        val wsConnS = MWsConnS(
          target     = key,
          conn        = Pending(),
          closeTimer  = None
        )
        val v2 = v0.withConns(
          v0.conns.updated(key, wsConnS)
        )
        updated( v2, wsOpenFx )

      } { existConn =>
        // Запрошенный коннекшен уже существует.
        // обновляем таймер автозакрытия коннекшена на новое значение:
        val conn1 = existConn.withCloseTimer(
          _closeTimerOpt(key, m.closeAfterSec)
        )

        // Сохранить новое состояние коннекшена в состояние:
        val v2 = v0.withConns(
          v0.conns.updated(key, conn1)
        )
        updated( v2 )
      }


    // Сообщение о том, что завершён эффект открытия веб-сокета
    case m: WsOpenedConn =>
      val v0 = value
      val key = m.target
      v0.conns.get( key ).fold {
        // Should never happen: было выполнено открытие закрытого/неизвестного коннекшена.
        LOG.warn( WarnMsgs.UNKNOWN_CONNECTION, msg = m )
        for (ws <- m.ws) {
          _safeCloseWs( ws )
        }
        noChange

      } { existConn =>
        val v0 = value
        m.ws.fold(
          {ex =>
            LOG.error( ErrorMsgs.CONNECTION_ERROR, ex, m )
            val conn2 = existConn.withConn(
              existConn.conn.fail( ex )
            )
            val v2 = v0.withConns(
              v0.conns.updated(m.target, conn2)
            )
            updated(v2)
          },
          {ws =>
            try {
              // Подписаться на сообщения из сокета.
              ws.addEventListener4s(DomEvents.MESSAGE) { e: MessageEvent =>
                dispatcher(WsRawMsg(key, e.data))
              }
              // Желательно узнать о незапланированном закрытии сокета.
              ws.addEventListener4s(DomEvents.CLOSE) { _: CloseEvent =>
                dispatcher(WsCloseConn(key))
              }
              // Подписываемся на события ошибок сокета.
              ws.addEventListener4s(DomEvents.ERROR) { e: ErrorEvent =>
                LOG.error(ErrorMsgs.CONNECTION_ERROR, msg = (e.filename, e.lineno, e.colno, e.message))
                dispatcher(WsError(key, e.message))
              }

              // Сохранить готовый сокет в состояние.
              val conn2 = existConn
                .withConn(
                  existConn.conn
                    .ready(ws)
                )
                .withCloseTimer(
                  _closeTimerOpt(key, m.closeAfterSec)
                )
              val v2 = v0.withConns(
                v0.conns.updated(m.target, conn2)
              )
              updated(v2)

            } catch {
              case ex: Throwable =>
                // Не удалось обработать WebSocket, что-то пошло не так...
                LOG.error( ErrorMsgs.WEB_SOCKET_OPEN_FAILED, ex = ex, msg = ws.url )
                // В фоне запускаем callback обработки ошибки открытия сокета, если он задан.
                //for (onOpenErrorF <- m.onOpenErrorF) {
                //  Future {
                //    onOpenErrorF( ex )
                //  }
                //}
                // Всё, больше тут делать нечего.
                noChange
            }
          }
        )
      }


    // Сигнал (авто)закрытия какого-то коннекшена:
    case m: WsCloseConn =>
      val v0 = value
      println( m, v0 )
      v0.conns.get(m.target).fold {
        // Нет искомого коннекшена в состоянии.
        LOG.log( ErrorMsgs.NODE_NOT_FOUND, msg = m.target )
        noChange

      } { existConn =>

        // Закрыть существующий ws-коннекшен:
        _safeCloseConn( existConn )

        // Удалить данные по коннекшену из состояния:
        val v2 = v0.withConns(
          v0.conns - m.target
        )
        updated(v2)
      }

    // Команда к закрытию всех коннекшенов. Полезно при закрытии/уходе с текущей страницы браузера (window beforeunload).
    case WsCloseAll =>
      val v0 = value
      v0.conns
        .valuesIterator
        .foreach(_safeCloseConn)
      val v2 = v0.withConns(Map.empty)
      updated(v2)

  }


  /** Сборка таймера авто-закрытия указанного ws-коннекшена через указанное время (в секундах). */
  private def _closeTimerOpt(key: MWsConnTg, closeAfterSecOpt: Option[Int]): Option[Int] = {
    for (closeAfterSeconds <- closeAfterSecOpt) yield {
      DomQuick.setTimeout( closeAfterSeconds * 1000 ) { () =>
        dispatcher( WsCloseConn(key) )
      }
    }
  }

  private def _safeCloseConn(connS: MWsConnS): Unit = {
    for (ws <- connS.conn) {
      // Отменить autoclose-таймер, если есть:
      for (timerId <- connS.closeTimer) {
        DomQuick.clearTimeout( timerId )
      }

      _safeCloseWs(ws)
    }
  }

  private def _safeCloseWs(ws: WebSocket): Unit = {
    try {
      // Обнулить onclose, чтобы ws не слал сообщений о своём очевидном закрытии.
      ws.onclose = null
      ws.close()
    } catch {
      case ex: Throwable =>
        LOG.warn( WarnMsgs.CANNOT_CLOSE_SOMETHING, ex, msg = ws.url )
    }
  }

}
