package util.img

import java.io.{PrintWriter, File}
import models.im.{RGB, HistogramEntry}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.scalatestplus.play._
import play.api
import play.api.GlobalSettings
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, FakeApplication}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.14 11:13
 * Description: Тесты для проверки работы парсеров IM-гистограмм и детектора палитры картинки.
 */

class HistogramParsersSpec extends PlaySpec {
  import HistogramParsers._

  private val parsedSubstr = "] parsed: "

  private def parseLine(l: String, resultExpected: HistogramEntry): Unit = {
    val pr = parseAll(LINE_PARSER, l)
    pr.toString   must include (parsedSubstr)
    pr.get        mustBe resultExpected
  }


  "LINE_PARSER" must {
    s"parse into ${HistogramEntry.getClass.getSimpleName} every histogram line" in {
      // используем include вместо pr.successful mustBe true, чтобы на экран напечаталось сообщение об ошибке, а не просто экзепшен.
      parseLine("    104654: (252,220, 29) #FCDC1D srgb(252,220,29)",     HistogramEntry(104654L, "FCDC1D", RGB(252, 220, 29)) )
      parseLine("    231983: (170,162, 95) #AAA25F srgb(170,162,95)\n",   HistogramEntry(231983L, "AAA25F", RGB(170, 162, 95)) )
      parseLine("         1: ( 94, 70, 60) #5E463C srgb(94,70,60)",       HistogramEntry(1L, "5E463C", RGB(94, 70, 60)) )
      parseLine("    16272: (249,232,199) #F9E8C7 srgb(249,232,199)",     HistogramEntry(16272, "F9E8C7", RGB(249, 232, 199)) )
      parseLine("      1136: ( 17, 24,  9) #111809 srgb(17,24,9)",        HistogramEntry(1136, "111809", RGB(17, 24, 9)) )

      parseLine("    1: (  0, 64,193,  0) #0040C100 srgba(0,64,193,0)",   HistogramEntry(1, "0040C1", RGB(0, 64, 193)) )
    }
  }


  /** Пример выхлопа гистограммы IM. */
  private val hist1 = {
    """
      |     21689: ( 42, 45, 12) #2A2D0C yellow
      |      5487: ( 54, 42, 18) #362A12 srgb(54,42,18)
      |     83956: (100, 96, 33) #646021 double cyan
      |    176410: (144,139, 91) #908B5B srgb(144,139,91)
    """.stripMargin
  }

  /** Распарсенный пример выхлопа hist1. */
  private val hist1parsed = List(
    HistogramEntry(21689,  "2A2D0C", RGB(42, 45, 12)),
    HistogramEntry(5487,   "362A12", RGB(54, 42, 18)),
    HistogramEntry(83956,  "646021", RGB(100, 96, 33)),
    HistogramEntry(176410, "908B5B", RGB(144, 139, 91))
  )


  "MULTILINE_PARSER" must {
    "parse multiline histogram text" in {
      val pr = parseAll(MULTILINE_PARSER, hist1)
      pr.successful mustBe true
      pr.get mustBe hist1parsed
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


/**
 * Тесты для детектора палитры.
 * Для определения цвета используются картинки с явным преобладанием какого-то цвета.
 * При тесте измеряется дистанция от найденного цвета до желаемых цветов.
 */
class MainColorDetectorSpec extends PlaySpec with OneAppPerSuite with FutureAwaits with DefaultAwaitTimeout {
  import MainColorDetector._

  /** Путь к ресурсом в рамках classpath. Ресурсы лежат внутри sioweb21/test/resources/. */
  private val RSC_BASEPATH = "/util/img/"

  /** Максимальная погрешность при сравнении желаемого цвета с найденным (макс. цветовое расстояние в 3д пространстве). */
  private val maxColorDistance: Double = app.configuration.getDouble("mcd.test.distance.error.max") getOrElse 15.0


  "detectFileMainColor()" must {

    /** Метод, производящий тестирование одного файла и проверяет конечные результаты. */
    def mkTest(filename: String, mainColorsHex: List[String]): Unit = {
      // Копируем файл из jar classpath в /tmp/. Так картинка станет гарантированно доступна для внешней IM.
      // Рецепт копирования взят из http://stackoverflow.com/a/10308305
      val rscFilepath = RSC_BASEPATH + filename
      val fileExt = FilenameUtils.getExtension(filename)    // взято из http://stackoverflow.com/a/16202288
      val rscUrl = getClass.getResource(rscFilepath)
      val tmpImgFile = File.createTempFile(classOf[MainColorDetectorSpec].getSimpleName, "." + fileExt)
      FileUtils.copyURLToFile(rscUrl, tmpImgFile)
      val detectResultFut = detectFileMainColor(tmpImgFile, suppressErrors = false, maxColors = 8)
      detectResultFut onComplete { case _ =>
        tmpImgFile.delete()
      }
      val detectResult = await(detectResultFut)
      detectResult.nonEmpty  mustBe  true
      val dmchRgb = detectResult.get.rgb
      mainColorsHex foreach { mch =>
        val mchRgb = RGB(mch)
        val distance = MainColorDetector.colorDistance3D(dmchRgb, mchRgb)
        distance mustBe <= (maxColorDistance)
      }
    }

    "detect main color pallette for 1214545755807.jpg" in {
      mkTest("1214545755807.jpg", List("ECD50C"))
    }

    "detect main color for 1224517380638.jpg" in {
      mkTest("1224517380638.jpg", List("CFC9C2"))
    }

  }


  /** Штатный Global производит долгую инициализацию, которая нам не нужна. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )

}


