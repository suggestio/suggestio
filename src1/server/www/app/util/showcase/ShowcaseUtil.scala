package util.showcase

import javax.inject.{Inject, Singleton}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidth, BlockWidths}
import io.suggest.dev.{MScreen, MSzMults}
import io.suggest.grid.{GridCalc, MGridCalcConf}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sc.ScConstants
import io.suggest.sc.sc3.MScQs
import io.suggest.util.logs.MacroLogsImplLazy
import models.blk
import models.blk._
import models.im.make.MakeResult
import models.mproj.ICommonDi
import models.msc.TileArgs
import util.adv.AdvUtil
import util.n2u.N2NodesUtil
import io.suggest.common.empty.OptionUtil.Implicits._

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
                               advUtil      : AdvUtil,
                               n2NodesUtil  : N2NodesUtil,
                               mNodes       : MNodes,
                               mCommonDi    : ICommonDi
                             )
  extends MacroLogsImplLazy
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
        val bwidth: BlockWidth = advUtil.getAdvMainBlockMeta(e)
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
  def focusedBrArgsFor(mad: MNode, deviceScreenOpt: Option[MScreen] = None): Future[blk.RenderArgs] = {
    val szMult: SzMult_t = {
      val dscrSz = for {
        dscr <- deviceScreenOpt
        bm   <- advUtil.getAdvMainBlockMeta(mad)
      } yield {
        fitBlockToScreen(bm, dscr)
      }
      dscrSz getOrElse {
        TILES_SZ_MULTS.last
      }
    }

    // Нужно получить данные для рендера широкой карточки.
    for (bgImgOpt <- focWideBgImgArgs(mad, szMult, deviceScreenOpt)) yield {
      blk.RenderArgs(
        mad       = mad,
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
  def focWideBgImgArgs(mad: MNode, szMult: SzMult_t, devScrOpt: Option[MScreen]): Future[Option[MakeResult]] = {
    LOGGER.error(s"focWideBgImgArgs(${mad.idOrNull}) Not implemented for jd-ads")
    Future.successful( None )
  }


  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  private val TILES_SZ_MULTS: List[SzMult_t] = {
    List(
      1.4F, 1.3F, 1.2F, 1.1F, 1.06F, 1.0F,
      MIN_SZ_MULT   // Экраны с шириной 640csspx требуют немного уменьшить карточку.
    )
  }

  /** Сколько пикселей минимум оставлять по краям раскрытых карточек. */
  private def FOCUSED_PADDING_CSSPX = 10

  def MIN_W1 = -1F

  /** float mult, аналогичный исходному. */
  private def MIN_SZ_MULT = MSzMults.GRID_MIN_SZMULT_D.toFloat


  /** Конфиг для рассчёта кол-ва колонок плитки. */
  val GRID_COLS_CONF = MGridCalcConf.EVEN_GRID


  def getTileArgs(dscrOpt: Option[MScreen]): TileArgs = {
    dscrOpt.fold {
      TileArgs(
        szMult    = MIN_SZ_MULT,
        colsCount = GRID_COLS_CONF.maxColumns
      )
    } { getTileArgs }
  }
  def getTileArgs(dscr: MScreen): TileArgs = {
    val colsCount = GridCalc.getColumnsCount(dscr, GRID_COLS_CONF)
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
  def getSzMult4tilesScr(colsCount: Int, dscr: MScreen): SzMult_t = {
    val blockWidthPx = GRID_COLS_CONF.cellWidth.value
    // Считаем целевое кол-во колонок на экране.
    @tailrec def detectSzMult(restSzMults: List[SzMult_t]): SzMult_t = {
      val nextSzMult = restSzMults.head
      if (restSzMults.tail.isEmpty) {
        nextSzMult
      } else {
        // Вычислить остаток ширины за вычетом всех отмасштабированных блоков, с запасом на боковые поля.
        val w1 = getW1(nextSzMult, colsCount, blockWidth = blockWidthPx, scrWidth = dscr.width, paddingPx = GRID_COLS_CONF.cellPadding)
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
    scrWidth - (colCnt * blockWidth + GRID_COLS_CONF.cellPadding * (colCnt + 1)) * szMult
  }

  /**
   * Вписывание блока по ширине и высоте в экран устройства. Прикидываются разные szMult.
   * @param bm Метаданные блока.
   * @param dscr Данные по экрану устройства.
   * @return Значение SzMult, пригодное для рендера блока.
   */
  def fitBlockToScreen(bm: BlockMeta, dscr: MScreen): SzMult_t = {
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
      Iterator.single(FOCUSED_SZ_MULT) ++ szMultIter0
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


  /** Необходимо ли перевести focused-запрос в index запрос? */
  def isFocGoToProducerIndexFut(qs: MScQs): Future[Option[MNode]] = {
    lazy val logPrefix = s"_isFocGoToProducerIndexFut()#${System.currentTimeMillis()}:"

    val toProducerFutOpt = for {
      focQs <- qs.foc
      if focQs.focIndexAllowed
    } yield {
      for {
        // Прочитать из хранилища указанную карточку.
        madOpt <- mNodesCache.getById( focQs.lookupAdId )

        // .get приведёт к NSEE, это нормально.
        producerId = {
          madOpt
            .flatMap(n2NodesUtil.madProducerId)
            .get
        }

        if !(qs.search.rcvrId containsStr producerId )

        producer <- {
          // 2015.sep.16: Нельзя перепрыгивать на продьюсера, у которого не больше одной карточки на главном экране.
          val prodAdsCountFut: Future[Long] = {
            val args = new MNodeSearchDfltImpl {
              override def outEdges: Seq[ICriteria] = {
                val cr = Criteria(
                  predicates  = MPredicates.Receiver :: Nil,
                  nodeIds     = producerId :: Nil
                )
                cr :: Nil
              }
              override def limit = 2
              override def nodeTypes = MNodeTypes.Ad :: Nil
            }
            mNodes.dynCount(args)
          }

          val prodFut = mNodesCache.getById(producerId)
            .map(_.get)

          // Как выяснилось, бывают карточки-сироты (продьюсер удален, карточка -- нет). Нужно сообщать об этой ошибке.
          for (ex <- prodFut.failed) {
            val msg = s"Producer node[$producerId] does not exist."
            if (ex.isInstanceOf[NoSuchElementException])
              LOGGER.error(msg)
            else
              LOGGER.error(msg, ex)
          }

          // Фильтруем prodFut по кол-ву карточек, размещенных у него на главном экране.
          prodAdsCountFut
            .filter { _ > 1 }
            .flatMap { _ => prodFut }
        }

      } yield {
        // Да, вернуть продьюсера.
        LOGGER.trace(s"$logPrefix Foc ad#${focQs.lookupAdId} go-to index producer#$producerId")
        Some( producer )
      }
    }

    toProducerFutOpt
      .getOrNoneFut
      .recover { case _: NoSuchElementException =>
        None
      }
  }

}


/** Description: Интерфейс для доступа к DI-инстансу [[util.showcase.ShowcaseUtil]]. */
trait IScUtil {

  def scUtil: ShowcaseUtil

}
