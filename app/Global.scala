import util.{ContextT, SiowebSup}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */
import play.api._
import play.api.mvc._
import play.api.mvc.Results._


object Global extends GlobalSettings with ContextT {

  /**
   * При запуске нужно
   * @param app
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.ensureStarted
  }


  /**
   *Вызов страницы 404


   override def onHandlerNotFound(request: RequestHeader): Result = {

    super.onHandlerNotFound(request)
     NotFound(
       views.html.static.http404Tpl()
     )
  }
  */
}

