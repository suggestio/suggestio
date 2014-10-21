package util.blocks

import controllers.routes
import io.suggest.ym.model.common.{Imgs, BlockMeta, MImgInfoT}
import models.blk.BlockHeights
import models.im._
import play.api.mvc.Call
import util.PlayLazyMacroLogsImpl
import util.cdn.CdnUtil
import util.img._
import util.showcase.ShowcaseUtil
import scala.annotation.tailrec
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 10:02
 * Description: Утиль для поддержки фоновой картинки блока.
 * 2014.oct.16: Код продолжен толстеть и был перенесён сюда из BlockImg.scala.
 */

object BgImg extends PlayLazyMacroLogsImpl {

  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, imgUtil = OrigImageUtil)

  /** Желаемые ширИны широкого бэкграунда. */
  val WIDE_WIDTHS_PX: List[Int] = {
    configuration.getIntSeq("blocks.bg.wide.widths.px")
      .map(_.toList.map(_.intValue))
      .getOrElse {
        // TODO Цифры взяты с потолка. Стоило бы навести в них какую-то логику...
        List(160, 350, 500, 650, 850, 950, 1100, 1250, 1600, 2048)
      }
      .sorted
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
  @tailrec def detectMaxSzMult(szMult: Int, blockSz: MImgSizeT, screenSz: MImgSizeT): Int = {
    if (szMult <= 1) {
      1
    } else if (blockSz.width * szMult <= screenSz.width) {
      szMult
    } else {
      detectMaxSzMult(szMult / 2, blockSz, screenSz)
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
  def getRenderSz(szMult: Int, blockMeta: MImgSizeT, devScreen: DevScreen): MImgInfoMeta = {
    getRenderSz(szMult, blockMeta, devScreen, devScreen.pixelRatioOpt)
  }
  def getRenderSz(szMult: Int, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): MImgInfoMeta = {
    val imgResMult = getImgResMult(szMult, blockMeta, devScreenSz, pxRatioOpt)
    MImgInfoMeta(
      height = (blockMeta.height * imgResMult).toInt,
      width  = (blockMeta.width * imgResMult).toInt
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
  def getImgResMult(szMult: Int, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): Float = {
    val pxRatio = pxRatioDefaulted(pxRatioOpt)
    // Реальный мультипликатор размера (разрешения) картинки на основе размеров экрана, блока и пожеланий в настройках рендера.
    val sizeMult = detectMaxSzMult(szMult, blockMeta, screenSz = devScreenSz)
    // Финальный мультипликатор размера картинки. Учитывает плотность пикселей устройства и допуск рендера в 2х разрешении.
    // TODO Надо наверное как-то ограничивать это чудо природы? Для развернутой картинки на 3.0-экране будет
    //      6-кратное разрешение блока /O_o/ Памяти на девайсе может не хватить.
    pxRatio.pixelRatio * sizeMult
  }

  /**
   * В былом формате откропанная картинка хранилась в двойном разрешении, которое соответствовало размерам блока.
   * Ширина и длина кропа сохранялись согласно двойному размеру блока, а offX и offY были относительно оригинала.
   * В общем, абсолютно неюзабельный для дальнейших трансформаций формат.
   */
  def canRenderDyn(iik: ImgIdKey, blockMeta: MImgSizeT): Boolean = {
    iik.cropOpt.isEmpty || iik.cropOpt.exists { crop =>
      val oldFormat  =  crop.height == blockMeta.height  ||  crop.width == blockMeta.width
      !oldFormat
    }
  }

  /** Подобрать ширину фоновой картинки на основе списка допустимых вариантов. */
  @tailrec def normWideBgWidth(minWidth: Int,  acc: Int = WIDE_WIDTHS_PX.head,  variants: List[Int] = WIDE_WIDTHS_PX.tail): Int = {
    if (acc < minWidth && variants.nonEmpty) {
      normWideBgWidth(minWidth, variants.head, variants.tail)
    } else {
      acc
    }
  }

}


/** Функционал для сохранения фоновой (основной) картинки блока. */
trait SaveBgImgI extends ISaveImgs {

  import BgImg._
  import BgImg.LOGGER._

  def BG_IMG_FN: String
  def bgImgBf: BfImage

  /** Прочитать инфу о сохраняемой картинке из карты картинок создаваемого блока. */
  override def getBgImg(bim: BlockImgMap): Option[ImgInfo4Save[ImgIdKey]] = {
    bim.get(BG_IMG_FN)
  }

  /** Прочитать данные по картинки из imgs-поля рекламной карточки. */
  def getMadBgImg(mad: Imgs): Option[MImgInfoT] = {
    mad.imgs.get(BG_IMG_FN)
  }


  /**
   * Сгенерить ссылку для получения фоновой картинки. Система выберет подходящую картинку под девайс.
   * @param imgInfo Инфа о картинке, используемой в качестве фона.
   * @param blockMeta Метаданные блока (и картинки, соответственно).
   * @param brArgs Параметры рендера блока.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для рендера в ссылку.
   */
  def bgImgCall(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    Some( ImgIdKey(imgInfo.filename) )
      .filter { iik =>
        canRenderDyn(iik, blockMeta)
      }
      .flatMap {
        // dynImg принимает только orig-картинки.
        case oiik: OrigImgIdKey => Some(oiik)
        case _ => None
      }
      .fold [Call] {
        // Пропускаем картинку, ибо данные для этого дела были отброшены.
        routes.Img.getImg(imgInfo.filename)
      } { oiik =>
        val devScreen = ctx.deviceScreenOpt getOrElse {
          warn(s"bgImgCall($imgInfo, bh=${blockMeta.height}): width=${blockMeta.width} Missing client screen size! Will use standard VGA (1024х768)!")
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

        // Если картинка была сохранена откропанной, то надо откропать исходник заново, отресайзив до размера кропа.
        if (oiik.cropOpt.isDefined) {
          val crop = oiik.cropOpt.get
          imOpsAcc ::= AbsCropOp(crop)
        }
        imOpsAcc ::= ImFilters.Lanczos
        // Генерим финальную ссыль на картинку:
        val dargs = DynImgArgs(oiik.uncropped,  imOpsAcc)
        routes.Img.dynImg(dargs)
      }
  }

  /** Аналог bgImgCall, но метод пытается сгенерить ссылку на картинку, пролегающую через CDN (если настроено). */
  def bgImgCallCdn(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    val call = bgImgCall(imgInfo, blockMeta, brArgs)
    CdnUtil.forCall(call)
  }


  /**
   * Генерация ссылки на wide-версию оригинала фона.
   * @param bgImgInfo Инфа о картинки, сохранённая в карточке.
   * @param brArgs Параметры рендера блока.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call для генерации картинки.
   */
  def wideBgImgCall(bgImgInfo: MImgInfoT, bm: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    val iik = ImgIdKey( bgImgInfo.filename )
    // Собираем хвост параметров сжатия.
    val pxRatio = pxRatioDefaulted( ctx.deviceScreenOpt.flatMap(_.pixelRatioOpt) )
    val bgc = pxRatio.bgCompression
    // Нужно вычислить размеры wide-версии оригинала.
    // Размер по высоте ограничиваем через высоту карточки (с учетом pixel ratio).
    val imgResMult = brArgs.szMult * pxRatio.pixelRatio
    val tgtHeightReal = (bm.height * imgResMult).toInt
    // Ширину кропа подбираем квантуя ширину экрана по допустимому набору ширИн.
    val cropWidth = ctx.deviceScreenOpt
      .fold(WIDE_WIDTHS_PX.last) { ds => normWideBgWidth(ds.width) }
    val imOps0 = List[ImOp](
      ImFilters.Lanczos,
      StripOp,
      ImInterlace.Plane,
      bgc.chromaSubSampling,
      bgc.imQualityOp
    )
    // TODO Нужно брать отн. середины только когда нет исходного кропа и реально широкая картинка. Иначе надо транслировать исходный пользовательский кроп в этот.
    val imOpsAcc: List[ImOp] =
      // В общих чертах вписать изображение в примерно необходимые размеры:
      AbsResizeOp(MImgInfoMeta(height = tgtHeightReal, width = cropWidth), Seq(ImResizeFlags.OnlyShrinkLarger, ImResizeFlags.FillArea)) ::
        // Вырезать из середины необходимый кусок:
        ImGravities.Center ::
        AbsCropOp(ImgCrop(width = cropWidth, height = tgtHeightReal, 0, 0)) ::
        // Сжать картинку по-лучше
        imOps0
    val dargs = DynImgArgs(iik.uncropped, imOpsAcc)
    routes.Img.dynImg(dargs)
  }

  /** Аналог wideBgImgCall(), но ссылка по возможности пролегает через CDN. */
  def wideBgImgCallCdn(bgImgInfo: MImgInfoT, bm: BlockMeta, brArgs: blk.RenderArgs)(implicit ctx: Context): Call = {
    val call = wideBgImgCall(bgImgInfo, bm, brArgs)
    CdnUtil.forCall(call)
  }

}


/** Примесь для блока, чтобы в нём появилась поддержка задания/отображения фоновой картинки. */
trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = BG_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut
    )
  }

  abstract override def blockFieldsRev: List[BlockFieldT] = bgImgBf :: super.blockFieldsRev

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

