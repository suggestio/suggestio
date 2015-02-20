package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, MCommandTypes, MJsCommand, MJsCtx}
import org.scalajs.dom.{MessageEvent, WebSocket, console}
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
trait WsKeeper {

  /** Вебсокет. Скрыт от посторонних глаз. */
  private[this] var _ws: WebSocket = null

  /**
   * Запуск вебсокета.
   * @param wsUrl ссылка для связи с сервером.
   */
  protected[this] def startWs(wsUrl: String): Unit = {
    val ws = new WebSocket(wsUrl)
    _ws = ws
    ws.onmessage = handleMessage(_: MessageEvent)
    // TODO Делать реконнект при проблеме со связью.
  }

  /**
   * Обработка сообщений от sio.
   * @param msg Входящее сообщение.
   */
  private[this] def handleMessage(msg: MessageEvent): Unit = {
    // Десериализация
    val cmd = MJsCommand.fromString(msg.data.toString)
    cmd.ctype match {
      // Это js. Нужно запустить его на исполнение.
      case MCommandTypes.JavaScript =>
        js.eval( cmd.data )

      // Вызов ensure ready. data содержит строку с MJsCtx внутри.
      case MCommandTypes.Action =>
        val mctx = MJsCtx.fromString(cmd.data)
        AdaptersSupport.handleAction(mctx) onComplete {
          // Успешное завершение действия
          case Success(mctx1) =>
            val json = JSON.stringify(mctx1.toJson)
            console.info("Action finished: " + json)
            _ws.send(json)

          // Асинхронный облом при исполнении запрошенного действия.
          case Failure(ex) =>
            val mctx2 = mctx.copy(
              status = Some(MAnswerStatuses.Error)    // TODO Отправлять ошибку
            )
            val json = JSON.stringify(mctx2.toJson)
            console.error("Action failed: " + json)
            _ws.send(json)
        }

      // Неизвестное сообщение.
      case other =>
        console.error("Unexpected ws-message received: " + cmd)
    }
  }

}
