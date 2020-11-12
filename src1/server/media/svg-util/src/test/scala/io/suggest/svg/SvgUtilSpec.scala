package io.suggest.svg

import java.io.File

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 13:37
 * Description: Тесты для SvgUtil.
 */
class SvgUtilSpec extends AnyFlatSpec {

  private val RSC_DIR = ""

  private def testFile(filename: String, isSvg: Boolean, svgDeepTest: Boolean = true): Unit = {
    testFilePath(RSC_DIR + "/" + filename, isSvg, svgDeepTest)
  }

  /** Тестируем isSvgValid() и isSvgFileValid(). */
  private def testFilePath(filePath: String, isSvg: Boolean, svgDeepTest: Boolean = true): Unit = {
    // Тестируем isSvgValid():
    val is = getClass.getResourceAsStream(filePath)
    val url = getClass.getResource(filePath).toString
    assert(is != null, "[[Test file NOT found]]")
    try {
      val docOpt = SvgUtil.safeOpenWrap(
        SvgUtil.open(is, url),
        filePath,
      )
      docOpt.nonEmpty shouldBe isSvg

      // Для SVG-документов попытаться построить дерево.
      if (svgDeepTest && isSvg) {
        val doc = docOpt.get
        val gvt = SvgUtil.buildGvt(doc)
        assert(gvt != null)

        val bounds = gvt.getBounds
        assert(bounds.getWidth > 0)
        assert(bounds.getHeight > 0)
      }
    } finally {
      is.close()
    }

    // тестируем isSvgFileValid():
    val fpath = getClass.getResource(filePath).getFile
    val f = new File(fpath)
    SvgUtil.safeOpenWrap( SvgUtil.open(f), f.toString ).nonEmpty  shouldBe  isSvg
  }



  "isSvgValid()" should "see a valid idented svg file: valid_svg1_idented.svg" in {
    testFile("valid_svg1_idented.svg", isSvg = true)
  }

  it should "accept shrinked version of previous file too (valid_svg1_shrinked.svg)" in {
    testFile("valid_svg1_shrinked.svg", isSvg = true)
  }

  it should "accept valid svg file: valid_svg2_shrinked.svg" in {
    // TODO Что-то не так с рендером этого файла: The attribute "offset" of the element <stop> is required
    testFile("valid_svg2_shrinked.svg", isSvg = true, svgDeepTest = false)
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

  it should "accept SVG-signature of Ekaterina II" in {
    testFile("Catherine_The_Great_Signature.svg", isSvg = true)
  }

  it should "accept SVG world map of voltage frequencies" in {
    testFile("world-map-voltage-frequencies.svg", isSvg = true)
  }

}
