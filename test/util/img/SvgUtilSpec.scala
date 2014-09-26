package util.img

import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 13:37
 * Description: Тесты для SvgUtil.
 */
class SvgUtilSpec extends FlatSpec with Matchers {

  private val TEST_RSC_DIR = "/util/img/svg_util"

  private def testFile(filename: String, isSvg: Boolean): Unit = {
    testFilePath(s"$TEST_RSC_DIR/$filename", isSvg)
  }

  private def testFilePath(filepath: String, isSvg: Boolean): Unit = {
    val is = getClass.getResourceAsStream(filepath)
    assert(is != null)
    try {
      SvgUtil.isSvgValid(is)  shouldBe  isSvg
    } finally {
      is.close()
    }
  }


  "isSvgValid()" should "see a valid idented svg file: valid_svg1_idented.svg" in {
    testFile("valid_svg1_idented.svg", isSvg = true)
  }

  it should "accept shrinked version of previous file too (valid_svg1_shrinked.svg)" in {
    testFile("valid_svg1_shrinked.svg", isSvg = true)
  }

  it should "accept valid svg file: valid_svg2_shrinked.svg" in {
    testFile("valid_svg2_shrinked.svg", isSvg = true)
  }

  it should "refuse text file (lorem_ipsum.txt)" in {
    testFile("lorem_ipsum.txt", isSvg = false)
  }

  it should "refuse raster graphics (1214545755807.jpg)" in {
    testFilePath("/util/img/1214545755807.jpg", isSvg = false)
  }

  it should "refuse invalid svg (invalid_svg1_idented.svg)" in {
    testFile("invalid_svg1_idented.svg", isSvg = false)
  }

  it should "refuse valid xml non-svg file (uca.xml)" in {
    testFile("uca.xml", isSvg = false)
  }

}
