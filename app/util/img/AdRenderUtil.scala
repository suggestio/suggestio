package util.img

import java.io.File

import controllers.routes
import io.suggest.common.fut.FutureUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.MNode
import models.blk.{OneAdQsArgs, szMulted}
import models.im._
import models.im.make.{MakeArgs, MakeResult, Makers}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.blocks.BgImg
import util.xplay.PlayUtil.httpPort

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.03.15 16:52
 * Description: Содержимое этого модуля выросло внутри WkHtmlUtil.
 */
object AdRenderUtil {

  /**
   * Генерации абсолютной ссылки на отрендеренную в картинку рекламную карточку.
   * @param adArgs Параметры рендера.
   * @return Строка с абсолютной ссылкой на локалхост.
   */
  def adImgLocalUrl(adArgs: OneAdQsArgs): String = {
    "http://localhost:" + httpPort + routes.MarketShowcase.onlyOneAd(adArgs).url
  }


  /**
   * Сгенерить данные для рендера фоновой картинки для рендера одинокой карточки.
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

      val margs = MakeArgs(bgImg, bm, args.szMult, Some(dscr))
      maker.icompile(margs)
        .map { Some.apply }
    }

    FutureUtil.optFut2futOpt(optFut)(identity)
  }


  /**
   * Рендер указанной рекламной карточки
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
    val renderArgs = AdRenderArgs.RENDERER.forArgs(
      src    = adImgLocalUrl(adArgs),
      scrSz  = MImgInfoMeta(width = extWidth, height = height),
      outFmt = fmt
    )

    // Запускаем генерацию результата
    renderArgs.renderCached
  }


}
