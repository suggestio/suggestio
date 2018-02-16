package io.suggest.sec.av

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.18 12:58
  * Description: Тесты для антивирусного сканирования.
  */
class ClamAvUtilSpec extends PlaySpec with GuiceOneAppPerSuite {

  // TODO Есть проблема с инжекцией в тестах.
  private lazy val clamAvUtil = app.injector.instanceOf[ClamAvUtil]

  private def _getReqForFileName(filename: String): ClamAvScanRequest = {
    ClamAvScanRequest(
      file = getClass.getResource("av/text-file").getFile
    )
  }

  "--fdpass scan" must {

    "bypass simple text file" in {
      clamAvUtil.scanClamdFdpass( _getReqForFileName("text-file") )
    }

    "Report problem on EICAR sign" in {
      clamAvUtil.scanClamdFdpass( _getReqForFileName("eicar.com.txt") )
    }

  }

}
