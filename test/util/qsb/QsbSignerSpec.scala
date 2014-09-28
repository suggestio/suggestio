package util.qsb

import org.scalatestplus.play._
import play.core.parsers.FormUrlEncodedParser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 21:36
 * Description: Тесты для HMAC-подписывалки qs-аргументов.
 * Нужно подписывать аргументы, затем успешно проверять их подпись.
 */
class QsbSignerSpec extends PlaySpec {

  "QsbSigner" must {

    "sign and verify simple qs string" in {
      val unSignedQsString = "a=1"
      val signer = new QsbSigner("secretKey1", "sign")
      val signedQsString = signer.mkSigned(unSignedQsString)
      signedQsString must include (unSignedQsString)
      signedQsString must include ("&")
      signedQsString must include ("sign=")
      // Имитируем обращение к ссылке
      val qsParams = FormUrlEncodedParser.parse(signedQsString)
      val signCheckResult = signer.bind("", qsParams)
      signCheckResult mustBe defined
      signCheckResult mustBe Some(Right(Map("a" -> Seq("1"))))
    }

  }

}
