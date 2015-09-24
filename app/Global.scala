import akka.actor.{ActorSystem, Cancellable}
import io.suggest.model.{SioCassandraClient, EsModel}
import io.suggest.util.SioEsUtil
import models.usr.SuperUsers
import org.elasticsearch.client.Client
import org.elasticsearch.index.mapper.MapperException
import play.api.mvc.{Result, WithFilters, RequestHeader}
import util.cdn.{CorsFilter, DumpXffHeaders}
import util.event.SiowebNotifier
import util.radius.RadiusServerImpl
import util.secure.PgpUtil
import util.showcase.ScStatSaver
import util.xplay.{SioHttpErrorHandler, SecHeadersFilter}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import util.jmx.JMXImpl
import util._
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

object Global extends WithFilters(new HtmlCompressFilter, new DumpXffHeaders, SecHeadersFilter(), new CorsFilter) {

  // Логгеры тут работают через вызов Logger.*
  import Logger._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit private def sioNotifier = SiowebNotifier

  private var cronTimers : List[Cancellable] = null

  /**
   * При запуске нужно все перечисленные действия.
   * @param app Экземпляр класса Application.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    val esNodeFut = Future {
      SiowebEsUtil.ensureNode()
    }
    ensureScryptNoJni()
    // Запускаем супервизора
    val akka = app.injector.instanceOf[ActorSystem]
    SiowebSup.startLink(akka)
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    val esClientFut = esNodeFut map {
      _.client()
    }
    val fut = esClientFut flatMap { implicit esClient =>
      initializeEsModels() map { _ => esClient }
    } flatMap { implicit esClient =>
      // Если в конфиге явно не включена поддержка проверки суперюзеров в БД, то не делать этого.
      // Это также нужно было при миграции с MPerson на MNode, чтобы не произошло повторного создания новых
      // юзеров в MNode, при наличии уже существующих в MPerson.
      val ck = "start.ensure.superusers"
      val createIfMissing = app.configuration.getBoolean(ck).getOrElse(false)
      val fut = SuperUsers.resetSuperuserIds(createIfMissing)
      if (!createIfMissing)
        debug("Does not ensuring superusers in permanent models: " + ck + " != true")
      fut.map { _ => esClient }
    }

    // Инициализировать связку ключей, если необходимо.
    esClientFut onSuccess { case esClient =>
      PgpUtil.maybeInit()(esClient)
    }

    JMXImpl.registerAll()
    // Блокируемся, чтобы не было ошибок в браузере и консоли из-за асинхронной работы с ещё не запущенной системой.
    val startTimeout: FiniteDuration = (app.configuration.getInt("start.timeout_sec") getOrElse 32).seconds
    CipherUtil.ensureBcJce()
    Await.ready(fut, startTimeout)
    synchronized {
      cronTimers = Crontab.startTimers(app)
    }
    RadiusServerImpl.start(true, true)
  }


  /**
   * Проинициализировать все ES-модели и основной индекс.
   * @param triedIndexUpdate Флаг того, была ли уже попытка обновления индекса на последнюю версию.
   */
  def initializeEsModels(triedIndexUpdate: Boolean = false)(implicit client: Client): Future[_] = {
    val esModels = SiowebEsModel.ES_MODELS
    val futInx = EsModel.ensureEsModelsIndices(esModels)
    val logPrefix = "initializeEsModels(): "
    futInx onComplete {
      case Success(result) => debug(logPrefix + "ensure() -> " + result)
      case Failure(ex)     => error(logPrefix + "ensureIndex() failed", ex)
    }
    val futMappings = futInx flatMap { _ =>
      SiowebEsModel.putAllMappings(esModels)
    }
    futMappings onComplete {
      case Success(_)  => info(logPrefix + "Finishied successfully.")
      case Failure(ex) => error(logPrefix + "Failure", ex)
    }
    // Это код обновления на следующую версию. Его можно держать и после обновления.
    futMappings recoverWith {
      case ex: MapperException if !triedIndexUpdate =>
        info("Trying to update main index to v2.1 settings...")
        SioEsUtil.updateIndex2_1To2_2(EsModel.DFLT_INDEX) flatMap { _ =>
          initializeEsModels(triedIndexUpdate = true)
        }
    }
    futMappings
  }


  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    ScStatSaver.BACKEND.close()
    // Сразу в фоне запускаем отключение тяжелых клиентов к кластерных хранилищам:
    val casCloseFut = SioCassandraClient.close()
    // Была одна ошибка после проблемы в DI после onStart(). JMXImpl должен останавливаться перед elasticsearch.
    JMXImpl.unregisterAll()
    val esCloseFut = Future {
      SiowebEsUtil.stopNode()
    }

    // В текущем потоке: Исполняем синхронные задачи завершения работы...
    super.onStop(app)
    // Остановить таймеры
    synchronized {
      Crontab.stopTimers(cronTimers)
      cronTimers = null
    }
    // Останавливаем RADIUS-сервер.
    RadiusServerImpl.stop()

    // Дожидаемся завершения асинхронных задач.
    val closeFut = casCloseFut.flatMap(_ => esCloseFut)
    Await.ready(closeFut, 20.seconds)
  }


  /** Вызов страницы 404. В продакшене надо выводить специальную страницу 404. */
  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    // TODO логгер тут не работает почему-то...
    trace(request.path + " - 404")
    maybeApplication match {
      case Some(app) if app.mode == Mode.Prod =>
        SioHttpErrorHandler.http404Fut(request)

      // При разработке следует выводить нормальное 404.
      case _ =>
        super.onHandlerNotFound(request)
    }
  }


  /** Запрещаем бородатому scrypt'у грузить в jvm нативную amd64-либу, ибо она взрывоопасна без перекомпиляции
    * под свежие libcrypto.so (пакет openssl):
    *
    * Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
    * C  [libcrypto.so.1.0.0+0x6c1d7]  SHA256_Update+0x157
    *
    * Java frames: (J=compiled Java code, j=interpreted, Vv=VM code)
    *   com.lambdaworks.crypto.SCrypt.scryptN([B[BIIII)[B+0
    *   com.lambdaworks.crypto.SCrypt.scrypt([B[BIIII)[B+14
    *   com.lambdaworks.crypto.SCryptUtil.check(Ljava/lang/String;Ljava/lang/String;)Z+118
    * @see com.lambdaworks.jni.LibraryLoaders.loader(). */
  private def ensureScryptNoJni() {
    val scryptJniProp = "com.lambdaworks.jni.loader"
    if (System.getProperty(scryptJniProp) != "nil")
      System.setProperty(scryptJniProp, "nil")
  }
}


