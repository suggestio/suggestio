package util.blocks

import io.suggest.ym.model.common.{IBlockMeta, Imgs, BlockMeta, MImgInfoT}
import models.blk.SzMult_t
import models.im._
import play.api.mvc.Call
import util.PlayLazyMacroLogsImpl
import util.cdn.CdnUtil
import util.img._
import scala.annotation.tailrec
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models._
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 10:02
 * Description: Утиль для поддержки фоновой картинки блока.
 * 2014.oct.16: Код продолжен толстеть и был перенесён сюда из BlockImg.scala.
 */

object BgImg extends PlayLazyMacroLogsImpl {

  import LOGGER._

  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, preDetectMainColor = true)

  /** Целевая высота широкой картинки. */
  val WIDE_TARGET_HEIGHT_PX = configuration.getInt("blocks.bg.wide.height.target.px") getOrElse 620


  /** Желаемые ширИны широкого бэкграунда. */
  val WIDE_WIDTHS_PX: List[Int]  = getConfSzsRow("widths",  List(350, 500, 650, 850, 950, 1100, 1250, 1600, 2048) )
  val WIDE_HEIGHTS_PX: List[Int] = getConfSzsRow("heights", List(300, 460, WIDE_TARGET_HEIGHT_PX) )


  private def getConfSzsRow(confKeyPart: String, default: => List[Int]): List[Int] = {
    configuration.getIntSeq(s"blocks.bg.wide.$confKeyPart.px")
      .fold(default) { _.toList.map(_.intValue) }
      .sorted
  }


  /**
   * Рассчет ширины широкой картинки. Нужно снизить лишний трафик клиента, снизить нагрузку на хранилища картинок,
   * но отображать картинку как можно шире. Для этого используется квантование переданной ширины экрана устройства.
   * @param deviceScreenOpt Параметры экрана, если известны.
   * @return Целочисленная ширина картинки.
   */
  private def getWideWidthCss(deviceScreenOpt: Option[DevScreen]): Int = {
    // TODO Следует лимитировать ширину по доступной ширине картинки при текущем szMult.
    deviceScreenOpt match {
      case Some(ds) =>
        normWideWidthBgSz(ds.width)
      case None =>
        WIDE_WIDTHS_PX.last
    }
  }


  def szMulted(origPx: Int, szMult: SzMult_t): Int = {
    szMulted(origPx.toFloat, szMult)
  }
  def szMulted(origPx: SzMult_t, szMult: SzMult_t): Int = {
    Math.round(origPx * szMult)
  }

  /** Попытаться подправить опциональный исходный кроп, если есть. Если нет, то фейл. */
  def getAbsCropOrFail(iik: MAnyImgT, wideWh: MImgSizeT): Future[ImgCrop] = {
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
  private def updateCrop0(crop0: ImgCrop, wideWh: MImgSizeT, origWhFut: Future[MImgSizeT]): Future[ImgCrop] = {
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
  private def getWideCropInfo(iik: MAnyImgT, wideWh: MImgSizeT): Future[ImgCropInfo] = {
    getAbsCropOrFail(iik, wideWh)
      .map { crop1 => ImgCropInfo(crop1, isCenter = false) }
      .recover { case ex: Exception =>
        warn(s"Failed to read image[${iik.fileName}] WH", ex)
        // По какой-то причине, нет возможности/необходимости сдвигать окно кропа. Делаем новый кроп от центра:
        val c = ImgCrop(width = wideWh.width, height = wideWh.height, 0, 0)
        ImgCropInfo(c, isCenter = true)
      }
  }

  /**
   * Асинхронно собрать параметры для доступа к dyn-картинке. Необходимость асинхронности вызвана
   * необходимостью получения данных о размерах исходной картинки.
   * @param bgImgInfo Данные о фоновой картинке карточки.
   * @param bm Метаданные блока карточки.
   * @param szMult Множитель размера, заданный на верхнем уровне (контроллером или чем-то в той области).
   *               2014.nov.05: Не используется, а надо бы. Пока высота прибивается к лимиту экрана или системы.
   * @param ctx Контекст рендера шаблона.
   * @return Фьючерс с данными по рендеру широкой фоновой картинки.
   */
  def wideBgImgArgs(bgImgInfo: MImgInfoT, bm: BlockMeta, szMult: SzMult_t)(implicit ctx: Context): Future[blk.WideBgRenderCtx] = {
    wideBgImgArgs(bgImgInfo, bm, szMult, ctx.deviceScreenOpt)
  }
  def wideBgImgArgs(bgImgInfo: MImgInfoT, bm: BlockMeta, szMult: SzMult_t, deviceScreenOpt: Option[DevScreen]): Future[blk.WideBgRenderCtx] = {
    val iik = MImg( bgImgInfo.filename )
    val iikOrig = iik.original
    // Собираем хвост параметров сжатия.
    val pxRatio = pxRatioDefaulted( deviceScreenOpt.flatMap(_.pixelRatioOpt) )
    // Нужно вычислить размеры wide-версии оригинала. Используем szMult для вычисления высоты.
    val tgtHeightCssRaw = bm.height * szMult
    val tgtHeightReal = szMulted(tgtHeightCssRaw, pxRatio.pixelRatio)
    // Ширину экрана квантуем, получая ширину картинки.
    val cropWidthCssPx = getWideWidthCss(deviceScreenOpt)
    val cropWidth = szMulted(cropWidthCssPx, pxRatio.pixelRatio)
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
    val wideWh = MImgInfoMeta(height = tgtHeightReal, width = cropWidth)
    val cropInfoFut = getWideCropInfo(iik, wideWh)
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
      blk.WideBgRenderCtx(
        szCss       = szCss,
        szReal      = wideWh,
        dynCallArgs = iik.copy(dynImgOps = imOps2)
      )
    }
  }


  /**
   * Определить максимальный разумный множитель размера картинки для указанного экрана.
   * Если рендер с указанным множителем не оправдан, то будет попытка с множитель в 2 раза меньшим.
   * @param szMult Текущий (исходный) желаемый множитель размера. Т.е. максимальный допустимый (запрошенный).
   * @param blockSz Размер блока.
   * @param screenSz Размер экрана.
   * @return Множитель.
   */
  // TODO Вероятно, этот метод не нужен. Мнение контроллера по вопросам рендера не должно "корректироваться" на нижнем уровне.
  @tailrec def detectMaxSzMult(szMult: SzMult_t, blockSz: MImgSizeT, screenSz: MImgSizeT): SzMult_t = {
    if (szMult <= 1F) {
      1F
    } else if (blockSz.width * szMult <= screenSz.width) {
      szMult
    } else {
      detectMaxSzMult(szMult / 2F, blockSz, screenSz)
    }
  }

  /** Дефолтовый pixel ratio, используемый в рамках bgImg. */
  def pxRatioDflt = DevPixelRatios.MDPI

  /** Если pixel ratio не задан, то взять дефолтовый, используемый для bgImg. */
  def pxRatioDefaulted(pxRatioOpt: Option[DevPixelRatio]): DevPixelRatio = {
    if (pxRatioOpt.isDefined) pxRatioOpt.get else pxRatioDflt
  }

  /**
   * Вычислить размер картинки для рендера на основе размера блока и параметрах экрана.
   * @param szMult Желаемый контроллером множитель размера картинки.
   * @param blockMeta Целевой размер. В частности - метаданные блока.
   * @param devScreen Данные по экрану устройства.
   * @return Параметры для картинки.
   */
  def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreen: DevScreen): MImgInfoMeta = {
    getRenderSz(szMult, blockMeta, devScreen, devScreen.pixelRatioOpt)
  }
  def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): MImgInfoMeta = {
    val imgResMult = getImgResMult(szMult, blockMeta, devScreenSz, pxRatioOpt)
    MImgInfoMeta(
      height = szMulted(blockMeta.height, imgResMult),
      width  = szMulted(blockMeta.width, imgResMult)
    )
  }

  /**
   * Определить мультипликатор размеров сторон картинки. по сути - комбинация pxRatioDefaulted() и detectMaxSzMult().
   * @param szMult Мультипликатор размера, желаемый контроллером.
   * @param blockMeta Целевой размер картинки.
   * @param devScreenSz Экран устройства.
   * @param pxRatioOpt Плотность пикселей устройства.
   * @return Мультипликатор, на который надо домножать пиксельный размер стороны картинки.
   */
  def getImgResMult(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): Float = {
    val pxRatio = pxRatioDefaulted(pxRatioOpt)
    // Реальный мультипликатор размера (разрешения) картинки на основе размеров экрана, блока и пожеланий в настройках рендера.
    val sizeMult = detectMaxSzMult(szMult, blockMeta, screenSz = devScreenSz)
    // Финальный мультипликатор размера картинки. Учитывает плотность пикселей устройства и допуск рендера в 2х разрешении.
    // TODO Надо наверное как-то ограничивать это чудо природы? Для развернутой картинки на 3.0-экране будет
    //      6-кратное разрешение блока /O_o/ Памяти на девайсе может не хватить.
    pxRatio.pixelRatio * sizeMult
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
  def translatedCropOffset(ocOffCoord: Int, ocSz: Int, targetSz: Int, oiSz: Int, rszRatio: Float): Int = {
    val newCoordFloat = centerNearestLineSeg1D(
      centerCoord = (ocOffCoord + ocSz / 2) / rszRatio,
      segLen = targetSz.toFloat,
      axLen = oiSz / rszRatio
    )
    newCoordFloat.toInt
  }

  /** Быстрый экстрактор фоновой картинки. */
  def getBgImg(mad: MAdT) = BlocksConf.applyOrDefault(mad.blockMeta.blockId).getMadBgImg(mad)

  /**
   * Сгенерить ссылку для получения фоновой картинки. Система выберет подходящую картинку под девайс.
   * @param imgInfo Инфа о картинке, используемой в качестве фона.
   * @param blockMeta Метаданные блока (и картинки, соответственно).
   * @param brArgs Параметры рендера блока.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для рендера в ссылку.
   */
  def bgImgCall(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    val oiik = MImg(imgInfo.filename)
    val devScreen = ctx.deviceScreenOpt getOrElse {
      DevScreen(
        width = 1024,
        height = 768,
        pixelRatioOpt = None
      )
    }
    val devPxRatio = pxRatioDefaulted( devScreen.pixelRatioOpt )
    // Генерим dynImg-ссылку на картинку.
    val fgc = devPxRatio.fgCompression

    // Настройки сохранения результирующей картинки (аккамулятор).
    var imOpsAcc: List[ImOp] = List(
      StripOp,
      ImInterlace.Plane,
      fgc.chromaSubSampling,
      fgc.imQualityOp
    )

    // Втыкаем resize. Он должен идти после возможного кропа, но перед другими операциями.
    imOpsAcc ::= {
      val sz = getRenderSz(brArgs.szMult, blockMeta = blockMeta, devScreen)
      // IgnoreAspectRatio полезен, иначе браузер сам начнёт пытаться растягивать картинку, отображая мазню на экране.
      AbsResizeOp(sz, ImResizeFlags.IgnoreAspectRatio)
    }
    imOpsAcc ::= ImFilters.Lanczos

    // Генерим финальную ссыль на картинку с учетом возможного кропа или иных исходных трансформаций:
    val dargs = oiik.copy(dynImgOps = oiik.dynImgOps ++ imOpsAcc)
    DynImgUtil.imgCall(dargs)
  }

}


/** Функционал для сохранения фоновой (основной) картинки блока. */
trait SaveBgImgI extends ISaveImgs {

  def BG_IMG_FN: String
  def bgImgBf: BfImage

  /** Прочитать данные по картинки из imgs-поля рекламной карточки. */
  def getMadBgImg(mad: Imgs): Option[MImgInfoT] = {
    mad.imgs.get(BG_IMG_FN)
  }

  def bgImgCall(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    BgImg.bgImgCall(imgInfo, blockMeta, brArgs)
  }

  /** Аналог bgImgCall, но метод пытается сгенерить ссылку на картинку, пролегающую через CDN (если настроено). */
  def bgImgCallCdn(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    val call = bgImgCall(imgInfo, blockMeta, brArgs)
    CdnUtil.forCall(call)
  }


  /**
   * Асинхронно собрать параметры для доступа к dyn-картинке. Необходимость асинхронности вызвана
   * необходимостью получения данных о размерах исходной картинки.
   * @param mad рекламная карточка или что-то совместимое с Imgs и IBlockMeta.
   * @param szMult Требуемый мультипликатор размера картинки.
   * @return None если нет фоновой картинки. Иначе Some() с данными рендера фоновой wide-картинки.
   */
  def wideBgImgArgs(mad: Imgs with IBlockMeta, szMult: SzMult_t)(implicit ctx: Context): Future[Option[blk.WideBgRenderCtx]] = {
    getMadBgImg(mad) match {
      case Some(bgImgInfo) =>
        BgImg.wideBgImgArgs(bgImgInfo, mad.blockMeta, szMult)
          .map(Some.apply)
      case None =>
        Future successful Option.empty[blk.WideBgRenderCtx]
    }
  }

}


/** Примесь для блока, чтобы в нём появилась поддержка задания/отображения фоновой картинки. */
trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf


  /** Поиск поля картинки для указанного имени поля. */
  override def getImgFieldForName(fn: String): Option[BfImage] = {
    if (fn == BG_IMG_FN)
      Some(bgImgBf)
    else
      super.getImgFieldForName(fn)
  }

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = BG_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut
    )
  }

  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = bgImgBf :: super.blockFieldsRev(af)

  // Mapping
  private def m = bgImgBf.getStrictMapping.withPrefix(bgImgBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeBim = m.bind(data)
    SaveImgUtil.mergeBindAcc(maybeAcc0, maybeBim)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyBIM(bgImgBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyBIM(bgImgBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

