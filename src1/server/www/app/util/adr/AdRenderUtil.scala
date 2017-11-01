package util.adr

import java.io.File
import javax.inject.{Inject, Named, Singleton}

import controllers.routes
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.{EdgeUid_t, MEdge}
import io.suggest.model.n2.node.MNode
import models.adr.MAdRenderArgs
import models.blk.{OneAdQsArgs, szMulted}
import models.im._
import models.im.make.{IMaker, MakeArgs, MakeResult, Makers}
import models.mproj.ICommonDi
import util.adr.phantomjs.{PhantomJsRrrDiFactory, PhantomJsRrrUtil}
import util.adr.wkhtml.{WkHtmlRrrDiFactory, WkHtmlRrrUtil}
import util.blocks.BgImg
import util.xplay.PlayUtil

import scala.concurrent.Future
import scalaz.Tree
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.03.15 16:52
 * Description: Содержимое этого модуля выросло внутри WkHtmlUtil.
 */
@Singleton
class AdRenderUtil @Inject() (
                               @Named("blk") blkImgMaker : IMaker,
                               playUtil                  : PlayUtil,
                               mCommonDi                 : ICommonDi
                             ) {

  import mCommonDi._
  import current.injector

  private def _wkHtmlFactory = (
    injector.instanceOf[WkHtmlRrrDiFactory],
    injector.instanceOf[WkHtmlRrrUtil]
  )
  private def _phantomJsFactory = (
    injector.instanceOf[PhantomJsRrrDiFactory],
    injector.instanceOf[PhantomJsRrrUtil]
  )

  /** Используемый по умолчанию рендерер. Влияет на дефолтовый рендеринг карточки. */
  val (_RRR_FACTORY, _RRR_UTIL) = {
    val ck = "ad.render.renderer.dflt"
    configuration.getOptional[String](ck)
      .fold [(IAdRrrDiFactory, IAdRrrUtil)] (_wkHtmlFactory) { raw =>
        if ( raw.startsWith("wkhtml") ) {
          _wkHtmlFactory
        } else if (raw.startsWith("phantom")) {
          _phantomJsFactory
        } else {
          throw new IllegalStateException("Unknown ad2img renderer: " + ck + " = " + raw)
        }
      }
  }

  /**
   * Генерации абсолютной ссылки на отрендеренную в картинку рекламную карточку.
   *
   * @param adArgs Параметры рендера.
   * @return Строка с абсолютной ссылкой на локалхост.
   */
  def adImgLocalUrl(adArgs: OneAdQsArgs): String = {
    "http://localhost:" + playUtil.httpPort + routes.Sc.onlyOneAd(adArgs).url
  }


  /**
   * Сгенерить данные для рендера фоновой картинки для рендера одинокой карточки.
   *
   * @param mad Карточка.
   * @param args Переданные через qs параметры рендера.
   * @return Future None, если у карточки нет фоновой картинки.
   *         Future Some() если есть картинка. Широкий фон или нет -- зависит от args.
   */
  def getBgImgOpt(mad: MNode, args: OneAdQsArgs): Future[Option[MakeResult]] = {
    val optFut = for {
      // Генерация данных по фоновой картинке карточки.
      bgImg <- BgImg.getBgImg(mad)
      bm    <- mad.ad.blockMeta
    } yield {

      // Высота виртуального экрана и плотность пикселей всегда одинаковая.
      val dscrF = DevScreen(
        _ : Int,
        height = szMulted(bm.height, args.szMult),
        pixelRatioOpt = Some(DevPixelRatios.MDPI)
      )

      // Дальше есть выбор между wide и не-wide рендером.
      val (maker, dscr) = args.wideOpt match {
        case Some(wide) =>
          (Makers.StrictWide, dscrF(wide.width))
        // Нет wide-аргументов. Рендерим как block.
        case None =>
          val width = szMulted(bm.width, args.szMult)
          val dscr = dscrF(width)
          (Makers.Block, dscr)
      }

      val imaker = current.injector.instanceOf( maker.makerClass )
      val margs = MakeArgs(bgImg, bm, args.szMult, Some(dscr))

      for (res <- imaker.icompile(margs)) yield {
        Some(res)
      }
    }

    FutureUtil.optFut2futOpt(optFut)(identity)
  }


  /**
   * Рендер указанной рекламной карточки
   *
   * @param adArgs Данные по рендеру.
   * @param mad карточка для рендера.
   * @return Фьючерс с байтами картинки.
   */
  def renderAd2img(adArgs: OneAdQsArgs, mad: MNode): Future[File] = {
    val bm = mad.ad.blockMeta.get

    // Высота отрендеренной карточки с учетом мультипликатора
    lazy val width0 = szMulted(bm.width, adArgs.szMult)
    val height = szMulted(bm.height, adArgs.szMult)

    // Eсли запрошен широкий рендер, то нужно рассчитывать кроп и размер экрана с учётом квантования фоновой картинки.
    // Внешняя полная ширина отрендеренной широкой карточки.
    // Если Без wide, значит можно рендерить карточку as-is.
    val extWidth = adArgs.wideOpt.fold(width0)(_.width)

    // Собираем параметры рендера воедино.
    val fmt = adArgs.imgFmt
    val scrSz = MSize2di(width = extWidth, height = height)
    val rArgs = MAdRenderArgs(
      src     = adImgLocalUrl(adArgs),
      scrSz   = scrSz,
      outFmt  = fmt,
      quality = _RRR_UTIL.qualityDflt(scrSz, fmt)
    )

    // Запускаем генерацию результата
    _RRR_FACTORY
      .instance(rArgs)
      .renderCached
  }


}


/** Интерфейс для поля с DI-инстансом [[AdRenderUtil]]. */
trait IAdRenderUtilDi {
  def adRenderUtil: AdRenderUtil
}
