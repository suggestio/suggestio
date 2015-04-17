package util.showcase

import io.suggest.ym.model.common.MImgInfoMeta
import models.blk.{SzMult_t, szMulted}
import models.im._
import models.im.make.{IMakeArgs, IMaker, MakeResult}
import models.{ImgCrop, MImgSizeT}
import play.api.Play.{configuration, current}
import util.PlayMacroLogsImpl

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 13:27
 * Description: ShowCase Wide Render. Движок, который рендерит картинки в wide-режиме для широкоэкранной выдачи.
 * Этот функционал заточен под отображение карточек на всю ширину экрана, которая может вообще любой.
 * Фоновые изображения в таких случаях, как правило, уезжают за пределы wide-ширины из-за квантования ширин.
 */

object ScWideMaker extends IMaker with PlayMacroLogsImpl {

  import LOGGER._

  /** Желаемые ширИны широкого бэкграунда. */
  val WIDE_WIDTHS_PX: List[Int]  = getConfSzsRow("widths",  List(350, 500, 650, 850, 950, 1100, 1250, 1600, 2048) )

  private def getConfSzsRow(confKeyPart: String, default: => List[Int]): List[Int] = {
    configuration.getIntSeq(s"blocks.bg.wide.$confKeyPart.px")
      .fold(default) { _.toList.map(_.intValue) }
      .sorted
  }


  /** Подобрать ширину фоновой картинки на основе списка допустимых вариантов. */
  @tailrec def normWideBgSz(minWidth: Int,  acc: Int,  variants: Iterable[Int]): Int = {
    if (acc < minWidth && variants.nonEmpty) {
      normWideBgSz(minWidth, variants.head, variants.tail)
    } else {
      acc
    }
  }
  def normWideBgSz(minSz: Int, variants: Iterable[Int]): Int = {
    normWideBgSz(minSz, variants.head, variants = variants.tail)
  }
  def normWideWidthBgSz(minSz: Int): Int = {
    normWideBgSz(minSz, WIDE_WIDTHS_PX)
  }

  /** Попытаться подправить опциональный исходный кроп, если есть. Если нет, то фейл. */
  def getAbsCropOrFail(iik: MAnyImgT, wideWh: MImgSizeT)(implicit ec: ExecutionContext): Future[ImgCrop] = {
    iik.cropOpt match {
      case Some(crop0) =>
        val origWhFut = iik.original
          .getImageWH
          .map(_.get)  // Будет Future.failed при проблеме - так и надо.
        updateCrop0(crop0, wideWh, origWhFut)
      case None =>
        Future failed new NoSuchElementException("No default crop is here.")
    }
  }

  /** Поправить исходный кроп под wide-картинку. Гравитация производного кропа совпадает с исходным кропом. */
  def updateCrop0(crop0: ImgCrop, wideWh: MImgSizeT, origWhFut: Future[MImgSizeT])(implicit ec: ExecutionContext): Future[ImgCrop] = {
    origWhFut.map { origWh =>
      // Есть ширина-длина сырца. Нужно придумать кроп с центром как можно ближе к центру исходного кропа.
      // Результат должен изнутри быть вписан в исходник по размерам.
      val rszRatioV = origWh.height.toFloat / wideWh.height.toFloat
      val rszRatioH = origWh.width.toFloat / wideWh.width.toFloat
      val rszRatio  = Math.max(1.0F, Math.min(rszRatioH, rszRatioV))
      val w = szMulted(wideWh.width, rszRatio)
      val h = szMulted(wideWh.height, rszRatio)
      ImgCrop(
        width = w, height = h,
        // Для пересчета координат центра нужна поправка, иначе откропанное изображение будет за экраном.
        offX = translatedCropOffset(ocOffCoord = crop0.offX, ocSz = crop0.width, targetSz = w, oiSz = origWh.width, rszRatio = rszRatio),
        offY = translatedCropOffset(ocOffCoord = crop0.offY, ocSz = crop0.height, targetSz = h, oiSz = origWh.height, rszRatio = rszRatio)
      )
    }
  }
  /** Сделать из опционального исходнго кропа новый wide-кроп с указанием гравитации. */
  def getWideCropInfo(iik: MAnyImgT, wideWh: MImgSizeT)(implicit ec: ExecutionContext): Future[ImgCropInfo] = {
    getAbsCropOrFail(iik, wideWh)
      .map { crop1 => ImgCropInfo(crop1, isCenter = false) }
      .recover { case ex: Exception =>
        if (!ex.isInstanceOf[NoSuchElementException])
          warn(s"Failed to read image[${iik.fileName}] WH", ex)
        else
          debug("Failed to get abs crop: " + ex.getMessage)
        // По какой-то причине, нет возможности/необходимости сдвигать окно кропа. Делаем новый кроп от центра:
        val c = ImgCrop(width = wideWh.width, height = wideWh.height, 0, 0)
        ImgCropInfo(c, isCenter = true)
      }
  }


  /**
   * В одномерном пространстве (на одной оси, начинающийся с 0 и заканчивающейся length) определить начало отрезка,
   * центр которого будет как можно ближе к указанной координате центра, и иметь длину length.
   * @param centerCoord Координата желаемого центра отрезка.
   * @param segLen Длина отрезка.
   * @param axLen Длина оси.
   * @return Координата начала отрезка.
   *         Конец отрезка можно получить, сложив координату начала с length.
   */
  def centerNearestLineSeg1D(centerCoord: Float, segLen: Float, axLen: Float): Float = {
    // Координата середины оси:
    val axCenter = axLen / 2.0F
    // Половинная длина желаемого отрезка:
    val segSemiLen = segLen / 2.0F
    val resRaw = if (centerCoord == axCenter) {
      // Желаемый центр находится на середине оси. Вычитаем полудлину отрезка от координаты центра.
      (centerCoord - segSemiLen).toInt
    } else {
      // Центры не совпадают. В таком случае можно легко вычислить координату конца отрезка.
      val rightSegCoord = Math.min(centerCoord + segSemiLen, axLen)
      // Координата начала отрезка получается, если из координаты конца вычесть полную длину отрезку.
      (rightSegCoord - segLen).toInt
    }
    Math.max(0, resRaw)
  }

  /**
   * Трансляция одного кропа по одной оси на новый размер.
   * @param ocOffCoord Сдвиг по текущей оси исходного кропа. Например crop.offX для оси X.
   * @param ocSz Размер исходного кропа по текущей оси. Например crop.width для оси X.
   * @param targetSz Целевой размер нового кропа (новый width).
   * @param oiSz Полный размер изображения по текущей оси. origWh.width для оси Х.
   * @param rszRatio Используемый коэффициент масштабирования карточки и изображения размера задается здесь.
   * @return Новое значение offset'а для кропа.
   */
  def translatedCropOffset(ocOffCoord: Int, ocSz: Int, targetSz: Int, oiSz: Int, rszRatio: SzMult_t): Int = {
    val newCoordFloat = centerNearestLineSeg1D(
      centerCoord = (ocOffCoord + ocSz / 2) / rszRatio,
      segLen = targetSz.toFloat,
      axLen = oiSz / rszRatio
    )
    newCoordFloat.toInt
  }


  /**
   * Собрать ссылку на wide-картинку и сопутствующие метаданные.
   * @param args Контейнер с аргументами вызова.
   * @return Фьючерс с результатом.
   */
  override def icompile(args: IMakeArgs)(implicit ec: ExecutionContext): Future[MakeResult] = {
    import args._
    val iik = MImg( img.filename )
    val iikOrig = iik.original
    // Собираем хвост параметров сжатия.
    val devScreen = args.devScreenOpt getOrElse DevScreen.default
    val pxRatio = devScreen.pixelRatio
    // Нужно вычислить размеры wide-версии оригинала. Используем szMult для вычисления высоты.
    val tgtHeightCssRaw = blockMeta.height * szMult
    val tgtHeightReal = szMulted(tgtHeightCssRaw, pxRatio.pixelRatio)
    // Ширину экрана квантуем, получая ширину картинки.
    val cropWidthCssPx = normWideWidthBgSz(devScreen.width)
    val cropWidth = szMulted(cropWidthCssPx, pxRatio.pixelRatio)
    // Запустить сбор инфы по кропу.
    val wideWh = MImgInfoMeta(height = tgtHeightReal, width = cropWidth)
    val cropInfoFut = getWideCropInfo(iik, wideWh)
    // Начинаем собирать список трансформаций по ресайзу:
    val bgc = pxRatio.bgCompression
    val imOps0 = List[ImOp](
      // 2015.mar.11: Вписать откропанное изображение в примерно необходимые размеры. До это кроп был внутри ресайза.
      AbsResizeOp( MImgInfoMeta(height = tgtHeightReal, width = 0) /*, Seq(ImResizeFlags.FillArea)*/ ),
      ImFilters.Lanczos,
      StripOp,
      ImInterlace.Plane,
      bgc.chromaSubSampling,
      bgc.imQualityOp
    )
    // Нужно брать кроп отн.середины только когда нет исходного кропа и реально широкая картинка. Иначе надо транслировать исходный пользовательский кроп в этот.
    val imOps2Fut = cropInfoFut
      .map { cropInfo =>
        if (cropInfo.isCenter) {
          warn(s"Failed to read image[${iikOrig.fileName}] WH")
          // По какой-то причине, нет возможности/необходимости сдвигать окно кропа. Делаем новый кроп от центра:
          ImGravities.Center ::  AbsCropOp(cropInfo.crop) ::  imOps0
        } else {
          AbsCropOp(cropInfo.crop) :: imOps0
        }
      }
    // Вычислить размер картинки в css-пикселях.
    val szCss = MImgInfoMeta(
      height = tgtHeightCssRaw.toInt,
      width  = cropWidthCssPx
    )
    // Дождаться результатов рассчета картинки и вернуть контейнер с результатами.
    for {
      imOps2 <- imOps2Fut
    } yield {
      MakeResult(
        szCss       = szCss,
        szReal      = wideWh,
        dynCallArgs = iik.copy(dynImgOps = imOps2),
        isWide      = true
      )
    }
  }

}
