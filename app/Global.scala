import akka.actor.Cancellable
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import io.suggest.model.EsModel
import org.elasticsearch.client.Client
import play.api.mvc.{Result, WithFilters, RequestHeader}
import util.captcha.CipherUtil
import scala.concurrent.{Await, Future, future}
import scala.util.{Failure, Success}
import util.jmx.JMXImpl
import util.{HtmlCompressUtil, Crontab, SiowebEsUtil, SiowebSup}
import play.api.Play._
import play.api._
import scala.concurrent.duration._
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 11:06
 * Description: Global модуль приложения используется для инициализации и остановки проекта, централизованной
 * обработки ошибок и т.д.
 * http://www.playframework.com/documentation/2.1.0/ScalaGlobal
 */

object Global extends WithFilters(SioHTMLCompressorFilter()) {

  // Логгеры тут работают через вызов Logger.*
  import Logger._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit private def sioNotifier = util.event.SiowebNotifier

  private var cronTimers : List[Cancellable] = null

  /**
   * При запуске нужно все перечисленные действия.
   * @param app Экземпляр класса Application.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    SiowebSup.startLink
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    val fut = future {
      SiowebEsUtil.ensureNode()
    } map {
      _.client()
    } flatMap { implicit esClient =>
      initializeEsModels map { _ => esClient }
    } flatMap { implicit esClient =>
      resetSuperuserIds map { _ => esClient }
    }
    JMXImpl.registerAll()
    // Блокируемся, чтобы не было ошибок в браузере и консоли из-за асинхронной работы с ещё не запущенной системой.
    val startTimeout: FiniteDuration = (app.configuration.getInt("start.timeout_sec") getOrElse 32).seconds
    CipherUtil.ensureBcJce()
    Await.ready(fut, startTimeout)
    synchronized {
      cronTimers = Crontab.startTimers
    }
  }



  /** Проинициализировать все ES-модели и основной индекс. */
  def initializeEsModels(implicit client: Client): Future[_] = {
    val futInx = EsModel.ensureEsModelsIndices
    val logPrefix = "initializeEsModels(): "
    futInx onComplete {
      case Success(result) => debug(logPrefix + "ensure() -> " + result)
      case Failure(ex)     => error(logPrefix + "ensureIndex() failed", ex)
    }
    val futMappings = futInx flatMap { _ =>
      SiowebEsModel.putAllMappings
    }
    futMappings onComplete {
      case Success(_)  => info(logPrefix + "Finishied successfully.")
      case Failure(ex) => error(logPrefix + "Failure", ex)
    }
    futMappings
  }


  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    super.onStop(app)
    JMXImpl.unregisterAll()
    // Остановить таймеры
    synchronized {
      Crontab.stopTimers(cronTimers)
      cronTimers = null
    }
    // При девелопменте: ES-клиент сам по себе не остановится, поэтому нужно его грохать вручную, иначе будет куча инстансов.
    SiowebEsUtil.stopNode()
  }


  /** Вызов страницы 404. В продакшене надо выводить специальную страницу 404. */
  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    // TODO логгер тут не работает почему-то...
    trace(request.path + " - 404")
    maybeApplication match {
      case Some(app) if app.mode == Mode.Prod =>
        _root_.controllers.Application.http404Fut(request)

      // При разработке следует выводить нормальное 404.
      case _ => super.onHandlerNotFound(request)
    }
  }

  /** Выставить в MPerson id'шники суперпользователей. Для этого надо убедится, что все админские MPersonIdent'ы
    * существуют. Затем, сохранить в глобальную переменную в MPerson этот списочек. */
  def resetSuperuserIds(implicit client: Client): Future[_] = {
    import _root_.models._
    val logPrefix = "resetSuperuserIds(): "
    Future.traverse(MPersonIdent.SU_EMAILS) { email =>
      EmailPwIdent.getById(email) flatMap {
        // Суперюзер ещё не сделан. Создать MPerson и MPI для текущего email.
        case None =>
          val logPrefix1 = s"$logPrefix[$email] "
          info(logPrefix1 + "Installing new sio superuser...")
          MPerson(lang = "ru").save.flatMap { personId =>
            val pwHash = MPersonIdent.mkHash(email)
            EmailPwIdent(email=email, personId=personId, pwHash = pwHash).save.map { mpiId =>
              info(logPrefix1 + s"New superuser installed as $personId. mpi=$mpiId")
              personId
            }
          }

        // Суперюзер уже существует. Просто возвращаем его id.
        case Some(mpi) => Future successful mpi.personId
      }
    } andThen {
      case Success(suPersonIds) =>
        MPerson.setSuIds(suPersonIds.toSet)
        info(logPrefix + suPersonIds.length + " superusers installed successfully")

      case Failure(ex) =>
        error(logPrefix + "Failed to install superusers", ex)
    }
  }

}


/**
 * Defines a user-defined HTML compressor filter.
 */
object SioHTMLCompressorFilter {

  /**
   * Сборка сжимающего html-фильтра.
   * @return The HTML compressor filter.
   */
  def apply() = new HTMLCompressorFilter(HtmlCompressUtil.getForGlobalUsing)
}

