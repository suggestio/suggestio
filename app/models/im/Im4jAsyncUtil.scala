package models.im

import org.im4java.process.{ProcessEvent, ProcessEventListener}

import scala.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.14 17:26
 * Description: Утиль для упрощения асинхронной работы с im4java.
 */

/** Трейт, содранный с org.im4java.test.TestCase16. */
trait Im4jAsyncProcessListenerT extends ProcessEventListener {

  protected var _iProcess: Option[Process] = None
  protected var _isTerminated: Boolean = false

  protected lazy val resPromise = Promise[Int]()

  def future = resPromise.future

  protected def starting(pEvent: ProcessEvent): Unit = {
    _isTerminated = true
    _iProcess = Option(pEvent.getProcess)
  }

  override def processInitiated(pEvent: ProcessEvent): Unit = {
    _isTerminated = false
  }

  override def processStarted(pEvent: ProcessEvent): Unit = {
    starting(pEvent)
  }

  override def processTerminated(pEvent: ProcessEvent): Unit = {
    _iProcess.synchronized {
      _iProcess = None
    }
    _isTerminated = true
    pEvent.getException match {
      case null =>
        resPromise success pEvent.getReturnCode

      case ex =>
        resPromise failure ex
    }
  }

  /** Принудительно разрушить процесс. В processTerminated() придёт экзепшен. */
  def destroy: Unit = {
    _iProcess.foreach { iproc =>
      try {
        _iProcess.synchronized {
          iproc.destroy()
        }
      } catch {
        case ex: Exception => // do nothing
      }
    }
  }

  def isRunning = !_isTerminated

}

/** Листенер, который возвращает успешный фьючерс с любым кодом возврата. */
class Im4jAsyncProcessListener extends Im4jAsyncProcessListenerT {
  override lazy val future = super.future
}

/** Listener, который возвращает success-future только если результат выполнения команды == 0. */
class Im4jAsyncSuccessProcessListener extends Im4jAsyncProcessListenerT {
  override lazy val future = {
    super.future
      .filter { _ == 0 }
  }
}
