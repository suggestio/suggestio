package io.suggest.os.notify.api.cnl

import cordova.Cordova
import io.suggest.common.empty.OptionUtil
import io.suggest.log.Log
import io.suggest.perm.BoolOptPermissionState
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.04.2020 17:38
  * Description: Утиль для CNL.
  */
object CordovaNotificationlLocalUtil extends Log {

  final def CNL = Cordova.plugins.notification.local

  def isCnlApiAvailable(): Boolean =
    CordovaLocalNotificationAh.circuitDebugInfoSafe().isSuccess


  /** Запрос пермишена.
    * @return Возврат данных о пермишшене в унифицированном формате.
    */
  def hasPermissionState(): Future[BoolOptPermissionState] = {
    for {
      res <- CNL.hasPermissionF()
    } yield {
      val resOpt = OptionUtil.SomeBool.orNone( res )
      BoolOptPermissionState( resOpt )
    }
  }

}
