package io.suggest.sec.av

import java.util.concurrent.TimeUnit

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.18 12:58
  * Description: Тесты для антивирусного сканирования.
  */
class ClamAvUtilSpec extends PlaySpec with GuiceOneAppPerSuite {

  override implicit lazy val app: Application = {
    new GuiceApplicationBuilder()
      .configure(
        "sec.av.clam.tcp.host" -> "localhost"
      )
      .build()
  }

  // TODO Есть проблема с инжекцией в тестах.
  private lazy val clamAvUtil = app.injector.instanceOf[ClamAvUtil]

  private def _getReqForFileName(filename: String): ClamAvScanRequest = {
    val url = getClass.getResource("/clam/" + filename)
    ClamAvScanRequest(
      file = url.getFile
    )
  }

  private val TEXT_FILE = "text-file"
  private val EICAR_FILE = "eicar.com.txt"

  private def _fdPassTest(file: String, isClean: Boolean) = {
    clamAvUtil
      .scanClamdFdpass( _getReqForFileName(file) )
      .isClean mustBe isClean
  }

  private def _remoteTest(file: String, isClean: Boolean) = {
    val scanFut = clamAvUtil.scanClamdRemote( _getReqForFileName(file) )
    await( scanFut, 3, TimeUnit.SECONDS ).isClean mustBe isClean
  }


  "--fdpass scan" must {

    "bypass simple text file" in {
      _fdPassTest(TEXT_FILE, true)
    }

    "Report problem on EICAR sign" in {
      _fdPassTest(EICAR_FILE, false)
    }

  }



  "remote clamd scan" must {

    "bypass simple text file" in {
      _remoteTest( TEXT_FILE, true )
    }

    "Report problem on EICAR sign" in {
      _remoteTest( EICAR_FILE, false )
    }

  }

}
