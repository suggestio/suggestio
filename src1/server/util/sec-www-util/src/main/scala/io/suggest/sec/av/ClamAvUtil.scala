package io.suggest.sec.av

import javax.inject.Inject

import io.suggest.async.AsyncUtil
import io.suggest.util.logs._

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
  def scan(req: ClamAvScanRequest): Future[ClamAvScanResult] = {
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
  def scanClamdFdpass(req: ClamAvScanRequest): ClamAvScanResult = {
    import Words._
    val ret = Process(CLAMDSCAN :: FDPASS :: req.file :: Nil) ! LOGGER.process
    ClamAvScanResult(ret)
  }

}


case class ClamAvScanRequest(
                              file: String
                            )


case class ClamAvScanResult(
                             result   : Int
                           ) {

  def isClean: Boolean = {
    result == 0
  }

}
