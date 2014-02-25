import akka.actor.Cancellable
import io.suggest.model.EsModel
import org.elasticsearch.client.Client
import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.{Future, future}
import scala.util.{Failure, Success}
import util.{Crontab, SiowebEsUtil, SiowebSup}
import play.api.Play._
import play.api._
import models._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */

object Global extends GlobalSettings {

  // Логгеры тут работают через вызов Logger.*
  import Logger._

  import scala.concurrent.ExecutionContext.Implicits.global

  private var cronTimers : List[Cancellable] = null

  /**
   * При запуске нужно все перечисленные действия.
   * @param app Экземпляр класса Application.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.startLink
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    future {
      SiowebEsUtil.ensureNode()
    } onSuccess { case esNode =>
      initializeEsModels(esNode.client)
    }
    cronTimers = Crontab.startTimers
  }



  /** Проинициализировать все ES-модели и основной индекс. */
  def initializeEsModels(implicit client: Client): Future[_] = {
    val futInx = EsModel.ensureSioIndex
    futInx onComplete {
      case Success(result) => debug("ensureIndex() -> " + result)
      case Failure(ex)     => error("ensureIndex() failed", ex)
    }
    val futMappings = futInx flatMap { _ =>
      info("Index do not have mappings for type=" + MShopPromoOffer.ES_TYPE_NAME)
      EsModel.putAllMappings
    }
    futMappings onComplete {
      case tryResult => debug("Set index mappings completed with " + tryResult)
    }
    futMappings
  }


  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    super.onStop(app)
    // Остановить таймеры
    Crontab.stopTimers(cronTimers)
    cronTimers = null
    // При девелопменте: ES-клиент сам по себе не остановится, поэтому нужно его грохать вручную, иначе будет куча инстансов.
    SiowebEsUtil.stopNode()
  }


  /** Вызов страницы 404. В продакшене надо выводить специальную страницу 404. */
  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    // TODO логгер тут не работает почему-то...
    println(request.path + " - 404")
    maybeApplication match {
      case Some(app) if app.mode == Mode.Prod => _root_.controllers.Application.http404Fut(request)
      // При разработке следует выводить нормальное 404.
      case _ => super.onHandlerNotFound(request)
    }
  }

}

