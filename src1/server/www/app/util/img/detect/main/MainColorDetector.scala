package util.img.detect.main

import java.io.File
import java.nio.file.Files
import java.text.ParseException
import javax.inject.Inject

import io.suggest.color.{MColorData, MHistogram}
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.mproj.ICommonDi
import org.im4java.core.{ConvertCmd, IMOperation}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.14 19:25
 * Description: Утиль для определения основных цветов на изображении.
 * База для работы: convert 12636786604889.jpg -gravity Center -crop 50%\! -gamma 2.0 -quantize Lab  +dither -colors 8 -format "%c" histogram:info:
 */
class MainColorDetector @Inject() (
                                    mCommonDi     : ICommonDi
                                  )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, cacheApiUtil}
  import mCommonDi.current.injector

  private lazy val mAnyImgs = injector.instanceOf[MAnyImgs]
  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
  private lazy val im4jAsyncUtil = injector.instanceOf[Im4jAsyncUtil]



  /** Дефолтовое значение размера промежуточной палитры цветовой гистограммы. */
  private def PALETTE_MAX_COLORS_DFLT = 8


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
    val listener = new im4jAsyncUtil.Im4jAsyncSuccessProcessListener
    cmd.addProcessEventListener(listener)

    val loggingEnabled = LOGGER.underlying.isDebugEnabled
    val tstampStart = if (loggingEnabled) System.currentTimeMillis() else 0L
    cmd.run(op)
    val resFut = listener.future
    if (loggingEnabled) {
      resFut.onComplete { res =>
        LOGGER.debug(s"convertToHistogram($tstampStart): convert to histogram finished.\n img=$imgFilepath\n out=$outFilepath\n maxColors=$maxColors\n Took ${System.currentTimeMillis() - tstampStart} ms.\n Cmd was: ${op.toString}\n Result: $res")
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
                                preImOps: Seq[ImOp] = Nil): Future[List[MColorData]] = {
    // Создаём временный файл для сохранения выхлопа convert histogram:info:
    val resultFile = File.createTempFile(getClass.getSimpleName, ".txt")

    val resultFut = for {
      _ <- convertToHistogram(img.getAbsolutePath, resultFile.getAbsolutePath, maxColors, preImOps)
    } yield {
      // Читаем и парсим из файла гистограмму.
      val pr = (new HistogramParsers)
        .parseFromFile( resultFile )

      pr.getOrElse {
        val histContent = Files.readAllBytes( resultFile.toPath )
        val msgSb = new StringBuilder(histContent.length + 128,  "Failed to understand histogram file: \n")
          .append(pr)
          .append('\n')
          .append(" Histogram file content was:\n")
          .append( new String(histContent) )
        val msg = msgSb.toString()
        if (suppressErrors) {
          LOGGER.warn(msg)
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
      resultFut.recoverWith {
        case ex: Exception =>
          LOGGER.warn(s"Failed to extract palette from picture ${img.getAbsolutePath}", ex)
          Future.successful(Nil)
      }
    } else {
      resultFut
    }
  }

  /**
    * Определить основной цвет на картинке.
    * Метод используется только для тестов.
    *
    * @param img Файл картинки.
    * @param suppressErrors Подавлять ошибки?
    * @param maxColors Необязательный размер промежуточной палитры и гистограммы.
    * @return None при ошибке и suppressErrors или если картинка вообще не содержит цветов.
    */
  def detectFileMainColor(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT,
                          preImOps: Seq[ImOp] = Nil): Future[Option[MColorData]] = {
    for {
      hist <- detectFilePaletteUnsorted(img, suppressErrors, maxColors, preImOps)
    } yield {
      if (hist.isEmpty) {
        LOGGER.debug("Detected colors pallette is empty. img = " + img.getAbsolutePath)
        None
      } else {
        val result = hist.maxBy(_.count.getOrElse(-1L))
        Some(result)
      }
    }
  }


  /** Результат вызова prepareImg(). */
  case class PrepareImgResult(localOpt: Option[MLocalImg], imOps: Seq[ImOp])

  /**
   * Подготовить указанную картинку к извлечению гистограммы.
   * @param bgImg4s Данные по картинке.
   * @return Фьючерс с данными картинки.
   */
  def prepareImg(bgImg4s: MAnyImgT): Future[PrepareImgResult] = {
    lazy val logPrefix = s"prepareImg(${bgImg4s.dynImgId.fileName}): "

    // toLocalImg не существовует обычно вообще (ибо голый orig [+ crop]). Поэтому сразу ищем оригинал, но не теряя надежды.
    val localOrigImgFut = mAnyImgs.toLocalImg(bgImg4s.original)

    // Всё-таки ищем отропанный результат.
    var localImg2Fut = for {
      locImgOpt <- mAnyImgs.toLocalImg(bgImg4s)
      if locImgOpt.exists { locImg =>
        mLocalImgs.isExists(locImg)
      }
    } yield {
      PrepareImgResult(locImgOpt, Seq.empty[ImOp])
    }

    // Если исходная картинка - чистый оригинал, то отрабатывать отсутствие произодной картинки не требуется.
    if (bgImg4s.dynImgId.hasImgOps) {
      // Если исходная картинка - обрезок, то можно изъять операции из исходной картинки и повторить их на оригинале вместе с генерацией гистограммы.
      localImg2Fut = localImg2Fut.recoverWith {
        case _: NoSuchElementException =>
          val resFut = for (v <- localOrigImgFut) yield {
            PrepareImgResult(v, bgImg4s.dynImgId.dynImgOps)
          }
          LOGGER.trace(s"${logPrefix}Derived img not exists. Re-applying ${bgImg4s.dynImgId.dynImgOps.size} IM ops to original: ${bgImg4s.dynImgId.dynImgOps}")
          resFut
      }
    }
    // Подавляем возможные исключения.
    localImg2Fut.recover {
      case ex: Throwable =>
        LOGGER.warn(s"${logPrefix}Failed to find img requested.", ex)
        PrepareImgResult(None, Nil)
    }
  }

  /**
   * Получить палитру цветов для указанной картинки.
   * @param bgImg4s Данные по исходной картинке.
   * @param maxColors Макс.размер результирующей палитры.
   * @return Фьючерс с гистограммой, где самый частый в начале, и далее по убыванию.
   */
  def detectPaletteFor(bgImg4s: MAnyImgT, maxColors: Int = PALETTE_MAX_COLORS_DFLT): Future[MHistogram] = {
    prepareImg(bgImg4s).flatMap {
      case PrepareImgResult(Some(localImg), preImOps) =>
        for {
          hist <- detectFilePaletteUnsorted(
            img             = mLocalImgs.fileOf(localImg),
            suppressErrors  = true,
            preImOps        = preImOps,
            maxColors       = maxColors
          )
        } yield {
          MHistogram(
            colors = hist.sortBy(v => -v.count.getOrElse(0L))
          )
        }

      case other =>
        val ex = new IllegalArgumentException("Failed to extract palette. prepareImg() result is " + other)
        Future.failed(ex)
    }
  }


  import scala.concurrent.duration._

  private def CACHE_COLOR_HISTOGRAM_DURATION = 10.seconds

  /** Закэшировать выполнение detectPaletteFor(). */
  def cached(bgImg4s: MAnyImgT)(detectF: => Future[MHistogram]): Future[MHistogram] = {
    cacheApiUtil.getOrElseFut(
      key         = "mcd." + bgImg4s.dynImgId.origNodeId + ".hist",
      expiration  = CACHE_COLOR_HISTOGRAM_DURATION
    )(detectF)
  }

}

