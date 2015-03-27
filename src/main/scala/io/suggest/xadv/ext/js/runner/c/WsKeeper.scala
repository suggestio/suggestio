package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m._
import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, WebSocket, console}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:02
 * Description: Обслуживание web-socket'a. Открытие, получение и отправка данных.
 * Работает по принципу статического актора.
 */
// FIXME 2015.mar.27 Этот модуль по названию относится только к ws, хотя по сути в нем вся логика приложения. Это неправильно.
trait WsKeeper {

  /** Запуск вебсокета. */
  protected[this] def startWs(adapters: List[IAdapter]): Unit = {
    val wsEl = dom.document.getElementById("socialApiConnection")
    val attr = "value"
    val wsUrl = wsEl.getAttribute(attr)
    val ws = new WebSocket(wsUrl)
    val appState = MAppState(
      ws        = ws,
      adapters  = adapters
    )
    // TODO Делать реконнект при проблеме со связью.
    // TODO Закрывать ws при закрытии текущей страницы.
    ws.onmessage = handleMessage(_: MessageEvent, appState)
    // Ссылка больше не нужна. Удалить её из верстки, в т.ч. в целях безопасности.
    wsEl.removeAttribute(attr)
  }

  /**
   * Асинхронная обработка сообщений от sio.
   * @param msg Входящее сообщение.
   */
  private[this] def handleMessage(msg: MessageEvent, appState: MAppState): Future[_] = {
    // Форсируем асинхронное исполнение, чтобы можно было унифицировать обработку ошибок.
    // Десериализация полученной команды:
    val cmdFut = Future {
      MJsCommand.fromString(msg.data.toString)
    }
    // Логгинг результата десериализации
    cmdFut onComplete {
      case Success(cmd) =>
        dom.console.info("Cmd received: " + cmd)
      case Failure(ex) =>
        dom.console.error("Failed to deserialize cmd msg " + msg.data + ": " + ex.getClass.getName + " " + ex.getMessage)
    }

    // В зависимости от полученной команды принять решение какое-то.
    cmdFut flatMap {
      // Это js. Нужно запустить его на исполнение.
      case cmd: MJsCommand =>
        Future {
          js.eval(cmd.jsCode)
        }

      // Вызов ensure ready. data содержит строку с MJsCtx внутри.
      case cmd: MActionCmd =>
        val fut = AdaptersSupport.handleAction(cmd.mctx, appState.adapters)
        // Если успех, то логгируем результат.
        fut onSuccess { case mctx2 =>
          console.info("Action finished. New ctx = " + mctx2)
        }
        // Если ошибка, то берём исходный контекст.
        fut recover { case ex =>
          console.error("Action failed: " + cmd)
          cmd.mctx.copy(
            status = Some(MAnswerStatuses.Error),
            error = Some {
              ex match {
                case ei: MErrorInfoT  => ei
                case _                => MErrorInfo("error.sio.internal", Seq(ex.getClass.getName, ex.getMessage))
              }
            }
          )

        } foreach { mctx2 =>
          // Отправить новый контекст серверу через ws.
          val ans = MAnswer(
            replyTo = cmd.replyTo,
            mctx    = mctx2
          )
          val json = JSON.stringify(ans.toJson)
          appState.ws.send(json)
        }
        fut
    }
  }

}
