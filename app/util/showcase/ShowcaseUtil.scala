package util.showcase

import controllers.routes
import io.suggest.ym.model.MAd
import io.suggest.ym.model.common.{AdShowLevels, IBlockMeta}
import models._
import models.blk.{BlockWidth, BlockHeights, BlockWidths}
import models.im.DevPixelRatios
import util.blocks.BgImg
import play.api.Play.{current, configuration}
import util.cdn.CdnUtil
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
  def getCats(adnIdOpt: Option[String]) = {
    val catAdsSearch = AdSearch(
      receiverIds   = adnIdOpt.toList,
      maxResultsOpt = Some(100),
      levels        = List(AdShowLevels.LVL_CATS)
    )
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
    (catsStatsFut, mmcatsFut)
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
  def FOCUSED_SZ_MULT = 2

  /** Дефолтовые аргументы рендера на черный день. Обычно не важно, что там написано в полях. */
  def focusedBrArgsDflt = blk.RenderArgs(szMult = FOCUSED_SZ_MULT)

  /**
   * Аргументы для рендера блока, когда карточка открыта.
   * @param mad Рекламная карточка.
   * @return Аргументы для рендера.
   */
  def focusedBrArgsFor(mad: MAdT)(implicit ctx: Context): Future[blk.RenderArgs] = {
    // Считаем целевое разрешение фоновой картинки карточки:
    val preferDoubleSize: Boolean = {
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
    val willWideBg: Boolean = mad.blockMeta.wide
    val szMult = if (preferDoubleSize || mad.blockMeta.height == BlockHeights.H140.heightPx) {
      2
    } else {
      1
    }
    val wideBgCtxOptFut: Future[Option[blk.WideBgRenderCtx]] = {
      if (willWideBg) {
        // Нужно получить данные для рендера широкой карточки.
        val bc = BlocksConf applyOrDefault mad.blockMeta.blockId
        bc.wideBgImgArgs(mad, szMult)
      } else {
        Future successful None
      }
    }
    wideBgCtxOptFut map { wideBgCtxOpt =>
      blk.RenderArgs(
        withEdit      = false,
        isStandalone  = false,
        szMult        = szMult,
        wideBg        = wideBgCtxOpt
      )
    }
  }

}

