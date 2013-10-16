import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.Future
import util.{ContextT, SiowebSup}
import play.api.Play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */
import play.api._

object Global extends GlobalSettings {

  /**
   * При запуске нужно
   * @param app
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.ensureStarted
  }


  override def onStop(app: Application) {
    super.onStop(app)
  }


  /**
   * Вызов страницы 404. В продакшене надо выводить специальную страницу 404.
   */
  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    maybeApplication match {
      case Some(app) if app.mode == Mode.Prod => controllers.Application.http404Fut(request)
      // При разработке следует выводить нормальное 404.
      case _ => super.onHandlerNotFound(request)
    }
  }

}

