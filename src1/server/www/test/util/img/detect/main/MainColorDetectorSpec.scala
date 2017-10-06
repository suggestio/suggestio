package util.img.detect.main

import java.io.File

import functional.OneAppPerSuiteNoGlobalStart
import models.im.MRgb
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.scalatestplus.play._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.14 11:13
 * Description: Тесты для детектора палитры.
 * Для определения цвета используются картинки с явным преобладанием какого-то цвета.
 * При тесте измеряется дистанция от найденного цвета до желаемых цветов.
 */
class MainColorDetectorSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart with FutureAwaits with DefaultAwaitTimeout {

  private lazy val mainColorDetector = app.injector.instanceOf[MainColorDetector]

  implicit private lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  /** Путь к ресурсом в рамках classpath. Ресурсы лежат внутри sioweb21/test/resources/. */
  private val RSC_BASEPATH = "/util/img/"

  /** Максимальная погрешность при сравнении желаемого цвета с найденным (макс. цветовое расстояние в 3д пространстве). */
  private val maxColorDistance: Double = app.configuration.getOptional[Double]("mcd.test.distance.error.max") getOrElse 15.0


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
      val detectResultFut = mainColorDetector.detectFileMainColor(tmpImgFile, suppressErrors = false, maxColors = 8)
      for (_ <- detectResultFut) {
        tmpImgFile.delete()
      }
      val detectResult = await(detectResultFut)
      detectResult.nonEmpty  mustBe  true
      val dmchRgb = detectResult.get.rgb
      val dmchRgbXyz = dmchRgb.toCoord3d
      for (mch <- mainColorsHex) {
        val mchRgb = MRgb(mch)
        val distance = dmchRgbXyz distance3dTo mchRgb.toCoord3d
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

}


