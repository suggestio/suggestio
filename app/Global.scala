import io.suggest.model.es.EsModelUtil
import io.suggest.util.SioEsUtil
import models.usr.MSuperUsers
import org.elasticsearch.client.Client
import org.elasticsearch.index.mapper.MapperException
import util.event.SiowebNotifier
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import util.jmx.JMXImpl
import util._
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

object Global extends GlobalSettings {

  // Логгеры тут работают через вызов Logger.*
  import Logger._
  import scala.concurrent.ExecutionContext.Implicits.global


  /**
   * При запуске нужно все перечисленные действия.
   * @param app Экземпляр класса Application.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    implicit val sioNotifier = _inject[SiowebNotifier](app)
    // Запускаем супервизора вместе с деревом остальных акторов.
    _inject[SiowebSup](app).startLink()

    // Запускать es-клиент при старте. Это нужно для следующих шагов инициализации.
    implicit val esClient = app.injector.instanceOf[Client]
    val fut = for {
      _ <- initializeEsModels(app)
      _ <- {
        // Если в конфиге явно не включена поддержка проверки суперюзеров в БД, то не делать этого.
        // Это также нужно было при миграции с MPerson на MNode, чтобы не произошло повторного создания новых
        // юзеров в MNode, при наличии уже существующих в MPerson.
        val ck = "start.ensure.superusers"
        val createIfMissing = app.configuration.getBoolean(ck).getOrElse(false)
        val mSuperUsers = _inject[MSuperUsers](app)
        val fut = mSuperUsers.resetSuperuserIds(createIfMissing)
        if (!createIfMissing)
          debug("Does not ensuring superusers in permanent models: " + ck + " != true")
        fut
      }
    } yield {
      None
    }

    jmxImpl(app).registerAll()
    // Блокируемся, чтобы не было ошибок в браузере и консоли из-за асинхронной работы с ещё не запущенной системой.
    val startTimeout = {
      app.configuration.getInt("start.timeout_sec")
        .getOrElse(32)
        .seconds
    }
    CipherUtil.ensureBcJce()
    Await.ready(fut, startTimeout)
  }


  /**
   * Проинициализировать все ES-модели и основной индекс.
   * @param triedIndexUpdate Флаг того, была ли уже попытка обновления индекса на последнюю версию.
   */
  def initializeEsModels(app: Application, triedIndexUpdate: Boolean = false)(implicit client: Client): Future[_] = {
    val _siowebEsModel = siowebEsModel(app)
    _siowebEsModel.maybeErrorIfIncorrectModels()
    val esModels = _siowebEsModel.ES_MODELS
    val futInx = EsModelUtil.ensureEsModelsIndices(esModels)
    val logPrefix = "initializeEsModels(): "
    futInx onComplete {
      case Success(result) => debug(logPrefix + "ensure() -> " + result)
      case Failure(ex)     => error(logPrefix + "ensureIndex() failed", ex)
    }
    val futMappings = futInx.flatMap { _ =>
      _siowebEsModel.putAllMappings(esModels)
    }
    futMappings.onComplete {
      case Success(_)  => info(logPrefix + "Finishied successfully.")
      case Failure(ex) => error(logPrefix + "Failure", ex)
    }
    // Это код обновления на следующую версию. Его можно держать и после обновления.
    futMappings.recoverWith {
      case ex: MapperException if !triedIndexUpdate =>
        info("Trying to update main index to v2.1 settings...")
        SioEsUtil.updateIndex2_1To2_2(EsModelUtil.DFLT_INDEX) flatMap { _ =>
          initializeEsModels(app, triedIndexUpdate = true)
        }
    }
    futMappings
  }

  private def _inject[T: ClassTag](app: Application) = app.injector.instanceOf[T]
  private def jmxImpl(app: Application)         = _inject[JMXImpl](app)
  private def siowebEsModel(app: Application)   = _inject[SiowebEsModel](app)

  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    // Была одна ошибка после проблемы в DI после onStart(). JMXImpl должен останавливаться перед elasticsearch.
    jmxImpl(app).unregisterAll()

    // В текущем потоке: Исполняем синхронные задачи завершения работы...
    super.onStop(app)
  }

}


