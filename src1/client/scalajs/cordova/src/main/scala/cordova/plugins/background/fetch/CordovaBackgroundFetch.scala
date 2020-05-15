package cordova.plugins.background.fetch

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js
import io.suggest.err.ToThrowableJs._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 11:37
  * Description: Основной интерфейс для bg-fetch-плагина.
  *
  * @see [[https://github.com/transistorsoft/cordova-plugin-background-fetch]]
  */
@js.native
trait CordovaBackgroundFetch extends js.Object {

  /** configure() method automatically calls start(). */
  def configure(callback: js.Function1[String, Unit],
                error: js.Function1[js.Any, Unit],
                config: CbfConfig,
                ): Unit = js.native

  def scheduleTask(task: CbfConfig,
                   success: js.Function0[Unit] = js.native,
                   failure: js.Function1[js.Any, Unit] = js.native,
                  ): Unit = js.native

  def stopTask(taskId: String,
               success: js.Function0[Unit] = js.native,
               failure: js.Function1[js.Any, Unit] = js.native,
              ): Unit = js.native

  def finish(taskId: String,
             success: js.Function0[Unit] = js.native,
             failure: js.Function1[js.Any, Unit],
            ): Unit = js.native

  /** configure() method automatically calls start(). */
  def start(success: js.Function0[Unit] = js.native,
            failure: js.Function1[js.Any, Unit] = js.native,
           ): Unit = js.native

  def stop(success: js.Function0[Unit] = js.native,
           failure: js.Function1[js.Any, Unit] = js.native,
          ): Unit = js.native

  def status(success: js.Function1[CbfStatus_t, Unit],
             failure: js.Function1[js.Any, Unit],
            ): Unit = js.native


  val NETWORK_TYPE_NONE, NETWORK_TYPE_ANY, NETWORK_TYPE_CELLULAR, NETWORK_TYPE_UNMETERED,
      NETWORK_TYPE_NOT_ROAMING: CbfNetworkType_t = js.native

  val STATUS_RESTRICTED, STATUS_DENIED, STATUS_AVAILABLE: CbfStatus_t = js.native

  val FETCH_RESULT_NEW_DATA, FETCH_RESULT_NO_DATA, FETCH_RESULT_FAILED: CbfFetchResult_t = js.native

}


object CordovaBackgroundFetch {

  implicit final class CBgFetchOpsExt( private val cbf: CordovaBackgroundFetch ) extends AnyVal {

    def scheduleTaskF(task: CbfConfig): Future[_] =
      JsApiUtil.call0ErrFut[js.Any]( cbf.scheduleTask(task, _, _) )

    def stopTaskF(taskId: String): Future[_] =
      JsApiUtil.call0ErrFut[js.Any]( cbf.stopTask(taskId, _, _) )

    def finishF(taskId: String): Future[_] =
      JsApiUtil.call0ErrFut[js.Any]( cbf.finish(taskId, _, _) )

    def startF(): Future[_] =
      JsApiUtil.call0ErrFut[js.Any]( cbf.start(_, _) )

    def stopF(): Future[_] =
      JsApiUtil.call0ErrFut[js.Any]( cbf.stop(_, _) )

    def statusF(): Future[CbfStatus_t] =
      JsApiUtil.call1ErrFut[CbfStatus_t, js.Any]( cbf.status(_, _) )

  }

}
