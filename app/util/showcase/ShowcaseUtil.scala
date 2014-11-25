package util.showcase

import controllers.routes
import io.suggest.ym.model.MAd
import io.suggest.ym.model.common.{AdShowLevels, IBlockMeta}
import models._
import models.blk.{SzMult_t, BlockWidth, BlockHeights, BlockWidths}
import models.im.DevPixelRatios
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
  val SHOW_EMPTY_CATS = configuration.getBoolean("market.frontend.cats.empty.show") getOrElse false

  /** Путь до ассета со скриптом выдачи. */
  val SHOWCASE_JS_ASSET = configuration.getString("showcase.js.asset.path") getOrElse "javascripts/market/showcase/showcase2.js"

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
  def FOCUSED_SZ_MULT: SzMult_t = 2.0F

  /** Дефолтовые аргументы рендера на черный день. Обычно не важно, что там написано в полях. */
  def focusedBrArgsDflt = blk.RenderArgs(szMult = FOCUSED_SZ_MULT)

  /**
   * Аргументы для рендера блока, когда карточка открыта.
   * @param mad Рекламная карточка.
   * @param inlineStyles Значение поля RenderArgs.inlineStyles в экземпляре результата.
   * @return Аргументы для рендера.
   */
  def focusedBrArgsFor(mad: MAdT, inlineStyles: Boolean = true)(implicit ctx: Context): Future[blk.RenderArgs] = {
    if (mad.blockMeta.wide) {
      // мультипликатор размера блока получаем на основе отношения высоты блока к целевой высоте фоновой картинки.
      val szMult: SzMult_t = BgImg.WIDE_TARGET_HEIGHT_PX.toFloat / mad.blockMeta.height.toFloat
      // Нужно получить данные для рендера широкой карточки.
      val bc = BlocksConf applyOrDefault mad.blockMeta.blockId
      val wideBgCtxOptFut = bc.wideBgImgArgs(mad, szMult)
      wideBgCtxOptFut map { wideBgCtxOpt =>
        blk.RenderArgs(
          withEdit      = false,
          isStandalone  = false,
          szMult        = szMult,
          wideBg        = wideBgCtxOpt,
          inlineStyles  = inlineStyles
        )
      }

    } else {

      // Если ширина экрана позволяет, то нужно отрендерить в увеличенном размере:
      val devScrHasDoubleWidth: Boolean = {
        ctx.deviceScreenOpt.exists { devScr =>
          val nonWideImgRenderSz = BgImg.getRenderSz(
            szMult      = FOCUSED_SZ_MULT,
            blockMeta   = mad.blockMeta,
            devScreenSz = devScr,
            pxRatioOpt  = Some(DevPixelRatios.MDPI)   // Узнаём реально отображаемое разрешение в css-пикселях.
          )
          // Если ширина экрана намекает, то рендерим на широкую.
          nonWideImgRenderSz.width  <  devScr.width
        }
      }
      // Рендерить в wide? Да, если карточка разрешает и разрешение экрана не противоречит этому
      val szMult: SzMult_t = if (devScrHasDoubleWidth || mad.blockMeta.height == BlockHeights.H140.heightPx) {
        FOCUSED_SZ_MULT
      } else {
        1F
      }
      // Возвращаем результат
      val bra = blk.RenderArgs(
        withEdit      = false,
        isStandalone  = false,
        szMult        = szMult,
        wideBg        = None,
        inlineStyles  = inlineStyles
      )
      Future successful bra
    }
  }


  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  val TILES_SZ_MULTS: List[SzMult_t] = configuration.getDoubleSeq("sc.tiles.szmults")
    .map { _.map(_.toFloat).toList }
    .getOrElse { List(1.1F, 1.2F, 1.3F, 1.4F) }
  /** Минимальные отступы по бокам. */
  val TILE_SIDE_PADDING_MIN_CSSPX: Int = configuration.getInt("sc.tiles.padding.side.min.csspx") getOrElse 10
  /** Горизонтальное расстояние между блоками. */
  val TILE_PADDING_CSSPX = configuration.getInt("sc.tiles.padding.between.blocks.csspx") getOrElse 20
  /** Макс. кол-во вертикальных колонок. */
  val TILE_MAX_COLUMNS = configuration.getInt("sc.tiles.columns.max") getOrElse 4

  /**
   * Вычислить мультипликатор размера для плиточной выдачи с целью подавления лишних полей по бокам.
   * @param ctx Контекст грядущего рендера.
   * @return SzMult_t выбранный для рендера.
   */
  def getSzMult4tiles(implicit ctx: Context): SzMult_t = {
    val szMultDflt: SzMult_t = 1.0F
    ctx.deviceScreenOpt.fold [SzMult_t] (szMultDflt) { dscr =>
      val blockWidthPx = BlockWidths.NORMAL.widthPx
      // Кол-во колонок на экране:
      val colCnt = Math.min(TILE_MAX_COLUMNS, dscr.width / blockWidthPx)
      @tailrec def detectSzMult(lastSzMult: SzMult_t, restSzMults: List[SzMult_t]): SzMult_t = {
        if (restSzMults.isEmpty) {
          lastSzMult
        } else {
          val nextSzMult = restSzMults.head
          // Вычислить остаток ширины за вычетом всех отмасштабированных блоков, с запасом на боковые поля.
          val bwPxW = (blockWidthPx * nextSzMult).toInt
          val w1 = dscr.width  - colCnt * bwPxW  - TILE_PADDING_CSSPX * 2 * (colCnt - 1)
          // Если ещё остался запас по высоте, то ещё увеличить масштабирование и повторить попытку.
          if (w1 > 0)
            detectSzMult(nextSzMult, restSzMults.tail)
          else if (w1 == 0)
            nextSzMult
          else
            lastSzMult
        }
      }
      detectSzMult(szMultDflt, TILES_SZ_MULTS)
    }
  }


}

