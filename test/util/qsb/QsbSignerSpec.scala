package util.qsb

import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication
import play.core.parsers.FormUrlEncodedParser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 21:36
 * Description: Тесты для HMAC-подписывалки qs-аргументов.
 * Нужно подписывать аргументы, затем успешно проверять их подпись.
 */
class QsbSignerSpec extends PlaySpec with OneAppPerSuite {

  override implicit lazy val app: FakeApplication = {
    FakeApplication(
      withGlobal = Some(new GlobalSettings() {
        override def onStart(app: play.api.Application) {
          super.onStart(app)
          println("Started dummy fake application, without Global.onStart() initialization.")
        }
      })
    )
  }

  /** Функция для тестирования генерации подписанной qs и проверки подписи. */
  private def singAndCheck(unSignedQsString: String, key: String, paramsMap: Map[String, Seq[String]], signKeyName: String): Unit = {
    val signer = new QsbSigner("secretKey1", signKeyName)
    val signedQsString = signer.mkSigned(key, unSignedQsString)
    signedQsString must include (unSignedQsString)
    if (paramsMap.nonEmpty)
      signedQsString must include ("&")
    signedQsString must include (s"$signKeyName=")
    // Имитируем обращение к сгенеренной ссылке
    val qsParams = FormUrlEncodedParser.parse(signedQsString)
    val signCheckResult = signer.bind(key, qsParams)
    signCheckResult mustBe defined
    signCheckResult mustBe Some(Right(paramsMap))
    // Имитируем подстановку ссылки без сигнатуры
    val sigMissParams = FormUrlEncodedParser.parse(unSignedQsString)
    val sigMissCheckResult = signer.bind(key, sigMissParams)
    sigMissCheckResult.filter(_.isLeft) mustBe None
    // Имитируем подстановку неверной сигнатуры
    val sigInvalidParams = sigMissParams + (signKeyName -> Seq("9e32295f8225803bb6d5fdfcc0674616a4413c1b"))
    val sigInvalidResult = signer.bind(key, sigInvalidParams)
    sigInvalidResult mustBe Some(Left(QsbSigner.SIG_INVALID_MSG))
  }


  "QsbSigner" must {

    "sign and verify simple qs string" in {
      singAndCheck(
        unSignedQsString = "a=1",
        key = "",
        paramsMap = Map("a" -> Seq("1")),
        signKeyName = "sign"
      )
    }

    "sign and verify comples qs.string without no-sig parts" in {
      singAndCheck(
        unSignedQsString = "i.id=awdfawerfwe5235_234df&i.sz.w=314&i.sz.h=234",
        key = "i.",
        paramsMap = Map("i.id" -> Seq("awdfawerfwe5235_234df"), "i.sz.w" -> Seq("314"), "i.sz.h" -> Seq("234")),
        signKeyName = "sig"
      )
    }

    "sign and verify comples qs.string with no-sig parts" in {
      singAndCheck(
        unSignedQsString = "i.id=awdfawerfwe5235_234df&i.sz.w=314&i.sz.h=234&z=asdasd",
        key = "i.",
        paramsMap = Map("i.id" -> Seq("awdfawerfwe5235_234df"), "i.sz.w" -> Seq("314"), "i.sz.h" -> Seq("234")),
        signKeyName = "sig"
      )
    }


    "sign and verify comples qs.string with no-sig parts and sig inside signed keyspace" in {
      singAndCheck(
        unSignedQsString = "i.id=awdfawerfwe5235_234df&i.sz.w=314&i.sz.h=234&z=asdasd",
        key = "i.",
        paramsMap = Map("i.id" -> Seq("awdfawerfwe5235_234df"), "i.sz.w" -> Seq("314"), "i.sz.h" -> Seq("234")),
        signKeyName = "i.sig"
      )
    }

    "sign and verify empty query string" in {
      singAndCheck(
        unSignedQsString = "",
        key = "",
        paramsMap = Map(),
        signKeyName = "siga"
      )
    }

    "sign and verify empty query string with non-signed data" in {
      singAndCheck(
        unSignedQsString = "a.z=123&a.c=123",
        key = "x",
        paramsMap = Map(),
        signKeyName = "siga"
      )
    }
  }

}
