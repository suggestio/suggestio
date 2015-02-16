package util.showcase

import controllers.routes
import io.suggest.ym.model.MAd
import io.suggest.ym.model.common.{BlockMeta, AdShowLevels, IBlockMeta}
import models._
import models.blk.{BlockHeights, SzMult_t, BlockWidth, BlockWidths}
import models.im.DevScreenT
import util.blocks.BgImg
import play.api.Play.{current, configuration}
import util.cdn.CdnUtil
import scala.annotation.tailrec
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:10
 * Description: Всякая статическая утиль для выдачи.
 */
object ShowcaseUtil {

  /** Отображать ли пустые категории? */
  val SHOW_EMPTY_CATS = configuration.getBoolean("market.frontend.cats.empty.show") getOrElse true

  /** Путь до ассета со скриптом выдачи. */
  val SHOWCASE_JS_ASSET = configuration.getString("showcase.js.asset.path") getOrElse "javascripts/market/showcase/showcase2.js"

  /** Фон под рекламными карточками заполняется на основе цвета с добавление прозрачности от фона. */
  val TILES_BG_FILL_ALPHA: Float = configuration.getDouble("showcase.tiles.bg.fill.ratio") match {
    case Some(alpha) => alpha.toFloat
    case None        => 0.8F
  }

  def getCatOwner(adnId: String) = MMartCategory.DEFAULT_OWNER_ID

  /**
   * Сгруппировать "узкие" карточки, чтобы они были вместе.
   * @param ads Исходный список элементов.
   * @tparam T Тип элемента.
   * @return
   */
  def groupNarrowAds[T <: IBlockMeta](ads: Seq[T]): Seq[T] = {
    val (enOpt1, acc0) = ads.foldLeft [(Option[T], List[T])] (None -> Nil) {
      case ((enOpt, acc), e) =>
        val bwidth: BlockWidth = BlockWidths(e.blockMeta.width)
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


  /**
   * Сбор данных по категорям для выдачи указанного узла-ресивера.
   * @param adnIdOpt id узла, если есть.
   * @return Кортеж из фьючерса с картой статистики категорий и списком отображаемых категорий.
   */
  def getCats(adnIdOpt: Option[String]): GetCatsResult = {
    val catAdsSearch = new AdSearch {
      override def receiverIds    = adnIdOpt.toList
      override def maxResultsOpt  = Some(100)
      override def levels         = List(AdShowLevels.LVL_CATS)
    }
    // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
    val catsStatsFut = MAd.stats4UserCats(MAd.dynSearchReqBuilder(catAdsSearch))
      .map { _.toMap }
    // Текущие категории узла
    val mmcatsFut: Future[Seq[MMartCategory]] = if(SHOW_EMPTY_CATS) {
      // Включено отображение всех категорий.
      val catOwnerId = adnIdOpt.fold(MMartCategory.DEFAULT_OWNER_ID) { getCatOwner }
      for {
        mmcats <- MMartCategory.findTopForOwner(catOwnerId)
        catsStats <- catsStatsFut
      } yield {
        // Нужно, чтобы пустые категории шли после непустых. И алфавитная сортировка
        val nonEmptyCatIds = catsStats
          .iterator
          .filter { case (_, count)  =>  count > 0L }
          .map { _._1 }
          .toSet
        mmcats.sortBy { mmcat =>
          val sortPrefix: String = nonEmptyCatIds.contains(mmcat.idOrNull) match {
            case true  => "a"
            case false => "z"
          }
          sortPrefix + mmcat.name
        }
      }
    } else {
      // Отключено отображение скрытых категорий. Исходя из статистики, прочитать из модели только необходимые карточки.
      catsStatsFut flatMap { catsStats =>
        MMartCategory.multiGet(catsStats.keysIterator)
          .map { _.sortBy(MMartCategory.sortByMmcat) }
      }
    }
    GetCatsResult(catsStatsFut, mmcatsFut)
  }


  /** Генерация абсолютной ссылки на скрипт showcase2.js.
    * @param mkPermanentUrl Создавать постоянную ссылку. Такая ссылка может быть встроена на другие сайты.
    *                       У неё проблемы с геоэффективным кешированием, но она не будет изменятся во времени.
    * @param ctx Контекст рендера шаблонов.
    * @return Строка абсолютной ссылки (URL).
    */
  def showcaseJsAbsUrl(mkPermanentUrl: Boolean)(implicit ctx: Context): String = {
    if (mkPermanentUrl) {
      // Генерим ссылку на скрипт, которой потом можно будет поделится с другим сайтом.
      ctx.currAudienceUrl + routes.Assets.at(SHOWCASE_JS_ASSET).url
    } else {
      // Генерим постоянную ссылку на ассет с бешеным кешированием и возможностью загона в CDN.
      val call1 = CdnUtil.asset(SHOWCASE_JS_ASSET)
      if(call1.isInstanceOf[ExternalCall]) {
        // У нас тут ссылка на CDN.
        call1.url
      } else {
        // Поддержка CDN невозможна. Допиливаем адрес до абсолютной ссылки.
        ctx.currAudienceUrl + call1.url
      }
    }
  }


  /** Дефолтовый мультипликатор размера для блоков, отображаемых через focusedAds(). */
  def FOCUSED_SZ_MULT: SzMult_t = 620F / 300F

  /**
   * Аргументы для рендера блока, когда карточка открыта.
   * @param mad Рекламная карточка.
   * @return Аргументы для рендера.
   */
  def focusedBrArgsFor(mad: MAdT)(implicit ctx: Context): Future[blk.RenderArgs] = {
    val szMult: SzMult_t = ctx.deviceScreenOpt match {
      case Some(dscr) => fitBlockToScreen(mad.blockMeta, dscr)
      case None       => TILES_SZ_MULTS.last
    }
    if (mad.blockMeta.wide) {
      // Нужно получить данные для рендера широкой карточки.
      val bc = BlocksConf applyOrDefault mad.blockMeta.blockId
      val wideBgCtxOptFut = bc.wideBgImgArgs(mad, szMult)
      wideBgCtxOptFut map { wideBgCtxOpt =>
        blk.RenderArgs(
          withEdit      = false,
          isStandalone  = false,
          szMult        = szMult,
          wideBg        = wideBgCtxOpt
        )
      }

    } else {
      // Возвращаем результат
      val bra = blk.RenderArgs(
        withEdit      = false,
        isStandalone  = false,
        szMult        = szMult,
        wideBg        = None
      )
      Future successful bra
    }
  }


  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  val TILES_SZ_MULTS: List[SzMult_t] = configuration.getDoubleSeq("sc.tiles.szmults")
    .map { _.map(_.toFloat).toList }
    .getOrElse { List(1.4F, 1.3F, 1.2F, 1.1F, 1.06F, 1.0F,
      32F/33F   // Экраны с шириной 640csspx требуют немного уменьшить карточку.
  ) }

  val FOCUSED_TILE_SZ_MULTS = FOCUSED_SZ_MULT :: TILES_SZ_MULTS

  /** Расстояние между блоками и до краёв экрана.
    * Размер этот должен быть жестко связан с остальными размерами карточек, поэтому не настраивается. */
  def TILE_PADDING_CSSPX = 20

  /** Сколько пикселей минимум оставлять по краям раскрытых карточек. */
  val FOCUSED_PADDING_CSSPX = configuration.getInt("sc.focused.padding.hsides.csspx") getOrElse 10

  /** Макс. кол-во вертикальных колонок. */
  val TILE_MAX_COLUMNS = configuration.getInt("sc.tiles.columns.max") getOrElse 4
  val TILE_MIN_COLUMNS = configuration.getInt("sc.tiles.columns.min") getOrElse 1

  val MIN_W1 = -1F

  val MIN_SZ_MULT = TILES_SZ_MULTS.last


  private def getBlockWidthPx = BlockWidths.NORMAL.widthPx

  /**
   * Рассчитать целевое кол-во колонок в плитке. Считаем это по минимальному padding'у.
   * @param dscr Экран устройства.
   * @return Число от 1 до 4.
   */
  def getTileColsCountScr(dscr: DevScreenT): Int = {
    val padding = TILE_PADDING_CSSPX * MIN_SZ_MULT
    val targetCount = ((dscr.width - padding) / (getBlockWidthPx*MIN_SZ_MULT + padding)).toInt
    Math.min(TILE_MAX_COLUMNS,
      Math.max(TILE_MIN_COLUMNS, targetCount))
  }

  def getTileArgs()(implicit ctx: Context): TileArgs = {
    getTileArgs( ctx.deviceScreenOpt )
  }
  def getTileArgs(dscrOpt: Option[DevScreenT]): TileArgs = {
    dscrOpt match {
      case Some(dscr) =>
        getTileArgs(dscr)
      case None =>
        TileArgs(
          szMult = MIN_SZ_MULT,
          colsCount = TILE_MAX_COLUMNS
        )
    }
  }
  def getTileArgs(dscr: DevScreenT): TileArgs = {
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
  def getSzMult4tilesScr(colsCount: Int, dscr: DevScreenT): SzMult_t = {
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
  def fitBlockToScreen(bm: BlockMeta, dscr: DevScreenT): SzMult_t = {
    val hfloat = bm.height.toFloat
    val szMultIter0 = BlockHeights.values
      .iterator
      .map { _.heightPx }
      .filter { heightPx => heightPx < dscr.height && heightPx >= bm.height }
      .map { heightPx => heightPx.toFloat / hfloat }
    // для не-wide карточек также возможно отображение в двойном размере.
    val szMultIter1: Iterator[SzMult_t] = if (bm.wide) {
      szMultIter0
    } else {
      Iterator(FOCUSED_SZ_MULT) ++ szMultIter0
    }
    val maxHiter = (szMultIter1 ++ TILES_SZ_MULTS.iterator)
      .filter { szMult =>
        // Проверяем, влезает ли ширина на экран при таком раскладе?
        val w1 = getW1(szMult, colCnt = 1, blockWidth = bm.width, scrWidth = dscr.width, paddingPx = FOCUSED_PADDING_CSSPX)
        w1 >= MIN_W1
      }
    if (maxHiter.isEmpty)
      MIN_SZ_MULT
    else
      maxHiter.max
  }

}

