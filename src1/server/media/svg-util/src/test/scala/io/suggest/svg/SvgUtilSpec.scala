package io.suggest.svg

import java.io.File

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 13:37
 * Description: Тесты для SvgUtil.
 */
class SvgUtilSpec extends FlatSpec {

  private val RSC_DIR = ""

  private def testFile(filename: String, isSvg: Boolean): Unit = {
    testFilePath(RSC_DIR + "/" + filename, isSvg)
  }

  /** Тестируем isSvgValid() и isSvgFileValid(). */
  private def testFilePath(filepath: String, isSvg: Boolean): Unit = {
    // Тестируем isSvgValid():
    val is = getClass.getResourceAsStream(filepath)
    assert(is != null, "[[Test file NOT found]]")
    try {
      SvgUtil.isSvgValid(is)  shouldBe  isSvg
    } finally {
      is.close()
    }
    // тестируем isSvgFileValid():
    val fpath = getClass.getResource(filepath).getFile
    val f = new File(fpath)
    SvgUtil.isSvgFileValid(f) shouldBe  isSvg
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

  //it should "refuse raster graphics (1214545755807.jpg)" in {
  //  testFilePath("/util/img/1214545755807.jpg", isSvg = false)
  //}

  it should "refuse invalid svg (invalid_svg1_idented.svg)" in {
    testFile("invalid_svg1_idented.svg", isSvg = false)
  }

  it should "refuse valid xml non-svg file (uca.xml)" in {
    testFile("uca.xml", isSvg = false)
  }

}
