package io.suggest.xadv.ext.js.runner.c

import io.suggest.common.ws.proto.MAnswerStatuses
import io.suggest.sjs.common.view.CommonPage
import io.suggest.xadv.ext.js.runner.m._
import io.suggest.xadv.ext.js.runner.vm.WsUrl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, WebSocket, console}

import scala.concurrent.Future
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
    val wsUrlVm = WsUrl.find().get
    val ws = new WebSocket( wsUrlVm.wsUrl )
    val appState = MAppState(
      ws        = ws,
      adapters  = adapters
    )
    ws.onmessage = handleMessage(_: MessageEvent, appState)
    // TODO Делать реконнект при проблеме со связью. Но сначала нужна серверная поддержка восстановления состояния.
    //      Firefox < 27 рвал ws-коннекшены по клавише ESC.
    // Закрывать сокет при закрытии текущей страницы.
    // TODO Закрывать сокет через обращение к appState, чтобы избежать возможных проблем в будущем с заменой экземпляров ws в состоянии.
    CommonPage.onClose( () => ws.close() )
    // Ссылка больше не нужна. Удалить её из верстки, в т.ч. в целях безопасности.
    wsUrlVm.remove()
  }

  /**
   * Асинхронная обработка сообщений от sio.
   * @param msg Входящее сообщение.
   */
  private[this] def handleMessage(msg: MessageEvent, appState: MAppState): Unit = {
    // Форсируем асинхронное исполнение, чтобы можно было унифицировать обработку ошибок.
    // Десериализация полученной команды:
    val cmdFut = Future {
      IJsCmd.fromString(msg.data.toString)
    }
    // Логгинг результата десериализации
    cmdFut onComplete {
      case Success(cmd) =>
        dom.console.info("Cmd received: " + cmd)
      case Failure(ex) =>
        dom.console.error("Failed to deserialize cmd msg " + msg.data + ": " + ex.getClass.getName + " " + ex.getMessage)
    }

    // В зависимости от полученной команды принять решение какое-то.
    cmdFut.foreach {
      // Это js. Нужно запустить его на исполнение.
      case cmd: MJsCommand =>
        val fn = { () =>
          Future {
            js.eval(cmd.jsCode)
          }
        }
        if (cmd.isPopup)
          appState.appContext.popupQueue.enqueue(fn)
        else
          fn()

      // Вызов ensure ready. data содержит строку с MJsCtx внутри.
      case cmd: MActionCmd =>
        val fut = AdaptersSupport.handleAction(cmd.mctx, appState)
        // Если успех, то логгируем результат.
        fut.foreach { mctx2 =>
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
          if (mctx2 != null) {
            // Отправить новый контекст серверу через ws.
            val ans = MAnswerCtx(
              replyTo = cmd.replyTo,
              mctx = mctx2
            )
            sendAnswer(ans, appState)
          }
        }

    }   // onSuccess
  }     // handleMessage


  /** Укороченная в плане кода отправка ответа на sio-сервер. */
  private def sendAnswer(ans: IAnswer, appState: MAppState): Unit = {
    val json = JSON.stringify(ans.toJson)
    appState.ws.send(json)
  }

}
