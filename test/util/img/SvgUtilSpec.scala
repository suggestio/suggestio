package util.img

import java.io.File

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 13:37
 * Description: Тесты для SvgUtil.
 */
class SvgUtilSpec extends PlaySpec {

  private val TEST_RSC_DIR = "/util/img/svg_util"

  private def testFile(filename: String, isSvg: Boolean): Unit = {
    testFilePath(s"$TEST_RSC_DIR/$filename", isSvg)
  }

  /** Тестируем isSvgValid() и isSvgFileValid(). */
  private def testFilePath(filepath: String, isSvg: Boolean): Unit = {
    val is = getClass.getResourceAsStream(filepath)
    assert(is != null)
    try {
      SvgUtil.isSvgValid(is)  mustBe  isSvg
    } finally {
      is.close()
    }

    val fpath = getClass.getResource(filepath).getFile
    val f = new File(fpath)
    SvgUtil.isSvgFileValid(f) mustBe isSvg
  }


  "isSvgValid()" must {

    "see a valid idented svg file: valid_svg1_idented.svg" in {
      testFile("valid_svg1_idented.svg", isSvg = true)
    }

    "accept shrinked version of previous file too (valid_svg1_shrinked.svg)" in {
      testFile("valid_svg1_shrinked.svg", isSvg = true)
    }

    "accept valid svg file: valid_svg2_shrinked.svg" in {
      testFile("valid_svg2_shrinked.svg", isSvg = true)
    }

    "refuse text file (lorem_ipsum.txt)" in {
      testFile("lorem_ipsum.txt", isSvg = false)
    }

    "refuse raster graphics (1214545755807.jpg)" in {
      testFilePath("/util/img/1214545755807.jpg", isSvg = false)
    }

    "refuse invalid svg (invalid_svg1_idented.svg)" in {
      testFile("invalid_svg1_idented.svg", isSvg = false)
    }

    "refuse valid xml non-svg file (uca.xml)" in {
      testFile("uca.xml", isSvg = false)
    }

  } // isSvgValid()


}
