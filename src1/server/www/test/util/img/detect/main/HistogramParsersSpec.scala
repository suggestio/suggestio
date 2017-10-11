package util.img.detect.main

import java.io.{File, PrintWriter}

import io.suggest.color.{MColorData, MRgb}
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 14:49
 * Description: Тесты для проверки работы парсеров IM-гистограмм и детектора палитры картинки.
 */

class HistogramParsersSpec extends PlaySpec {

  private val histogramParsers = new HistogramParsers

  import histogramParsers._

  private val parsedSubstr = "] parsed: "

  private def parseLine(l: String, resultExpected: MColorData): Unit = {
    val pr = parseAll(LINE_PARSER, l)
    pr.toString   must include (parsedSubstr)
    pr.get        mustBe resultExpected
  }

  private def _histogramEntry(freq: Long, colorHex: String, rgb: MRgb): MColorData = {
    MColorData(
      code    = colorHex,
      rgb     = Some(rgb),
      count   = Some(freq)
    )
  }


  "LINE_PARSER" must {
    // используем include вместо pr.successful mustBe true, чтобы на экран напечаталось сообщение об ошибке, а не просто экзепшен.
    "parse usual JPEG histogram line" in {
      parseLine("    104654: (252,220, 29) #FCDC1D srgb(252,220,29)",     _histogramEntry(104654L, "FCDC1D", MRgb(252, 220, 29)) )
    }
    "parse usual JPEG hist.line with \\n" in {
      parseLine("    231983: (170,162, 95) #AAA25F srgb(170,162,95)\n",   _histogramEntry(231983L, "AAA25F", MRgb(170, 162, 95)) )
    }
    "parse anothen unpopular histogram line" in {
      parseLine("         1: ( 94, 70, 60) #5E463C srgb(94,70,60)",       _histogramEntry(1L, "5E463C", MRgb(94, 70, 60)) )
    }
    "parse 16272 line" in {
      parseLine("    16272: (249,232,199) #F9E8C7 srgb(249,232,199)",     _histogramEntry(16272, "F9E8C7", MRgb(249, 232, 199)) )
    }
    "parse 1136 line" in {
      parseLine("      1136: ( 17, 24,  9) #111809 srgb(17,24,9)",        _histogramEntry(1136, "111809", MRgb(17, 24, 9)) )
    }

    "parse SRGBA integer line" in {
      parseLine("    1: (  0, 64,193,  0) #0040C100 srgba(0,64,193,0)",   _histogramEntry(1, "0040C1", MRgb(0, 64, 193)) )
    }
    "parse SRGBA line with floating alpha" in {
      parseLine("    1: (  0, 64,193,255) #0040C1FF srgba(0,64,193,0.999893)",   _histogramEntry(1, "0040C1", MRgb(0, 64, 193)) )
    }

    // 2015.aug.10: На картинки с белым фоном и небольшим логотипом в центре возникла проблема.
    "parse code description like gray(255)" in {
      parseLine("    212300: (255,255,255) #FFFFFF gray(255)",            _histogramEntry(212300, "FFFFFF", MRgb(255,255,255)))
    }
  }


  /** Пример выхлопа гистограммы IM. */
  private def hist1 = {
    """
      |     21689: ( 42, 45, 12) #2A2D0C yellow
      |      5487: ( 54, 42, 18) #362A12 srgb(54,42,18)
      |     83956: (100, 96, 33) #646021 double cyan
      |    176410: (144,139, 91) #908B5B srgb(144,139,91)
    """.stripMargin
  }

  /** Распарсенный пример выхлопа hist1. */
  private def hist1parsed = List(
    _histogramEntry(21689,  "2A2D0C", MRgb(42, 45, 12)),
    _histogramEntry(5487,   "362A12", MRgb(54, 42, 18)),
    _histogramEntry(83956,  "646021", MRgb(100, 96, 33)),
    _histogramEntry(176410, "908B5B", MRgb(144, 139, 91))
  )


  // TODO PNG или иной формат с прозрачным цветом.
  private def HIST2 = {
    """
      |    102691: (250,210, 14,  0) #FAD20E00 srgba(250,210,14,0.000366217)
      |    280189: (255,211,  0,255) #FFD300FF srgba(255,211,0,0.999893)
    """.stripMargin
  }
  private def HIST2_PARSED = List(
    _histogramEntry(102691, "FAD20E", MRgb(250, 210, 14)),
    _histogramEntry(280189, "FFD300", MRgb(255, 211, 0))
  )

  private def _testParseMultiline(text: String, res: List[MColorData]) = {
    val pr = parseAll( MULTILINE_PARSER, text )
    pr.successful mustBe true
    pr.get mustBe res
  }

  "MULTILINE_PARSER" must {

    "parse hist1: normal multiline histogram JPEG text" in {
      _testParseMultiline(hist1, hist1parsed)
    }

    "parse HIST2: histogram from PNG with alpha-channel" in {
      _testParseMultiline(HIST2, HIST2_PARSED)
    }

  }



  "parseFromFile()" must {
    "parse multiline histogram text from temporary file" in {
      val tempFile = File.createTempFile(classOf[MainColorDetectorSpec].getSimpleName, ".txt")
      // Хелпер для записи строки в файл.
      def writeText(text: String) {
        val out = new PrintWriter(tempFile)
        try {
          out.println(text)
        } finally {
          out.close()
        }
      }
      try {
        writeText(hist1)
        val pr = parseFromFile(tempFile)
        pr.successful mustBe true
        pr.get mustBe hist1parsed

      } finally {
        tempFile.delete()
      }
    }
  }

}
