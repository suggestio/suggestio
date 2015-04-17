package util.blocks

import io.suggest.ym.model.common.{Imgs, BlockMeta, MImgInfoT}
import models.blk.{SzMult_t, szMulted}
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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 10:02
 * Description: Утиль для поддержки фоновой картинки блока.
 * 2014.oct.16: Код продолжен толстеть и был перенесён сюда из BlockImg.scala.
 */

object BgImg extends PlayLazyMacroLogsImpl {

  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, preDetectMainColor = true)


  /**
   * Вычислить размер картинки для рендера на основе размера блока и параметрах экрана.
   * @param szMult Желаемый контроллером множитель размера картинки.
   * @param blockMeta Целевой размер. В частности - метаданные блока.
   * @param devScreen Данные по экрану устройства.
   * @return Параметры для картинки.
   */
  private def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreen: DevScreen): MImgInfoMeta = {
    getRenderSz(szMult, blockMeta, devScreen, devScreen.pixelRatioOpt)
  }
  private def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): MImgInfoMeta = {
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
  private def getImgResMult(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatioOpt: Option[DevPixelRatio]): Float = {
    val pxRatio = DevPixelRatios.pxRatioDefaulted(pxRatioOpt)
    // Реальный мультипликатор размера (разрешения) картинки на основе размеров экрана, блока и пожеланий в настройках рендера.
    val sizeMult = detectMaxSzMult(szMult, blockMeta, screenSz = devScreenSz)
    // Финальный мультипликатор размера картинки. Учитывает плотность пикселей устройства и допуск рендера в 2х разрешении.
    // TODO Надо наверное как-то ограничивать это чудо природы? Для развернутой картинки на 3.0-экране будет
    //      6-кратное разрешение блока /O_o/ Памяти на девайсе может не хватить.
    pxRatio.pixelRatio * sizeMult
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
  @tailrec private def detectMaxSzMult(szMult: SzMult_t, blockSz: MImgSizeT, screenSz: MImgSizeT): SzMult_t = {
    if (szMult <= 1F) {
      1F
    } else if (blockSz.width * szMult <= screenSz.width) {
      szMult
    } else {
      detectMaxSzMult(szMult / 2F, blockSz, screenSz)
    }
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
    val devPxRatio = DevPixelRatios.pxRatioDefaulted( devScreen.pixelRatioOpt )
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

