package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.magent.{MScreen, MAgent}
import io.suggest.sc.sjs.m.magent.vsz.ViewportSz
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom
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

  protected def firstStart() {
    become(new FirstInitState)
  }


  /** Инициализация кеша с размерами экрана. */
  protected def initScrSz(): Unit = {
    val scrSz = ViewportSz.getViewportSize.get
    MAgent.availableScreen = MScreen(scrSz)
  }


  /** Начальное состояние выдачи. Здесь начинается работа [[ScFsm]]. */
  protected class InitState extends FsmState {
    protected def _initFinished(): Future[_] = {
      // Инициализировать синхронные модели, без которых нельзя продолжать инициализацию.
      initScrSz()

      Future successful None
    }

    // Надо проинициализировать FSM и выставить следующее рабочее состояние.
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить инициализацию js-роутера.
      val jsRouterFut = SrvRouter.getRouter

      // Прочая асинхронная инициализация: дождаться её.
      _initFinished() onComplete { case res =>
        val e = res match {
          case s: Success[_] =>
            jsRouterFut
          case failure =>
            failure
        }
        _sendEventSync(e)
      }
    }

    override def receiverPart: PartialFunction[Any, Unit] = {
      // Инициализацию можно считать законченной.
      case jsRouterFut: Future[_] =>
        become( new AwaitJsRouterState(jsRouterFut) )
      case Failure(ex) =>
        error("early init failed", ex)
    }
  }


  /** Состояние самой первой инициализации. Тут помимо обычной инициализации, ещё происходит
    * out-of-FSM инициализация, т.е. инициалиция каких-то связанных глобальных вещей (document, window, etc). */
  protected class FirstInitState extends InitState {
    override protected def _initFinished(): Future[_] = {
      val fut = super._initFinished()
      // TODO Подписаться на различные глобальные события, если ещё не подписаны.
      DocumentView.initDocEvents()
      fut
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


  /** Состояние начальной инициализации роутера. */
  protected class AwaitJsRouterState(val jsRouterFut: Future[_]) extends AwaitJsRouterStateT {

    override def finished(): Unit = {
      // TODO Надо переключиться на следующей состояние.
      ???
    }

    override def failed(ex: Throwable): Unit = {
      error("JsRouter init failed. Retrying...", ex)
      dom.window.setTimeout(
        { () => become(new InitState) },
        250
      )
    }
  }


}
