import akka.actor.Cancellable
import io.suggest.model.{SioCassandraClient, EsModel}
import io.suggest.util.SioEsUtil
import models.usr.{MPerson, MPersonIdent, EmailPwIdent}
import org.elasticsearch.client.Client
import org.elasticsearch.index.mapper.MapperException
import play.api.mvc.{Result, WithFilters, RequestHeader}
import util.cdn.{DumpXffHeaders, CorsFilter}
import util.event.SiowebNotifier
import util.radius.RadiusServerImpl
import util.showcase.ScStatSaver
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

object Global extends WithFilters(new HtmlCompressFilter, new CorsFilter, new DumpXffHeaders) {

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
    SiowebSup.startLink
    // Запускать es-клиент при старте, ибо подключение к кластеру ES это занимает некоторое время.
    val fut = esNodeFut map {
      _.client()
    } flatMap { implicit esClient =>
      initializeEsModels() map { _ => esClient }
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
    RadiusServerImpl.start(true, true)
  }



  /** Проинициализировать все ES-модели и основной индекс. */
  def initializeEsModels(tried21: Boolean = false)(implicit client: Client): Future[_] = {
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
    // TODO 2014.aug.25: Снести этот код потом, когда мастер будет обновлён.
    futMappings recoverWith {
      case ex: MapperException if !tried21 =>
        info("Trying to update main index to v2.1 settings...")
        SioEsUtil.updateIndexTo2_1(EsModel.DFLT_INDEX) flatMap { _ =>
          initializeEsModels(tried21 = true)
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
    val esCloseFut = Future {
      SiowebEsUtil.stopNode()
    }

    // В текущем потоке: Исполняем синхронные задачи завершения работы...
    super.onStop(app)
    JMXImpl.unregisterAll()
    // Остановить таймеры
    synchronized {
      Crontab.stopTimers(cronTimers)
      cronTimers = null
    }
    // Останавливаем RADIUS-сервер.
    RadiusServerImpl.stop()

    // Дожидаемся завершения асинхронных задач.
    val closeFut = casCloseFut.flatMap(_ => esCloseFut)
    Await.ready(closeFut, 20 seconds)
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
    val se = MPersonIdent.SU_EMAILS
    trace(s"${logPrefix}There are ${se.size} superuser emails: [${se.mkString(", ")}]")
    Future.traverse(se) { email =>
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
        trace(logPrefix + suPersonIds.length + " superusers installed successfully")

      case Failure(ex) =>
        error(logPrefix + "Failed to install superusers", ex)
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


