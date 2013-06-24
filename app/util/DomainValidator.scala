package util

import models.MPersonDomainAuthz
import models.MPersonDomainAuthz.{TYPE_QI, TYPE_VALIDATION}
import scala.concurrent.{Promise, Future}
import java.io.InputStream
import scala.util.Success
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 10:58
 * Description: Модуль валидации нужен для:
 * - Прохождения валидации через meta-тег или загрузку файла на сервер.
 * - Поддержание актуальности любых MPersonDomainAuthz, в т.ч. qi.
 * Дергается из админки, когда нет уверенности в админских правах юзера.
 */

object DomainValidator {

  private val minimalPaths = List("")
  private val trueFuture  = Future.successful(true)
  private val falseFuture = Future.successful(false)
  private type RevalidateResult_t = Boolean
  private val validationNothingFoundFuture = Future.failed[RevalidateResult_t](NoMoreUrlsValidationException)

  /**
   * Запустить ревалидацию в фоне. Возвращаемый фьючерс
   * @param da данные по валидации.
   * @param sendEvents слать ли результаты в шину SioNotifier?
   */
  def revalidate(da: MPersonDomainAuthz, sendEvents:Boolean): Future[RevalidateResult_t] = {
    val filenameOpt = if (da.typ == TYPE_QI)
      None
    else
      Some("/" + da.id + ".txt")
    val dkey = da.dkey
    val urls = variateUrl(dkey, filenameOpt)
    // Нужно вызывать функцию-итерацию проверки до первой удачи.
    val p = Promise[RevalidateResult_t]()
    def revalidateOne(urlsRest:List[String], errAcc:List[(String, String)]) {
      if(urlsRest == Nil) {
        // Закончился список ссылок. Нужно на этом и закончить.
        p completeWith validationNothingFoundFuture

      } else {
        // Есть ещё ссылки для обхода. Нужно извлечь верхнюю ссылку, взять и проверить её.
        val urlH :: urlsT = urlsRest
        val queueUrlFuture = DomainRequester.queueUrl(dkey, urlH)
        queueUrlFuture onSuccess { case DRResp200(ct:String, is:InputStream) =>
          try {
            // TODO тут надо вызвать парсеры-анализаторы и заполнить isSuccess.
            val isSuccess = true
            if (isSuccess) {
              p complete Success(true)

            } else {
              revalidateOne(urlsT, errAcc)
            }

          } finally {
            is.close()
          }
        }
      }
    }
    revalidateOne(urls, Nil)
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
      yield (proto + "://" + host + "/" + path)
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
