package util.img

import functional.{OneAppPerSuiteNoGlobalStart, RegexParsersTesting}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.img.crop.MCrop
import io.suggest.util.UuidUtil.base64ToUuid
import models._
import models.im._
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.14 17:34
 * Description: Тесты для парсеров сериализованных идентификаторов картинок.
 */
class ImgFileNameParsersSpec
  extends PlaySpec
  with OneAppPerSuiteNoGlobalStart
  with ImgFileNameParsers
  with RegexParsersTesting
{

  /** Для тестирования парсера с удобным выводом ошибок лучше использовать сие: */
  private def parseFileName(f: CharSequence) = parseSuccess(fileNameP, f)


  "Parser 'fileNameP'" must {

    "parse simple alphanumeric img id:  k1zcZDgXSBOeQzV4GpOUiw" in {
      val pr = parseFileName("k1zcZDgXSBOeQzV4GpOUiw")
      pr._1         mustBe base64ToUuid("k1zcZDgXSBOeQzV4GpOUiw")
      pr._2         mustBe empty
    }
    "parse another simple serialized img uuid:  RJInLazORcmqjV-74oMUdQ" in {
      val pr = parseFileName("RJInLazORcmqjV-74oMUdQ")
      pr._1         mustBe base64ToUuid("RJInLazORcmqjV-74oMUdQ")
      pr._2         mustBe empty
    }


    "parse serialized img uuid with trailing '?' and empty dyn args" in {
      val pr = parseFileName("RJInLazORcmqjV-74oMUdQ?")
      pr._1         mustBe base64ToUuid("RJInLazORcmqjV-74oMUdQ")
      pr._2         mustBe empty
    }
    "parse serialized img uuid with trailing '~' and empty dyn args or missing compat.crop" in {
      val pr = parseFileName("RJInLazORcmqjV-74oMUdQ~")
      pr._1         mustBe base64ToUuid("RJInLazORcmqjV-74oMUdQ")
      pr._2         mustBe empty
    }


    "parse serialized compat. imgId~crop:  RJInLazORcmqjV-74oMUdQ~305x468_409_0" in {
      val pr = parseFileName("RJInLazORcmqjV-74oMUdQ~305x468_409_0")
      pr._1         mustBe base64ToUuid("RJInLazORcmqjV-74oMUdQ")
      pr._2         mustBe List(AbsCropOp(MCrop(305, 468, 409, 0)))
    }
    "parse serialize compat#2. : Zr7DfeinSqCCTaAIcUzwwQ~319x319_258_0" in {
      val pr = parseFileName("Zr7DfeinSqCCTaAIcUzwwQ~319x319_258_0")
      pr._1         mustBe base64ToUuid("Zr7DfeinSqCCTaAIcUzwwQ")
      pr._2         mustBe List(AbsCropOp(MCrop(319, 319, 258, 0)))
    }


    "parse serialized imgId with dynArgs: simple '...~c=256x256' - [AbsResizeOp]" in {
      val pr = parseFileName("8wQs1SQQRXavk4UFF0v7-A~c=256x256'")
      pr._1         mustBe base64ToUuid("8wQs1SQQRXavk4UFF0v7-A")
      pr._2         mustBe List(AbsResizeOp(MSize2di(256, 256)))
    }
    "parse serialized img filename with dynArgs: simple: ...?c=256x256 (unfamiliar url-qs-like syntax)" in {
      val pr = parseFileName("8wQs1SQQRXavk4UFF0v7-A~c=256x256'")
      pr._1         mustBe base64ToUuid("8wQs1SQQRXavk4UFF0v7-A")
      pr._2         mustBe List(AbsResizeOp(MSize2di(256, 256)))
    }

    "parse serialized img filename with several imOps: ...~i=a&c=140x140a&h=&d=a&j=a&f=80" in {
      val pr = parseFileName("8wQs1SQQRXavk4UFF0v7-A~i=a&c=140x140b&h=&d=a&j=a&f=80")
      pr._1         mustBe base64ToUuid("8wQs1SQQRXavk4UFF0v7-A")
      pr._2         mustBe List(
        ImFilters.Lanczos,
        AbsResizeOp(MSize2di(140, 140), ImResizeFlags.OnlyShrinkLarger),
        StripOp,
        ImInterlaces.Plane,
        ImSamplingFactors.SF_1x1,
        QualityOp(80.0)
      )
    }

  }

}
