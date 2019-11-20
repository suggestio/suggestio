package io.suggest.img

import io.suggest.img.crop.MCrop
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.14 13:23
 * Description: Тесты для парсинга сериализованного кропа.
 */
class ImgCropParsersSpec extends AnyFlatSpec {

  private val p = new ImgCropParsers {}
  import p._

  "cropStrP" should "parse simple string: 305x468_409_0" in {
    val pr = parseAll(cropStrP, "305x468_409_0")
    pr.successful shouldBe  true
    pr.get        shouldBe  MCrop(305, 468, 409, 0)
  }

  it should "parse other simple string: 319x319_258_0" in {
    val pr = parseAll(cropStrP, "319x319_258_0")
    pr.successful shouldBe  true
    pr.get        shouldBe  MCrop(319, 319, 258, 0)
  }

}
