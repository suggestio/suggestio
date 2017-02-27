package util.qsb

import functional.OneAppPerSuiteNoGlobalStart
import io.suggest.util.logs.MacroLogsImpl
import org.scalatestplus.play._
import play.core.parsers.FormUrlEncodedParser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 21:36
 * Description: Тесты для HMAC-подписывалки qs-аргументов.
 * Нужно подписывать аргументы, затем успешно проверять их подпись.
 */
class QsbSignerSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart with MacroLogsImpl {

  import LOGGER._

  /** Функция для тестирования генерации подписанной qs и проверки подписи. */
  private def singAndCheck(unSignedQsString: String, key: String, paramsMap: Map[String, Seq[String]], signKeyName: String): Unit = {
    val logPrefix = s"signAndCheck(${System.currentTimeMillis}):"
    trace(s"$logPrefix Starting with:\n unsigned = $unSignedQsString\n key = $key\n params = ${paramsMap.mkString(" & ")}\n signKeyName = $signKeyName")

    val signer = new QsbSigner("__THE_TOP_SECRET_PASSWORD_KEY__", signKeyName)
    val signedQsString = signer.mkSigned(key, unSignedQsString)
    trace(s"$logPrefix Signed: $signedQsString")
    signedQsString must include (unSignedQsString)

    if (paramsMap.nonEmpty)
      signedQsString must include ("&")

    signedQsString must include (s"$signKeyName=")

    // Имитируем обращение к сгенеренной ссылке
    val qsParams = FormUrlEncodedParser.parse(signedQsString)
    val signCheckResult = signer.bind(key, qsParams)
    trace(s"$logPrefix Sign check result: $signCheckResult")
    signCheckResult mustBe defined
    signCheckResult mustBe Some(Right(paramsMap))

    // Имитируем подстановку ссылки без сигнатуры
    val sigMissParams = FormUrlEncodedParser.parse(unSignedQsString)
    val sigMissCheckResult = signer.bind(key, sigMissParams)
    trace(s"$logPrefix sigMissCheckResult: $sigMissCheckResult")
    sigMissCheckResult.filter(_.isLeft) mustBe None

    // Имитируем подстановку неверной сигнатуры
    val sigInvalidParams = sigMissParams + (signKeyName -> Seq("9999999999999999999999999999999999999999"))
    val sigInvalidResult = signer.bind(key, sigInvalidParams)
    trace(s"$logPrefix sigInvalidResult: $sigInvalidResult")
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
        signKeyName = "sige"
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

    // length-extension attack: подставляется параметр с дублирующимся id, но иным значением.
    "die on lenght-extension attack" in {
      val unSignedQsString = "i.id=SOME_VALID_ID"
      val signKeyName = "sig"
      val key = "i"

      val signer = new QsbSigner("__THE_TOP_SECRET_PASSWORD_KEY__", signKeyName)
      val signedQsString = signer.mkSigned(key, unSignedQsString)

      val attackSignedQsString = signedQsString + "&i.id=ATTACKER_ID"

      // Имитируем обращение к сгенеренной ссылке
      val qsParams = FormUrlEncodedParser.parse(attackSignedQsString)
      val signCheckResult = signer.bind(key, qsParams)
      // Убедиться, что проверка зафейлилась.
      trace(s"sigCheckResult: $signCheckResult")

      assert(
        signCheckResult.exists(_.isLeft),
        signCheckResult
      )
    }

  }

}
