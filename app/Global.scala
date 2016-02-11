import akka.actor.Cancellable
import io.suggest.model.es.EsModelUtil
import io.suggest.util.SioEsUtil
import models.usr.MSuperUsers
import org.elasticsearch.client.Client
import org.elasticsearch.index.mapper.MapperException
import util.event.SiowebNotifier
import util.secure.PgpUtil
import util.showcase.ScStatSaver
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
    // Запускаем супервизора вместе с деревом остальных акторов.
    _inject[SiowebSup](app).startLink()
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    val esClientFut = esNodeFut map {
      _.client()
    }
    val fut = esClientFut flatMap { implicit esClient =>
      initializeEsModels(app) map { _ => esClient }
    } flatMap { esClient =>
      // Если в конфиге явно не включена поддержка проверки суперюзеров в БД, то не делать этого.
      // Это также нужно было при миграции с MPerson на MNode, чтобы не произошло повторного создания новых
      // юзеров в MNode, при наличии уже существующих в MPerson.
      val ck = "start.ensure.superusers"
      val createIfMissing = app.configuration.getBoolean(ck).getOrElse(false)
      val mSuperUsers = _inject[MSuperUsers](app)
      val fut = mSuperUsers.resetSuperuserIds(createIfMissing)
      if (!createIfMissing)
        debug("Does not ensuring superusers in permanent models: " + ck + " != true")
      fut.map { _ => esClient }
    }

    // Инициализировать связку ключей, если необходимо.
    esClientFut onSuccess { case esClient =>
      PgpUtil.maybeInit()(esClient)
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
    synchronized {
      cronTimers = crontab(app).startTimers(app)
    }
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
  private def crontab(app: Application)         = _inject[Crontab](app)
  private def jmxImpl(app: Application)         = _inject[JMXImpl](app)
  private def siowebEsModel(app: Application)   = _inject[SiowebEsModel](app)
  private def scStatSaver(app: Application)     = _inject[ScStatSaver](app)

  /**
   * При остановке системы (например, при обновлении исходников), нужно выполнить все нижеперечисленные действия.
   * @param app Экщемпляр класса Application.
   */
  override def onStop(app: Application) {
    scStatSaver(app).BACKEND.close()
    // Была одна ошибка после проблемы в DI после onStart(). JMXImpl должен останавливаться перед elasticsearch.
    jmxImpl(app).unregisterAll()
    val esCloseFut = Future {
      SiowebEsUtil.stopNode()
    }

    // В текущем потоке: Исполняем синхронные задачи завершения работы...
    super.onStop(app)
    // Остановить таймеры
    synchronized {
      crontab(app).stopTimers(cronTimers)
      cronTimers = null
    }

    // Дожидаемся завершения асинхронных задач.
    Await.ready(esCloseFut, 20.seconds)
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


