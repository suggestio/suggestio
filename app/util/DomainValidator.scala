package util

import _root_.util.event.SiowebNotifier
import models.MDomainAuthzT
import scala.concurrent.{Promise, Future}
import java.io.InputStream
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{TikaMetadataKeys, Metadata}
import java.util.concurrent.{Callable, FutureTask, ExecutionException, TimeoutException, TimeUnit}
import org.apache.http.HttpHeaders
import scala.concurrent.duration._
import io.suggest.sax.{SioMetaVerificationDetectorSAX, SioJsV2, SioSubstrDetectorSAX}
import org.xml.sax.ContentHandler
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.TeeContentHandler
import io.suggest.event.SioNotifier.{Classifier, ClassifierToken}
import io.suggest.util.event.subscriber.SioEventTJSable
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 10:58
 * Description: Модуль валидации нужен для:
 * - Прохождения валидации через meta-тег или загрузку файла на сервер.
 * - Поддержание актуальности любых MPersonDomainAuthz, в т.ч. qi.
 * Дергается из админки, когда нет уверенности в админских правах юзера.
 */

object DomainValidator extends Logs {

  private val minimalPaths = List("")
  private val validationNothingFoundFuture = Future.failed[Boolean](NoMoreUrlsValidationException)

  val parseTimeout = 3.seconds
  private val parseTimeoutMs = parseTimeout.toMillis

  /**
   * Возможно, необходимо запустить перевалидацию для домена.
   * @param da Экземпляр MPersonDomainAuthz.
   * @param sendEvents слать ли события об успехах/неудачах в шину?
   * @return Опциональный фьючерс с конечным результатом true/false. Если перевалидация не требуется, то вернется уже исполненный фьючерс с true.
   */
  def maybeRevalidate(da: MDomainAuthzT, sendEvents:Boolean = true): Option[Future[Boolean]] = {
    if (da.isNeedRevalidation)
      Some(revalidate(da, sendEvents))
    else
      None
  }

  /**
   * Запустить ревалидацию в фоне. Возвращаемый фьючерс содержит результат валидации.
   * @param da данные по валидации.
   * @param sendEvents слать ли результаты в шину SioNotifier? По дефолту - true.
   */
  def revalidate(da: MDomainAuthzT, sendEvents:Boolean = true): Future[Boolean] = {
    lazy val logPrefix = "revalidate(%s %s): " format(da, sendEvents)
    // Сгенерить имя файла, если это возможно
    val filenameOpt = if (da.isQiType)
      None
    else if (da.isValidationType)
      Some("/" + da.id + ".txt")
    else
      ???
    val dkey = da.dkey
    LOGGER.debug(logPrefix + "dkey=%s filename=%s".format(dkey, filenameOpt))

    // Подобрать ссылки для обхода.
    val urls = variateUrl(dkey, filenameOpt)
    LOGGER.debug(logPrefix + "will check URLs: " + urls)

    // Нужно вызывать функцию-итерацию проверки до первой удачи. А из функции быстро вернуть фьючерс результата.
    val p = Promise[Boolean]()

    def revalidateOne(urlsRest:List[String]) {
      if(urlsRest == Nil) {
        LOGGER.debug(logPrefix + " no more URLs to check. Validation failed.")
        // Закончился список ссылок. Нужно на этом и закончить.
        p completeWith validationNothingFoundFuture
        if (sendEvents)
          SiowebNotifier.publish(DVFailEvent(dkey=dkey, personIdOpt=da.personIdOpt))

      } else {
        // TODO нужно собирать ошибки в аккамулятор, и затем сохранять их в da.last_errors.
        // Есть ещё ссылки для обхода. Нужно извлечь верхнюю ссылку, взять и проверить её.
        val urlH :: urlsT = urlsRest
        LOGGER.debug(logPrefix + "sending req to " + urlH)
        val queueUrlFuture = DomainRequester.queueUrl(dkey, urlH)
        queueUrlFuture onComplete {
          case Success(DRResp200(ct:String, is:InputStream)) =>
            val result: Either[(String, Exception), Null] = try {
              // Вызвать парсеры-анализаторы и заполнить isSuccess.
              val md0 = new Metadata()
              md0.add(TikaMetadataKeys.RESOURCE_NAME_KEY, urlH)
              md0.add(HttpHeaders.CONTENT_TYPE, ct)
              val c = new DomainValidationTikaCallable(md0, is, da)
              val task = new FutureTask(c)
              val t = new Thread(task)
              t.start()
              try {
                task.get(parseTimeoutMs, TimeUnit.MILLISECONDS) match {
                  case false =>
                    val msg = "Validation code or expected js or expected meta-tag not found."
                    Left(msg -> new Exception(msg))

                  case true  => Right(null)
                }

              } catch {
                case ex:Exception =>
                  val msg = ex match {
                    case eex:ExecutionException => "Cannot parse page."
                    case tex:TimeoutException =>
                      LOGGER.warn(logPrefix + "Timeout parsing page %s ; Cancelling..." format urlH)
                      task.cancel(true)
                      "Page too big."
                    case _ => "Internal server error during validation procedure."
                  }
                  Left(msg -> ex)
              }

            } finally {
              is.close()
            }
            // Сайд-эффекты. Нужно заполнить promise результатом и
            result match {
              // Неудача чо-то с текущей ссылкой. Сообщить в шину и перейти к следующему.
              case Left((msg, ex)) =>
                if (sendEvents)
                  SiowebNotifier.publish(DVUrlFailedEvent(dkey=dkey, personIdOpt=da.personIdOpt, url=urlH, reason=msg, ex=ex))
                LOGGER.debug(logPrefix + "Check unsuccess for URL " + urlH, ex)
                revalidateOne(urlsT)

              // Всё ок.
              case Right(_) =>
                p complete Success(true)
                if (sendEvents)
                  SiowebNotifier.publish(DVSuccessEvent(dkey=dkey, personIdOpt=da.personIdOpt, url=urlH))
                LOGGER.debug("Check successeded for " + urlH)
            }


          // При ошибке запроса (не-200 ответ, таймаут, etc) надо уведомлять, лог писать и продолжать дальнейшие действия.
          // TODO следует это как-то объединить с ветвью Left((msg, ex)) в предыдущей части функции.
          case Failure(ex) =>
            LOGGER.debug("request to %s failed." format urlH, ex)
            // TODO нужно матчить исключение, извлекая из него причину ошибки. И отправлять юзеру.
            val msg = "Request failed"
            if(sendEvents)
              SiowebNotifier.publish(DVUrlFailedEvent(dkey=dkey, personIdOpt=da.personIdOpt, url=urlH, reason=msg, ex=ex))
            revalidateOne(urlsT)
        }
      }
    }
    revalidateOne(urls)
    p.future
  }


  /**
   * Создать набор ссылок для проверки валидатором.
   * @param dkey ключ домена
   * @param filenameOpt опциональное имя файла. Если None, то будет опрошен корень домена.
   * @return
   */
  private def variateUrl(dkey:String, filenameOpt:Option[String]) : List[String] = {
    val paths = filenameOpt match {
      case Some(filename) => variatePath(filename)
      case None           => minimalPaths
    }
    for(proto <- variateProto; host <- variateHostname(dkey); path <- paths)
      yield proto + "://" + host + "/" + path
  }

  /**
   * Варьируем хостнейм
   * @param host Хостнейм. Обычно это dkey.
   * @return
   */
  private def variateHostname(host:String): List[String] = {
    if (host.startsWith("www."))
      List(host, host.substring(4))
    else
      List("www." + host, host)
  }

  /**
   * Варьировать допустимые протоколы связи.
   */
  private val variateProto = List("http", "https")

  /**
   * Варьировать имя файла (без пути и расширения) по-всякому.
   * @param filename исходное имя файла.
   * @return Список путей, включая исходный
   */
  private def variatePath(filename:String): List[String] = {
    List("",  filename,  filename + ".txt")
  }

}

object NoMoreUrlsValidationException extends Exception


// tika-callable, содержащий прерываемую задачу по детектированию энтерпрайза.
class DomainValidationTikaCallable(md:Metadata, input:InputStream, da:MDomainAuthzT) extends Callable[Boolean] {

  /**
   * Запуск анализаторов. Используется TeeContentHandler для мультиплекса различных sio-SAX-handler'ов.
   * Функция выдает список найденных на странице данных для валидации.
   * @return
   */
  def call() = {
    val jsDetector = new SioJsDetectorInterruptableSAX
    var detectors = List[ContentHandler](jsDetector)
    // Если есть код тела проверки, то надо добавить детектор подстроки в писанине.
    if (da.bodyCodeOpt.isDefined)
      detectors = new SioSubstrDetectorSAX(da.bodyCodeOpt.get) :: detectors
    // Использовать AutoDetectParser, ибо на входе могут быть как HTML-файлы, так и plain-text.
    val parser = new AutoDetectParser()
    // Если в списке детекторов валидации только один детектор, то TeeContentHandler избыточен.
    val teeDetector = if(detectors.tail.isEmpty) {
      detectors.head
    } else {
      new TeeContentHandler(detectors : _*)
    }

    parser.parse(input, teeDetector, md)
    // Пройтись по исходному списку детекторов и выявить наличие в них положительных результатов.
    detectors.exists {
      // Обыденный детектор установленного js. Нужно проверить qi_id, записанный в js-ссылке.
      case d: SioJsDetectorInterruptableSAX =>
        val jsInfo = d.getSioJsInfo.find {
          case SioJsV2(_dkey, _qi_id) =>
            _dkey == da.dkey && _qi_id == da.id

          case _ => false
        }
        jsInfo.isDefined

      // Подключен к работе поиск подстроки по body_code.
      case d: SioSubstrDetectorSAX =>
        d.isMatchFound

    } || {
      // Если детекторы ничего не нашли, проверить собранные мета-теги на предмет там кода верификации.
      SioMetaVerificationDetectorSAX.findInTikaMetadata(md) exists(_ == da.id)
    }
  }

}


object DVEventUtil {
  val headSneToken: ClassifierToken = Some("dve")
  val jsonEventType = JsString("domain_validation")

  /**
   * Сгенерить классификатор для события.
   * @param dkeyOpt Ключ домена.
   * @param personIdOpt id юзера, который проходит валидацию.
   * @param isSuccessOpt Бинарный результат проверки.
   * @return Классификатор, пригодный для SiowebNotifier.
   */
  def getClassifier(dkeyOpt:Option[String] = None, personIdOpt:Option[String] = None, isSuccessOpt:Option[Boolean] = None): Classifier = {
    List(headSneToken, dkeyOpt, personIdOpt, isSuccessOpt)
  }
}




sealed trait DVEvent extends SioEventTJSable with DkeyContainerT {
  import DVEventUtil.jsonEventType

  val dkey: String
  val personIdOpt: Option[String]

  // true, если есть положительный результат.
  val isSuccess: Boolean

  // true, если процедура валидации домена на этом завершается.
  val isFinished: Boolean

  // Внутренняя функция. Подклассы пишут в неё специфичные для себя части json-списка, выводимые через toJson.

  protected def jsonProps: List[(String, JsValue)]

  def getClassifier: Classifier = DVEventUtil.getClassifier(dkeyOpt=Some(dkey), personIdOpt=personIdOpt, isSuccessOpt=Some(isSuccess))

  /**
   * Сериализовать данные события в json.
   * @return
   */
  implicit def toJson: JsValue = {
    val jsonFields0 = jsonProps
    val jsonFields1 = personIdOpt match {
      case Some(person_id) => "personId"   -> JsString(person_id) :: jsonFields0
      case None => jsonFields0
    }
    val jsonFields2 = {
      "type"        -> jsonEventType ::
      "dkey"        -> JsString(dkey) ::
      "is_success"  -> JsBoolean(isSuccess) ::
      "is_finished" -> JsBoolean(isFinished) ::
      jsonFields1
    }
    JsObject(jsonFields2)
  }
}

// Событие "проверка пройдена"
case class DVSuccessEvent(dkey:String, personIdOpt:Option[String], url:String) extends DVEvent {
  val isSuccess: Boolean  = true
  val isFinished: Boolean = true

  protected def jsonProps: List[(String, JsValue)] = List("url" -> JsString(url))
}

// Событие "На этой ссылке не фортануло". Т.е. некий промежуточный результат.
case class DVUrlFailedEvent(dkey:String, personIdOpt:Option[String], url:String, reason:String, ex:Throwable) extends DVEvent {
  val isSuccess: Boolean  = false
  val isFinished: Boolean = false

  protected def jsonProps: List[(String, JsValue)] = List(
    "reason" -> JsString(reason),
    "url"    -> JsString(url)
  )
}

// Событие "Вся валидация не удалась". Выдается после прохода всех ссылок с неудачами.
case class DVFailEvent(dkey:String, personIdOpt:Option[String]) extends DVEvent {
  val isSuccess: Boolean  = false
  val isFinished: Boolean = true

  protected def jsonProps: List[(String, JsValue)] = Nil
}
