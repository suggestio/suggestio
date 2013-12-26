import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.{Future, future}
import util.{SiowebEsUtil, SiowebSup}
import play.api.Play._
import play.api._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */

object Global extends GlobalSettings {

  /**
   * При запуске нужно все перечисленные действия.
   * @param app Экземпляр класса Application.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.startLink
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    import scala.concurrent.ExecutionContext.Implicits._
    future { SiowebEsUtil.ensureNode() }
  }


  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    super.onStop(app)
    // При девелопменте: ES-клиент сам по себе не остановится, поэтому нужно его грохать вручную, иначе будет куча инстансов.
    SiowebEsUtil.stopNode()
  }


  /**
   * Вызов страницы 404. В продакшене надо выводить специальную страницу 404.
   */
  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    // TODO логгер тут не работает почему-то...
    println(request.path + " - 404")
    maybeApplication match {
      case Some(app) if app.mode == Mode.Prod => controllers.Application.http404Fut(request)
      // При разработке следует выводить нормальное 404.
      case _ => super.onHandlerNotFound(request)
    }
  }

}

