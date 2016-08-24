package util.img.detect.main

import java.io.File
import java.nio.file.Files
import java.text.ParseException

import com.google.inject.{Singleton, Inject}
import models.im._
import models.mproj.ICommonDi
import org.im4java.core.{ConvertCmd, IMOperation}
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.14 19:25
 * Description: Утиль для определения основных цветов на изображении.
 * База для работы: convert 12636786604889.jpg -gravity Center -crop 50%\! -gamma 2.0 -quantize Lab  +dither -colors 8 -format "%c" histogram:info:
 */
@Singleton
class MainColorDetector @Inject() (
  mImgs3        : MImgs3,
  mCommonDi     : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi.{configuration, ec}

  /** Дефолтовое значение размера промежуточной палитры цветовой гистограммы. */
  val PALETTE_MAX_COLORS_DFLT = configuration.getInt("mcd.palette.colors.max.dflt") getOrElse 8


  /**
   * Отрендерить гистограмму по указанной картинке в указанный файл.
   * @param imgFilepath Путь в ФС до картинки.
   * @param outFilepath Путь к до файла, в который надо сохранить гистограмму.
   * @param maxColors Необязательный размер палитры и гистограммы.
   */
  def convertToHistogram(imgFilepath: String, outFilepath: String, maxColors: Int, preImOps: Seq[ImOp] = Nil): Future[_] = {
    // Запускаем команду,
    val op = new IMOperation()
    op.addImage(imgFilepath)
    preImOps.foreach { imOp =>
      imOp.addOperation(op)
    }
    op.gravity("Center")
    op.crop().addRawArgs("50%!") // -crop 50%\!
    op.gamma(2.0)
    op.quantize("Lab")
    op.p_dither()
    op.colors(8)
    op.format("%c")
    op.addRawArgs("histogram:info:" + outFilepath)
    val cmd = new ConvertCmd
    cmd.setAsyncMode(true)
    val listener = new Im4jAsyncSuccessProcessListener
    cmd.addProcessEventListener(listener)

    val loggingEnabled = LOGGER.underlying.isDebugEnabled
    val tstampStart = if (loggingEnabled) System.currentTimeMillis() else 0L
    cmd.run(op)
    val resFut = listener.future
    if (loggingEnabled) {
      resFut.onComplete { res =>
        debug(s"convertToHistogram($tstampStart): convert to histogram finished.\n img=$imgFilepath\n out=$outFilepath\n maxColors=$maxColors\n Took ${System.currentTimeMillis() - tstampStart} ms.\n Cmd was: ${op.toString}\n Result: $res")
      }
    }
    resFut
  }
  

  /**
   * Картинка лежит в файле. Нужно определить у неё палитру основных цветов.
   * Для этого квантуем цвета в пространстве CIE Lab, чтобы визуально близкие цвета принадлежали к одному кванту.
   * @param img Исходная картинка.
   */
  def detectFilePaletteUnsorted(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT,
                        preImOps: Seq[ImOp] = Nil): Future[List[HistogramEntry]] = {
    // Создаём временный файл для сохранения выхлопа convert histogram:info:
    val resultFile = File.createTempFile(getClass.getSimpleName, ".txt")
    val resultFut = convertToHistogram(img.getAbsolutePath, resultFile.getAbsolutePath, maxColors, preImOps) map { _ =>
      // Читаем и парсим из файла гистограмму.
      val pr = HistogramParsers.parseFromFile(resultFile)
      pr getOrElse {
        val histContent = Files.readAllBytes(resultFile.toPath)
        val msgSb = new StringBuilder(histContent.length + 128,  "Failed to understand histogram file: \n")
          .append(pr)
          .append('\n')
          .append(" Histogram file content was:\n")
          .append( new String(histContent) )
        val msg = msgSb.toString()
        if (suppressErrors) {
          warn(msg )
          Nil
        } else {
          throw new ParseException(msg, -1)
        }
      }
    }
    resultFut.onComplete { _ =>
      resultFile.delete()
    }
    if (suppressErrors) {
      resultFut recoverWith {
        case ex: Exception =>
          warn(s"Failed to extract palette from picture ${img.getAbsolutePath}", ex)
          Future.successful(Nil)
      }
    }
    resultFut
  }

  /**
   * Определить основной цвет на картинке.
   * @param img Файл картинки.
   * @param suppressErrors Подавлять ошибки?
   * @param maxColors Необязательный размер промежуточной палитры и гистограммы.
   * @return None при ошибке и suppressErrors или если картинка вообще не содержит цветов.
   */
  def detectFileMainColor(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT,
                          preImOps: Seq[ImOp] = Nil): Future[Option[HistogramEntry]] = {
    detectFilePaletteUnsorted(img, suppressErrors, maxColors, preImOps) map { hist =>
      if (hist.isEmpty) {
        debug("Detected colors pallette is empty. img = " + img.getAbsolutePath)
        None
      } else {
        val result = hist.maxBy(_.frequency)
        Some(result)
      }
    }
  }


  /** Дистанция между точками в трехмерном целочисленном пространстве цветов. Считаем по теореме Пифагора.
    * @param p1 Точка 1.
    * @param p2 Точка 2.
    * @param exp Значение показателя степеней. В теореме Пифагора используется степень и корень по показателю 2,
    *            но можно задать любой другой.
    * @return Расстояние между точками в 3-мерном пространстве.
    */
  def colorDistance3D(p1: ColorPoint3D, p2: ColorPoint3D, exp: Double = 2.0): Double = {
    val expDst = Math.pow(p2.x - p1.x, exp)  +  Math.pow(p2.y - p1.y, exp)  +  Math.pow(p2.z - p1.z, exp)
    Math.pow(expDst, 1.0 / exp)
  }


  /** Результат вызова prepareImg(). */
  case class PrepareImgResult(localOpt: Option[MLocalImg], imOps: Seq[ImOp])

  /**
   * Подготовить указанную картинку к извлечению гистограммы.
   * @param bgImg4s Данные по картинке.
   * @return Фьючерс с данными картинки.
   */
  def prepareImg(bgImg4s: MImgT): Future[PrepareImgResult] = {
    lazy val logPrefix = s"prepareImg(${bgImg4s.fileName}): "

    // toLocalImg не существовует обычно вообще (ибо голый orig [+ crop]). Поэтому сразу ищем оригинал, но не теряя надежды.
    val localOrigImgFut = mImgs3.toLocalImg(bgImg4s.original)

    // Всё-таки ищем отропанный результат.
    var localImg2Fut = mImgs3.toLocalImg(bgImg4s)
      .filter(_.exists(_.isExists))
      .map { v => PrepareImgResult(v, Seq.empty[ImOp]) }

    // Если исходная картинка - чистый оригинал, то отрабатывать отсутствие произодной картинки не требуется.
    if (bgImg4s.hasImgOps) {
      // Если исходная картинка - обрезок, то можно изъять операции из исходной картинки и повторить их на оригинале вместе с генерацией гистограммы.
      localImg2Fut = localImg2Fut.recoverWith {
        case ex: NoSuchElementException =>
          val resFut = localOrigImgFut
            .map { v => PrepareImgResult(v, bgImg4s.dynImgOps) }
          trace(s"${logPrefix}Derived img not exists. Re-applying ${bgImg4s.dynImgOps.size} IM ops to original: ${bgImg4s.dynImgOps}")
          resFut
      }
    }
    // Подавляем возможные исключения.
    localImg2Fut recover {
      case ex: Throwable =>
        warn(s"${logPrefix}Failed to find img requested.", ex)
        PrepareImgResult(None, Nil)
    }
  }

  /**
   * Поиск "главного" цвета для указанной (через указатель) картинки.
   * @param bgImg4s Исходная картинка.
   * @return Фьючерс с результатом работы.
   */
  def detectColorFor(bgImg4s: MImgT): Future[ImgBgColorUpdateAction] = {
    lazy val logPrefix = s"detectColorFor(${bgImg4s.fileName}): "
    prepareImg(bgImg4s) flatMap {
      case PrepareImgResult(Some(localImg), preImOps) =>
        detectFileMainColor(localImg.file, suppressErrors = true, preImOps = preImOps) map { heOpt =>
          val result = he2updateAction(heOpt)
          trace(s"${logPrefix}Detected color info for already saved orig img: $result")
          result
        }

      // Почему-то нет картинки.
      case _ =>
        warn(s"${logPrefix}Img not found anywhere: ${bgImg4s.fileName}")
        Future successful Keep
    }
  }

  /**
   * Получить палитру цветов для указанной картинки.
   * @param bgImg4s Данные по исходной картинке.
   * @param maxColors Макс.размер результирующей палитры.
   * @return Фьючерс с гистограммой, где самый частый в начале, и далее по убыванию.
   */
  def detectPaletteFor(bgImg4s: MImgT, maxColors: Int = PALETTE_MAX_COLORS_DFLT): Future[Histogram] = {
    prepareImg(bgImg4s) flatMap {
      case PrepareImgResult(Some(localImg), preImOps) =>
        detectFilePaletteUnsorted(localImg.file, suppressErrors = true, preImOps = preImOps, maxColors = maxColors)
          .map { hist => Histogram( hist.sortBy(v => -v.frequency) ) }
      case other =>
        Future failed new IllegalArgumentException("Failed to extract palette. prepareImg() result is " + other)
    }
  }

  /** Конвертация выхлопа detectMainColor() в инструкцию по обновлению карты цветов. */
  def he2updateAction(heOpt: Option[HistogramEntry], default: ImgBgColorUpdateAction = Keep): ImgBgColorUpdateAction = {
    if (heOpt.isDefined)
      Update(heOpt.get.colorHex)
    else
      default
  }

}




