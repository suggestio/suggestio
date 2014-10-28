package util.img

import io.suggest.img.ImgCrop
import models.im.AbsCropOp
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.14 17:34
 * Description: Тесты для парсеров сериализованных идентификаторов картинок.
 */
class ImgFileNameParsersSpec extends PlaySpec {

  val p = new ImgFileNameParsers {}
  import p._

  "parseAll()" must {

    "parse simple alphanumeric img id:  k1zcZDgXSBOeQzV4GpOUiw" in {
      val pr = p.parseAll(fileNameP, "k1zcZDgXSBOeQzV4GpOUiw")
      pr.successful mustBe true
      pr.get._2     mustBe empty
    }
    
    "parse simple serialized img uuid:  RJInLazORcmqjV-74oMUdQ" in {
      val pr = p.parseAll(fileNameP, "RJInLazORcmqjV-74oMUdQ")
      pr.successful mustBe true
      pr.get._2     mustBe empty
    }

    "parse serialized img uuid with trailing '?' and empty dyn args" in {
      val pr = p.parseAll(fileNameP, "RJInLazORcmqjV-74oMUdQ?")
      pr.successful mustBe true
      pr.get._2     mustBe empty
    }

    "parse serialized compat. imgId~crop:  RJInLazORcmqjV-74oMUdQ~305x468_409_0" in {
      val pr = p.parseAll(fileNameP, "RJInLazORcmqjV-74oMUdQ~305x468_409_0")
      pr.successful   mustBe true
      pr.get._2       mustBe List(AbsCropOp(ImgCrop(305, 468, 409, 0)))
    }

    "parse serialized imgId with tiny dynArgs: ???" in {
      pending   // TODO Нужен пример данных и тест для парсинга оных.
    }
  }

}
