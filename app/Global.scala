import play.api._
import util.SiowebSup

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */

object Global extends GlobalSettings{

  /**
   * При запуске нужно
   * @param app
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.ensureStarted
  }

}
