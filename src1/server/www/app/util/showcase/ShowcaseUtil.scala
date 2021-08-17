package util.showcase

import javax.inject.Inject
import io.suggest.ad.blk.{BlockHeights, BlockWidths, MBlockExpandMode}
import io.suggest.dev.{MScreen, MSzMults}
import io.suggest.grid.{GridCalc, MGridCalcConf}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.sc.ScConstants
import io.suggest.sc.sc3.MScQs
import io.suggest.util.logs.MacroLogsImplLazy
import models.blk
import models.blk._
import util.adv.AdvUtil
import util.n2u.N2NodesUtil
import io.suggest.common.empty.OptionUtil.Implicits._
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.model.EsModel
import io.suggest.jd.MJdConf
import io.suggest.n2.edge.{MEdgeFlagData, MPredicates}
import io.suggest.scalaz.ScalazUtil.Implicits._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:10
 * Description: Всякая статическая утиль для выдачи.
 */
final class ShowcaseUtil @Inject() (
                                     injector     : Injector,
                                   )
  extends MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val advUtil = injector.instanceOf[AdvUtil]
  private lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._

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
        val isNarrow = advUtil
          .getAdvMainBlock(e)
          .fold[Boolean] (true) { jdtTree =>
            jdtTree.rootLabel.props1.widthPx
              .exists { widthPx =>
                BlockWidths.min.value <= widthPx
              }
          }
        if (isNarrow) {
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
        dscr    <- deviceScreenOpt
        treeTpl <- advUtil.getAdvMainBlock(mad)
        p1 = treeTpl.rootLabel.props1
        wh      <- p1.wh
      } yield {
        fitBlockToScreen(wh, p1.expandMode, dscr)
      }
      dscrSz getOrElse {
        TILES_SZ_MULTS.last
      }
    }

    // Нужно получить данные для рендера широкой карточки.
    val brArgs = blk.RenderArgs(
      mad       = mad,
      withEdit  = false,
      szMult    = szMult,
      // Удалено bgImg при переезде на jdAds.
      bgImg     = None,
      isFocused = true
    )
    Future.successful( brArgs )
  }


  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  private def TILES_SZ_MULTS: List[SzMult_t] = {
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
  lazy val GRID_COLS_CONF = MGridCalcConf.EVEN_GRID


  def getTileArgs(dscrOpt: Option[MScreen]): MJdConf = {
    dscrOpt.fold {
      LOGGER.trace("getTileArgs(): Screen missing")
      MJdConf(
        szMult            = MSzMults.GRID_MIN_SZMULT,
        gridColumnsCount  = GRID_COLS_CONF.maxColumns,
        isEdit            = false
      )
    } { getTileArgs }
  }
  def getTileArgs(dscr: MScreen): MJdConf = {
    val gridColumnsConf = GRID_COLS_CONF
    val colsCount = GridCalc.getColumnsCount(dscr.wh, gridColumnsConf)
    MJdConf(
      szMult              = GridCalc.getSzMult4tilesScr(colsCount, dscr.wh, gridColumnsConf),
      gridColumnsCount    = colsCount,
      isEdit              = false,
    )
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
   * @param wh Метаданные блока.
   * @param dscr Данные по экрану устройства.
   * @return Значение SzMult, пригодное для рендера блока.
   */
  def fitBlockToScreen(wh: MSize2di, expandMode: Option[MBlockExpandMode], dscr: MScreen): SzMult_t = {
    val hfloat = wh.height.toFloat

    val szMultIter0 = for {
      bh <- BlockHeights.values.iterator
      heightPx = bh.value
      if (heightPx < dscr.wh.height) &&
         (heightPx >= wh.height)
    } yield {
      heightPx.toFloat / hfloat
    }

    // для не-wide карточек также возможно отображение в двойном размере.
    val szMultIter1: Iterator[SzMult_t] = if (expandMode.nonEmpty) {
      szMultIter0
    } else {
      Iterator.single(FOCUSED_SZ_MULT) ++ szMultIter0
    }

    val maxHiter = (szMultIter1 ++ TILES_SZ_MULTS.iterator).filter { szMult =>
      // Проверяем, влезает ли ширина на экран при таком раскладе?
      val w1 = getW1(szMult, colCnt = 1, blockWidth = wh.width, scrWidth = dscr.wh.width, paddingPx = FOCUSED_PADDING_CSSPX)
      w1 >= MIN_W1
    }

    if (maxHiter.isEmpty)
      MIN_SZ_MULT
    else {
      import Ordering.Float.TotalOrdering
      maxHiter.max
    }
  }


  /** Необходимо ли перевести focused-запрос в index запрос? */
  def isFocGoToProducerIndexFut(qs: MScQs): Future[Option[MNode]] = {
    lazy val logPrefix = s"_isFocGoToProducerIndexFut()#${System.currentTimeMillis()}:"

    val toProducerFutOpt = for {
      focQs <- qs.foc
      if focQs.indexAdOpen.nonEmpty &&
         (focQs.adIds.asSeq.lengthIs == 1)
      adId = focQs.adIds.head
    } yield {
      for {
        // Прочитать из хранилища указанную карточку.
        madOpt <- mNodes.getByIdCache( adId )

        // .get приведёт к NSEE, это нормально.
        producerId = {
          madOpt
            .flatMap(n2NodesUtil.madProducerId)
            .get
        }

        // Запрещать переход в самого себя:
        if !(qs.search.rcvrId containsStr producerId ) &&
          !ScConstants.Mad404.is404Node( producerId )   // Запрещать переход на 404-узел и 404-карточку.

        producer <- {
          val prodFut = mNodes.getByIdCache(producerId)
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
          prodFut
        }

      } yield {
        // Да, вернуть продьюсера.
        LOGGER.trace(s"$logPrefix Foc ad#${focQs.adIds} go-to index producer#$producerId")
        Some( producer )
      }
    }

    toProducerFutOpt
      .getOrNoneFut
      .recover { case _: NoSuchElementException =>
        None
      }
  }


  /** Собрать edge-флаги для ресивера, для отправки в выдачу клиента.
    *
    * @param qs распарсенные qs
    * @param mad Узел рекламной карточки.
    * @return Множество найденных edge-флагов.
    */
  def collectScRcvrFlags(qs: MScQs, mad: MNode): Iterable[MEdgeFlagData] = {
    val rcvrIds = (
      qs.common.locEnv.beacons ::
      qs.search.rcvrId.toList ::
      Nil
    )
      .iterator
      .flatten
      .map(_.id)
      .toSet

    val edgeFlagsApiAllowed = qs.common.apiVsn.clientEdgeFlagsAllowed
    (for {
      e <- mad.edges.withPredicateIter( MPredicates.Receiver )
      if (e.nodeIds & rcvrIds).nonEmpty
      // Могут быть одинаковые флаги, слать их много раз смысла нет.
      flagData <- e.info
        .flags
        .iterator
      if flagData.flag.isScClientSide &&
         // На версиях cordova app <= 4.2 любой неизвестный флаг приводил к краху.
         edgeFlagsApiAllowed.fold(true)(_ contains flagData.flag)
    } yield {
      flagData.flag -> flagData
    })
      .toMap
      .values
  }

}


/** Description: Интерфейс для доступа к DI-инстансу [[util.showcase.ShowcaseUtil]]. */
trait IScUtil {

  def scUtil: ShowcaseUtil

}
