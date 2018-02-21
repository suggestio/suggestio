package util.showcase

import javax.inject.{Inject, Singleton}

import io.suggest.ad.blk.{BlockPaddings, BlockWidths}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.img.crop.MCrop
import io.suggest.sc.tile.TileConstants
import io.suggest.util.logs.MacroLogsImpl
import models.blk.{szMulted, szMultedF, szRounded}
import models.im._
import models.im.make.{IImgMaker, MImgMakeArgs, MakeResult}
import models.mproj.ICommonDi
import util.img.ImgMakerUtil

import scala.annotation.tailrec
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 13:27
 * Description: ShowCase Wide Render. Движок, который рендерит картинки в wide-режиме для широкоэкранной выдачи.
 * Этот функционал заточен под отображение карточек на всю ширину экрана, которая может вообще любой.
 * Фоновые изображения в таких случаях, как правило, уезжают за пределы wide-ширины из-за квантования ширин.
 */
@Singleton
class ScWideMaker @Inject() (
                              mAnyImgs      : MAnyImgs,
                              imgMakerUtil  : ImgMakerUtil,
                              mCommonDi     : ICommonDi
                            )
  extends IImgMaker
  with MacroLogsImpl
{

  import mCommonDi._

  /** Желаемые ширИны широкого бэкграунда.
    * 2018-02-06 Уменьшение списка размеров: 950 и 1250 убрано.
    * Макс.ширина - 1260 -- это
    */
  val WIDE_WIDTHS_PX: List[Int] = List(350, 500, 650, 850, /*950,*/
    {
      // Макс.ширина равна макс.ширине плитки.
      val cols = TileConstants.CELL300_COLUMNS_MAX
      BlockWidths.NORMAL.value * cols + BlockPaddings.Bp20.value * (cols - 1)
    }
  )


  /** Подобрать ширину фоновой картинки на основе списка допустимых вариантов. */
  @tailrec private def normWideBgSz(minWidth: Int,  acc: Int,  variants: Iterable[Int]): Int = {
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
  def getAbsCropOrFail(iik: MAnyImgT, wideWh: ISize2di): Future[MCrop] = {
    iik.dynImgId.cropOpt.fold [Future[MCrop]] {
      Future.failed( new NoSuchElementException("No default crop is here.") )
    } { crop0 =>
      for {
        origWhOpt <- mAnyImgs.getImageWH( iik.original )
      } yield {
        // Будет Future.failed при проблеме - так и надо.
        updateCrop0(crop0, wideWh, origWhOpt.get)
      }
    }
  }

  /** Поправить исходный кроп под wide-картинку. Гравитация производного кропа совпадает с исходным кропом. */
  def updateCrop0(crop0: MCrop, wideWh: ISize2di, origWh: ISize2di): MCrop = {
    // Есть ширина-длина сырца. Нужно придумать кроп с центром как можно ближе к центру исходного кропа.
    // Результат должен изнутри быть вписан в исходник по размерам.
    val rszRatioV = origWh.height.toFloat / wideWh.height.toFloat
    val rszRatioH = origWh.width.toFloat / wideWh.width.toFloat
    val rszRatio  = Math.max(1.0F, Math.min(rszRatioH, rszRatioV))
    val w = szMulted(wideWh.width, rszRatio)
    val h = szMulted(wideWh.height, rszRatio)
    val r = MCrop(
      width  = w,
      height = h,
      // Для пересчета координат центра нужна поправка, иначе откропанное изображение будет за экраном.
      offX = translatedCropOffset(
        ocOffCoord  = crop0.offX,
        ocSz        = crop0.width,
        targetSz    = w,
        oiSz        = origWh.width
      ),
      offY = translatedCropOffset(
        ocOffCoord  = crop0.offY,
        ocSz        = crop0.height,
        targetSz    = h,
        oiSz        = origWh.height
      )
    )
    LOGGER.trace( s"updateCrop0($crop0, wh=$wideWh, origWh=$origWh) => $r ;; rsz=$rszRatio ;; h=$h")
    r
  }
  /** Сделать из опционального исходнго кропа новый wide-кроп с указанием гравитации. */
  def getWideCropInfo(iik: MAnyImgT, wideWh: ISize2di): Future[ImgCropInfo] = {
    getAbsCropOrFail(iik, wideWh)
      .map { crop1 =>
        ImgCropInfo(crop1, isCenter = false)
      }
      .recover { case ex: Exception =>
        def logPrefix = s"getWideCropInfo($iik, wh=$wideWh):"
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn(s"$logPrefix Failed to read image[${iik.dynImgId.fileName}] WH", ex)
        else
          LOGGER.debug(s"$logPrefix Failed to get abs crop: " + ex.getMessage)
        // По какой-то причине, нет возможности/необходимости сдвигать окно кропа. Делаем новый кроп от центра:
        val c = MCrop(width = wideWh.width, height = wideWh.height, 0, 0)
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
    val r = Math.max(0, resRaw)
    LOGGER.trace(s"centerNearest(center=$centerCoord, segLen=$segLen, axLen=$axLen) => $r (axC=$axCenter)")
    r
  }

  /**
   * Трансляция одного кропа по одной оси на новый размер.
   * @param ocOffCoord Сдвиг по текущей оси исходного кропа. Например crop.offX для оси X.
   * @param ocSz Размер исходного кропа по текущей оси. Например crop.width для оси X.
   * @param targetSz Целевой размер нового кропа (новый width).
   * @param oiSz Полный размер изображения по текущей оси. origWh.width для оси Х.
   * @return Новое значение offset'а для кропа.
   */
  def translatedCropOffset(ocOffCoord: Int, ocSz: Int, targetSz: Int, oiSz: Int): Int = {
    // 2017-12-13: Раньше тут нормировались axLen и centerCoord по rszRatio. Уже не вспомнить, почему так было.
    val newCoordFloat = centerNearestLineSeg1D(
      centerCoord = ocOffCoord + ocSz / 2,
      segLen      = targetSz.toFloat,
      axLen       = oiSz
    )
    newCoordFloat.toInt
  }


  /**
   * Собрать ссылку на wide-картинку и сопутствующие метаданные.
   * @param args Контейнер с аргументами вызова.
   * @return Фьючерс с результатом.
   */
  override def icompile(args: MImgMakeArgs): Future[MakeResult] = {
    lazy val logPrefix = s"icompile()#${System.currentTimeMillis()}:"
    LOGGER.trace( s"$logPrefix WIDE make: $args" )

    // Собираем хвост параметров сжатия.
    val devScreen = args.devScreenOpt
      .getOrElse( DevScreen.default )
    val pxRatio = devScreen.pixelRatio

    // Нужно вычислить размеры wide-версии оригинала. Используем szMult для вычисления высоты.
    val tgtHeightCssRaw = szMultedF(args.blockMeta.height, args.szMult)
    val tgtHeightReal = szMulted(tgtHeightCssRaw, pxRatio.pixelRatio)

    // Ширину экрана квантуем, получая ширину картинки.
    val cropWidthCssPx = normWideWidthBgSz(devScreen.width)
    val cropWidth = szMulted(cropWidthCssPx, pxRatio.pixelRatio)

    // Запустить сбор инфы по кропу.
    val wideWh = MSize2di(height = tgtHeightReal, width = cropWidth)

    val origImgId = args.img.dynImgId.original
    if (origImgId.dynFormat.isVector) {
      // Это SVG, вернуть всё как есть
      imgMakerUtil.returnImg( origImgId )

    } else {
      val cropInfoFut = getWideCropInfo(args.img, wideWh)

      // Начинаем собирать список трансформаций по ресайзу:
      val compression = args.compressMode
        .getOrElse(CompressModes.Bg)
        .fromDpr(pxRatio)

      val imOps0 = List[ImOp](
        AbsResizeOp(wideWh, ImResizeFlags.FillArea :: Nil),
        // FillArea почти всегда выдаёт результат, выходящий за пределы wideWh по одному из измерений.
        // Подогнать под wideWh, сделав extent-кроп по wideWh (как и рекомендуется в доках):
        ImGravities.Center,
        ExtentOp(wideWh),
        // сглаживание, сжатие вывода, итд
        ImFilters.Lanczos,
        StripOp,
        ImInterlaces.Plane,
        compression.chromaSubSampling,
        compression.imQualityOp
      )

      // Растр. Нужно брать кроп отн.середины только когда нет исходного кропа и реально широкая картинка. Иначе надо транслировать исходный пользовательский кроп в этот.
      val imFinal9Fut = for (cropInfo <- cropInfoFut) yield {
        // 2017-12-13: Тут долгое время был сначала ресайз до wideWh, а только потом кроп *в исходных измерениях*.
        // Это давало почему-то рабочие результаты, или ошибок просто не замечали...
        val imOps1 = AbsCropOp(cropInfo.crop) :: imOps0
        val imOps9 = if (cropInfo.isCenter) {
          LOGGER.warn(s"$logPrefix Failed to read image[${args.img.original.dynImgId.fileName}] WH")
          // По какой-то причине, нет возможности/необходимости сдвигать окно кропа. Делаем новый кроп от центра:
          ImGravities.Center :: imOps1
        } else {
          LOGGER.trace(s"$logPrefix Final crop info: $cropInfo")
          imOps1
        }
        LOGGER.trace(s"$logPrefix Final imOps=[${imOps9.mkString(", " )}]")
        args.img.withDynOps(imOps9)
      }

      // Вычислить размер картинки в css-пикселях.
      val szCss = MSize2di(
        height = szRounded(tgtHeightCssRaw),
        width  = cropWidthCssPx
      )
      // Дождаться результатов рассчета картинки и вернуть контейнер с результатами.
      for {
        imFinal9 <- imFinal9Fut
      } yield {
        MakeResult(
          szCss       = szCss,
          szReal      = wideWh,
          dynCallArgs = imFinal9,
          isWide      = true
        )
      }
    }
  }

}
