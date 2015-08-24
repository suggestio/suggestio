package io.suggest.sc.sjs.c.scfsm.init

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfsm.signals.{JsRouterReady, JsRouterFailed}
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.util.{Success, Failure}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 17:51
 * Description: Аддон для ScFsm для крупноузловой сборки состояний, связанных с инициализацией jsrouter'а.
 */
trait JsRouterInit extends ScFsmStub {

  /** Аддон с запуском инициализией jsrouter'а при инициализации состояния. */
  protected trait JsRouterInitStartStateT extends FsmState {
    
    override def afterBecome(): Unit = {
      super.afterBecome()
      val jsRouterFut = SrvRouter.getRouter
      jsRouterFut onComplete { case tryRes =>
        val msg = tryRes match {
          case _: Success[_]  => JsRouterReady
          case Failure(ex)    => JsRouterFailed(ex)
        }
        _sendEvent(msg)
      }
    }

  }


  /** Аддон состояний для поддержки получения и абстрактного реагирования на сигналы
    * асинхронной инициализации js-роутера. */
  protected trait JsRouterInitReceiveT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Роутер инициализирован успешно. Можно переходить к дальнейшей инициализации.
      case JsRouterReady =>
        become( _jsRouterReadyState )
      // Инициализация роутера не удалась. Нужно подождать и попробовать ещё раз.
      case JsRouterFailed(ex) =>
        _jsRouterInitFailed(ex)
    }
    
    /** Состояние, когда инициализация js-роутера успешно завершена. */
    def _jsRouterReadyState: FsmState
    
    /** Реакция в случае инициализация js-роутера не удалась. */
    def _jsRouterInitFailed(ex: Throwable): Unit = {
      error("JsRouter init failed. Retrying...", ex)
      _retry(250)(_reInitState)
    }

    /** Ошибка инициализации jsRouter'а является фатальной, поэтому надо попробовать ещё раз. */
    protected def _reInitState: FsmState
    
  }

}
