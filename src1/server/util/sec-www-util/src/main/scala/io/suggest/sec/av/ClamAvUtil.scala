package io.suggest.sec.av

import javax.inject.Inject

import io.suggest.async.AsyncUtil
import io.suggest.util.logs._
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.18 12:05
  * Description: Утиль для сканирования файлов антивирусом clam.
  * Используется для проверки принимаемых файлов.
  *
  * Есть два варианта проверки:
  * - Быстрый локальный через unix-сокет (clamdscan --fdpass).
  * - Обычный, с передачей файла по tcp.
  */
final class ClamAvUtil @Inject()(
                                  asyncUtil                 : AsyncUtil,
                                  implicit private val ec   : ExecutionContext
                                )
  extends MacroLogsImpl
{

  def USE_FDPASS_METHOD = true

  object Words {
    final def CLAMDSCAN = "clamdscan"
    final def FDPASS    = "--fdpass"
  }

  /** API для проверки файла антивирусом.
    *
    * @param req Описание данных для сканера.
    * @return Фьючерс с результатом проверки.
    */
  def scan(req: ClamAvScanRequest): Future[IClamAvScanResult] = {
    if (USE_FDPASS_METHOD) {
      Future {
        scanClamdFdpass(req)
      }( asyncUtil.singleThreadCpuContext )

    } else {
      // TODO Метод сканирования через TCP. Тут нужна будет доп.зависимость в виде clamd-java или подобного.
      ???
    }
  }


  /** Быстрое простое сканирование антивирусом с помощью консольной утилиты clamdscan.
    *
    * @param req Описание данных для сканирования.
    * @return Фьючерс с результатом.
    */
  def scanClamdFdpass(req: ClamAvScanRequest): ClamdscanResult = {
    lazy val logPrefix = s"scanClamdFdpass()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix $req")

    import Words._
    val ret = Process(CLAMDSCAN :: FDPASS :: req.file :: Nil) ! LOGGER.process(logPrefix)
    ClamdscanResult(ret)
  }

}


/** Модель-контейнер аргументов запроса AV-проверки.
  *
  * @param file Проверяемый файл.
  */
case class ClamAvScanRequest(
                              file: String
                            )


/** Интерфейс модели любого результата запроса AV-проверки. */
trait IClamAvScanResult {
  def isClean: Boolean
}


/** Модель-контейнер аргументов результата AV-проверки через консольную clamdscan.
  *
  * @param result Код результата, возвращённый консольной утилитой.
  */
case class ClamdscanResult(
                            result   : Int
                          )
  extends IClamAvScanResult
{

  def isClean: Boolean = {
    result ==* 0
  }

}
