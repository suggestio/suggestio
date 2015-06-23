package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.magent.MScreen
import io.suggest.sc.sjs.m.magent.vsz.ViewportSz
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sjs.common.util.ISjsLogger
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 17:25
 * Description: Поддержка инициализации выдачи.
 */
trait EarlyFsm extends ScFsmStub with ISjsLogger {

  /** Начальное состояние выдачи. Здесь начинается работа [[ScFsm]]. */
  protected trait InitStateT extends FsmEmptyReceiverState {
    protected def _earlyInit(): SD = {
      // Инициализировать синхронные модели, без которых нельзя продолжать инициализацию.
      _stateData.copy(
        screen = ViewportSz.getViewportSize.map( MScreen.apply )
      )
    }

    // Надо проинициализировать FSM и выставить следующее рабочее состояние.
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить инициализацию js-роутера.
      val jsRouterFut = SrvRouter.getRouter

      // Прочая асинхронная инициализация: дождаться её.
      val sd0 = _earlyInit()

      become( _jsRouterState(jsRouterFut), sd0 )
    }

    def _jsRouterState(jsRouterFut: Future[_]): FsmState
  }


  /** Состояние самой первой инициализации. Тут помимо обычной инициализации, ещё происходит
    * out-of-FSM инициализация, т.е. инициалиция каких-то связанных глобальных вещей (document, window, etc). */
  protected trait FirstInitStateT extends InitStateT {
    override protected def _earlyInit(): SD = {
      val sd0 = super._earlyInit()
      // TODO Подписаться на различные глобальные события, если ещё не подписаны.
      DocumentView.initDocEvents()
      sd0
    }
  }


  /** Абстрактная логика состояния ожидания js-роутера. */
  protected trait AwaitJsRouterStateT extends FsmState {
    def jsRouterFut: Future[_]

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить listener на js router future.
      jsRouterFut onComplete { case res =>
        _sendEventSync(res)
      }
    }

    override def receiverPart: Receive = {
      // Роутер инициализирован успешно. Можно переходить к дальнейшей инициализации.
      case s: Success[_] =>
        finished()
      // Инициализация роутера не удалась. Нужно подождать и попробовать ещё раз.
      case Failure(ex) =>
        failed(ex)
    }

    /** Инициализация роуетра успешно завершена. */
    def finished(): Unit
    /** Инициализация роутера не удалась. */
    def failed(ex: Throwable): Unit
  }

}
