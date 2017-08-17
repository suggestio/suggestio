package util.showcase

import javax.inject.{Inject, Singleton}

import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.ad.blk.BlockMeta
import io.suggest.sc.ScConstants
import io.suggest.sc.tile.ColumnsCountT
import models._
import models.blk._
import models.im._
import models.im.make.{MakeArgs, MakeResult, Makers}
import models.mctx.Context
import models.mproj.ICommonDi
import models.msc.{IScSiteColors, ScSiteColors, TileArgs}
import util.blocks.BgImg

import scala.annotation.tailrec
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:10
 * Description: Всякая статическая утиль для выдачи.
 */
@Singleton
class ShowcaseUtil @Inject() (
  mCommonDi : ICommonDi
)
  extends ColumnsCountT
{

  import mCommonDi._

  /** Значение cache-control для index-выдачи. */
  def SC_INDEX_CACHE_SECONDS = 20

  /** Дефолтовый цвет выдачи, если нет ничего. */
  def SITE_BGCOLOR_DFLT = ScConstants.Defaults.BG_COLOR

  /** Дефолтовый цвет элементов переднего плана. */
  def SITE_FGCOLOR_DFLT = ScConstants.Defaults.FG_COLOR


  /**
    * Сгруппировать "узкие" карточки, чтобы они были вместе.
    *
    * Деланье такой упаковки было сделано на ранних этапах выдачи, когда узкая карточка перемещалась
    * к следующей по списку узкой, либо в конец списка.
    * С одной стороны -- задачу уплотнения выдачи это решало.
    * С другой -- это усложняет сортировку карточек, SCv2 Focused-v2 выдача оперирует id'шниками без размеров,
    * что мешает сортировке, а сама плитка по сути разбивается на пары столбцов, а не на ячейки.
    *
    * @param ads Исходный список элементов.
    * @tparam T Тип элемента.
    * @return
    */
  def groupNarrowAds[T <: MNode](ads: Seq[T]): Seq[T] = {
    val (enOpt1, acc0) = ads.foldLeft [(Option[T], List[T])] (None -> Nil) {
      case ((enOpt, acc), e) =>
        val bwidth: BlockWidth = e.ad.blockMeta
          .fold(BlockWidths.default)(bm => BlockWidths.withValue(bm.width))
        if (bwidth.isNarrow) {
          enOpt match {
            case Some(en) =>
              (None, en :: e :: acc)
            case None =>
              (Some(e), acc)
          }
        } else {
          (enOpt, e :: acc)
        }
    }
    val acc1 = enOpt1 match {
      case Some(en) => en :: acc0
      case None     => acc0
    }
    acc1.reverse
  }

  /** Дефолтовый мультипликатор размера для блоков, отображаемых через focusedAds(). */
  def FOCUSED_SZ_MULT: SzMult_t = 620F / 300F

  /**
   * Аргументы для рендера блока, когда карточка открыта.
   * @param mad Рекламная карточка.
   * @return Аргументы для рендера.
   */
  def focusedBrArgsFor(mad: MNode)(implicit ctx: Context): Future[blk.RenderArgs] = {
    focusedBrArgsFor(mad, ctx.deviceScreenOpt)
  }
  def focusedBrArgsFor(mad: MNode, deviceScreenOpt: Option[DevScreen] = None): Future[blk.RenderArgs] = {
    val szMult: SzMult_t = {
      val dscrSz = for {
        dscr <- deviceScreenOpt
        bm   <- mad.ad.blockMeta
      } yield {
        fitBlockToScreen(bm, dscr)
      }
      dscrSz getOrElse {
        TILES_SZ_MULTS.last
      }
    }

    val bc = BlocksConf.applyOrDefault( mad )

    // Нужно получить данные для рендера широкой карточки.
    for (bgImgOpt <- focWideBgImgArgs(mad, szMult, deviceScreenOpt)) yield {
      blk.RenderArgs(
        mad       = mad,
        bc        = bc,
        withEdit  = false,
        szMult    = szMult,
        bgImg     = bgImgOpt,
        isFocused = true
      )
    }
  }


  /**
   * Асинхронно собрать параметры для доступа к dyn-картинке. Необходимость асинхронности вызвана
   * необходимостью получения данных о размерах исходной картинки.
   * @param mad рекламная карточка или что-то совместимое с Imgs и IBlockMeta.
   * @param szMult Требуемый мультипликатор размера картинки.
   * @return None если нет фоновой картинки. Иначе Some() с данными рендера фоновой wide-картинки.
   */
  def focWideBgImgArgs(mad: MNode, szMult: SzMult_t, devScrOpt: Option[DevScreen]): Future[Option[MakeResult]] = {
    val optFut = for {
      mimg <- BgImg.getBgImg(mad)
      bm   <- mad.ad.blockMeta
    } yield {
      val wArgs = MakeArgs(
        img           = mimg,
        blockMeta     = bm,
        szMult        = szMult,
        devScreenOpt  = devScrOpt
      )
      val maker = Makers.forFocusedBg(bm.wide)
      current.injector
        .instanceOf( maker.makerClass )
        .icompile(wArgs)
        .map(Some.apply)
    }
    FutureUtil.optFut2futOpt(optFut)(identity)
  }

  override val MIN_SZ_MULT = super.MIN_SZ_MULT

  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  private val TILES_SZ_MULTS: List[SzMult_t] = configuration.getOptional[Seq[Double]]("sc.tiles.szmults")
    .map { _.map(_.toFloat).toList }
    .getOrElse {
      List(
        1.4F, 1.3F, 1.2F, 1.1F, 1.06F, 1.0F,
        MIN_SZ_MULT   // Экраны с шириной 640csspx требуют немного уменьшить карточку.
      )
    }


  /** Расстояние между блоками и до краёв экрана.
    * Размер этот должен быть жестко связан с остальными размерами карточек, поэтому не настраивается. */
  override def TILE_PADDING_CSSPX = super.TILE_PADDING_CSSPX

  /** Сколько пикселей минимум оставлять по краям раскрытых карточек. */
  private val FOCUSED_PADDING_CSSPX = configuration.getOptional[Int]("sc.focused.padding.hsides.csspx").getOrElse(10)

  /** Макс. кол-во вертикальных колонок. */
  override val TILE_MAX_COLUMNS = configuration.getOptional[Int]("sc.tiles.columns.max").getOrElse(4)
  override val TILE_MIN_COLUMNS = configuration.getOptional[Int]("sc.tiles.columns.min").getOrElse( super.TILE_MIN_COLUMNS )

  def MIN_W1 = -1F


  /** Размер одной ячейки в плитке. */
  override protected def CELL_WIDTH_CSSPX = getBlockWidthPx
  private def getBlockWidthPx = BlockWidths.NORMAL.value


  def getTileArgs(dscrOpt: Option[DevScreen]): TileArgs = {
    dscrOpt.fold {
      TileArgs(
        szMult = MIN_SZ_MULT,
        colsCount = TILE_MAX_COLUMNS
      )
    } { getTileArgs }
  }
  def getTileArgs(dscr: DevScreen): TileArgs = {
    val colsCount = getTileColsCountScr(dscr)
    TileArgs(
      szMult      = getSzMult4tilesScr(colsCount, dscr),
      colsCount   = colsCount
    )
  }

  /**
   * Вычислить мультипликатор размера для плиточной выдачи с целью подавления лишних полей по бокам.
   * @param colsCount Кол-во колонок в плитке.
   * @param dscr Экран.
   * @return Оптимальное значение SzMult_t выбранный для рендера.
   */
  def getSzMult4tilesScr(colsCount: Int, dscr: DevScreen): SzMult_t = {
    val blockWidthPx = getBlockWidthPx
    // Считаем целевое кол-во колонок на экране.
    @tailrec def detectSzMult(restSzMults: List[SzMult_t]): SzMult_t = {
      val nextSzMult = restSzMults.head
      if (restSzMults.tail.isEmpty) {
        nextSzMult
      } else {
        // Вычислить остаток ширины за вычетом всех отмасштабированных блоков, с запасом на боковые поля.
        val w1 = getW1(nextSzMult, colsCount, blockWidth = blockWidthPx, scrWidth = dscr.width, paddingPx = TILE_PADDING_CSSPX)
        if (w1 >= MIN_W1)
          nextSzMult
        else
          detectSzMult(restSzMults.tail)
      }
    }
    detectSzMult(TILES_SZ_MULTS)
  }

  /**
   * w1 - это ширина экрана за вычетом плитки блоков указанной ширины в указанное кол-во колонок.
   * Этот метод запускает формулу рассчета оставшейся ширины.
   * @param szMult Предлагаемый мультипликатор размера.
   * @param colCnt Кол-во колонок.
   * @param blockWidth Ширина блока.
   * @param scrWidth Доступная для размещения плитки блоков ширина экрана.
   * @return Остаток ширины.
   */
  private def getW1(szMult: SzMult_t, colCnt: Int, blockWidth: Int, scrWidth: Int, paddingPx: Int): Float = {
    scrWidth - (colCnt * blockWidth + TILE_PADDING_CSSPX * (colCnt + 1)) * szMult
  }

  /**
   * Вписывание блока по ширине и высоте в экран устройства. Прикидываются разные szMult.
   * @param bm Метаданные блока.
   * @param dscr Данные по экрану устройства.
   * @return Значение SzMult, пригодное для рендера блока.
   */
  def fitBlockToScreen(bm: BlockMeta, dscr: DevScreen): SzMult_t = {
    val hfloat = bm.height.toFloat

    val szMultIter0 = for {
      bh <- BlockHeights.values.iterator
      heightPx = bh.value
      if heightPx < dscr.height && heightPx >= bm.height
    } yield {
      heightPx.toFloat / hfloat
    }

    // для не-wide карточек также возможно отображение в двойном размере.
    val szMultIter1: Iterator[SzMult_t] = if (bm.wide) {
      szMultIter0
    } else {
      Iterator(FOCUSED_SZ_MULT) ++ szMultIter0
    }

    val maxHiter = (szMultIter1 ++ TILES_SZ_MULTS.iterator).filter { szMult =>
      // Проверяем, влезает ли ширина на экран при таком раскладе?
      val w1 = getW1(szMult, colCnt = 1, blockWidth = bm.width, scrWidth = dscr.width, paddingPx = FOCUSED_PADDING_CSSPX)
      w1 >= MIN_W1
    }

    if (maxHiter.isEmpty)
      MIN_SZ_MULT
    else
      maxHiter.max
  }


  /** Обычные цвета выдачи, не нужны в 99% случаев. */
  private def SC_COLORS_GEO = ScSiteColors(bgColor = SITE_BGCOLOR_DFLT, fgColor = SITE_FGCOLOR_DFLT)

  def siteScColors(nodeOpt: Option[MNode]): IScSiteColors = {
    nodeOpt.fold(SC_COLORS_GEO) { mnode =>
      ScSiteColors(
        bgColor = mnode.meta.colors.bg.fold(SITE_BGCOLOR_DFLT)(_.code),
        fgColor = mnode.meta.colors.fg.fold(SITE_FGCOLOR_DFLT)(_.code)
      )
    }
  }

}
