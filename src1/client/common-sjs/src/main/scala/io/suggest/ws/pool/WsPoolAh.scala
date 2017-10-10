package io.suggest.ws.pool

import diode.data.Ready
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.event.DomEvents
import io.suggest.primo.MConflictRes
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.ws.pool.m._
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent}
import org.scalajs.dom.raw.WebSocket
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import io.suggest.url.MHostUrl
import org.scalajs.dom

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 18:05
  * Description: Action handler, поддерживающий собственный пул непостоянных websocket-коннекшенов.
  * Возник из-за необходимости поддерживать недолгие ws-связи с произвольными серверами.
  */
class WsPoolAh[M](
                   modelRW      : ModelRW[M, MWsPoolS],
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


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сообщение из открытого сокета.
    case m: WsMsg =>
      val v0 = value
      v0.conns.get( m.from ).fold {
        // Внезапно, не найден коннекшен.
        LOG.warn( WarnMsgs.UNKNOWN_CONNECTION, msg = m )
        noChange

      } { existConn =>
        // Есть коннекшен, значит есть и callback-функция.
        try {
          val fxOpt = existConn.onMessageF(m.payload)
          fxOpt.fold(noChange)(effectOnly)
        } catch {
          case ex: Throwable =>
            LOG.error( ErrorMsgs.CALLBACK_FUNCTION_FAILED, ex, m )
            noChange
        }
      }


    // Поступил запрос на открытие ws-коннекшена.
    case m: WsEnsureConn =>
      val v0 = value
      val key = m.hostUrl
      v0.conns.get( key ).fold {
        // Нет коннекшена с данными координатами. Просто открываем новый ws-коннекшен:
        val wsProto = HttpConst.Proto.wsOrWss( Xhr.PREFER_SECURE_URLS )
        val wsUrl = wsProto + HttpConst.Proto.DELIM + m.hostUrl.host + m.hostUrl.relUrl
        try {
          val ws = new WebSocket(wsUrl)

          // Подписаться на сообщения из сокета.
          ws.addEventListener4s(DomEvents.MESSAGE) { e: MessageEvent =>
            dispatcher( WsMsg(key, e.data) )
          }

          // Желательно узнать о незапланированном закрытии сокета.
          ws.addEventListener4s(DomEvents.CLOSE) { _: CloseEvent =>
            dispatcher( WsCloseConn(key) )
          }

          // Подписываемся на события ошибок сокета.
          ws.addEventListener4s(DomEvents.ERROR) { e: ErrorEvent =>
            LOG.error( ErrorMsgs.CONNECTION_ERROR, msg = (e.filename, e.lineno, e.colno, e.message))
            dispatcher( WsError(key, e.message) )
          }

          // Собрать состояние WS-коннекшена
          val wsConnS = MWsConnS(
            hostUrl     = key,
            onMessageF  = m.callbackF,
            conn        = Ready( ws ),
            closeTimer  = _closeTimerOpt(key, m.closeAfterSec)
          )

          // Сохранить данные о коннекшене в состояние:
          val v2 = v0.withConns(
            v0.conns + (key -> wsConnS)
          )
          updated( v2 )

        } catch {
          case ex: Throwable =>
            // Не удалось открыть WebSocket или что-то ещё пошло не так.
            LOG.error( ErrorMsgs.WEB_SOCKET_OPEN_FAILED, ex = ex, msg = wsUrl )

            // В фоне запускаем callback обработки ошибки открытия сокета, если он задан.
            for (onOpenErrorF <- m.onOpenErrorF) {
              Future {
                onOpenErrorF( ex )
              }
            }

            // Всё, больше тут делать нечего.
            noChange
        }

      } { existConn =>
        // Запрошенный коннекшен уже существует.
        // обновляем таймер автозакрытия коннекшена на новое значение:
        val conn1 = existConn.withCloseTimer(
          _closeTimerOpt(key, m.closeAfterSec)
        )

        // Обновляем callbackF согласно указанной в m политике разрешения конфликтов:
        val conn2 = m.conflictRes match {
          case MConflictRes.ReplaceOld => conn1.withOnMessageF( m.callbackF )
          case MConflictRes.IgnoreNew  => conn1
          case MConflictRes.Merge      =>
            val oldCb = conn1.onMessageF
            conn1.withOnMessageF { data =>
              val fxs = oldCb(data) ++ m.callbackF(data)
              if (fxs.isEmpty) None
              else if (fxs.size == 1) Some( fxs.head )
              else Some( fxs.reduceLeft(_ + _) )
            }
        }

        // Сохранить новое состояние коннекшена в состояние:
        val v2 = v0.withConns(
          v0.conns + (key -> conn2)
        )
        updated( v2 )
      }


    // Сигнал (авто)закрытия какого-то коннекшена:
    case m: WsCloseConn =>
      val v0 = value
      v0.conns.get(m.hostUrl).fold {
        // Нет искомого коннекшена в состоянии.
        LOG.log( ErrorMsgs.NODE_NOT_FOUND, msg = m.hostUrl )
        noChange

      } { existConn =>

        // Закрыть существующий ws-коннекшен:
        _safeCloseConn( existConn )

        // Удалить данные по коннекшену из состояния:
        val v2 = v0.withConns(
          v0.conns - m.hostUrl
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
  private def _closeTimerOpt(key: MHostUrl, closeAfterSecOpt: Option[Int]): Option[Int] = {
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

      try {
        // Обнулить onclose, чтобы ws не слал сообщений о своём очевидном закрытии.
        ws.onclose = null
        ws.close()
      } catch {
        case ex: Throwable =>
          LOG.warn( WarnMsgs.CANNOT_CLOSE_SOMETHING, ex, msg = connS.hostUrl )
      }
    }
  }

}
