package util.img

import controllers.routes
import io.suggest.ym.model.common.MImgInfoMeta
import models.blk.szMulted
import models.im.make.{IMakeResult, Makers, MakeArgs}
import models.{MAd, MAdT}
import models.blk.OneAdQsArgs
import util.blocks.BgImg
import util.xplay.PlayUtil.httpPort
import models.im._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
  def getBgImgOpt(mad: MAd, args: OneAdQsArgs): Future[Option[IMakeResult]] = {
    // Генерация данных по фоновой картинке карточки.
    BgImg.getBgImg(mad) match {
      // Фоновая картинка у карточки задана.
      case Some(bgImg) =>
        // Высота виртуального экрана и плотность пикселей всегда одинаковая.
        val pxRatioOpt = Some(DevPixelRatios.MDPI)
        val height = szMulted(mad.blockMeta.height, args.szMult)
        // Дальше есть выбор между wide и не-wide рендером.
        val (maker, dscr) = args.wideOpt match {
          case Some(wide) =>
            val dscr = DevScreen(
              width  = wide.width,
              height = height,
              pixelRatioOpt = pxRatioOpt
            )
            (Makers.ScWide, dscr)
          // Нет wide-аргументов. Рендерим как block.
          case None =>
            val dscr = DevScreen(
              width  = szMulted(mad.blockMeta.width, args.szMult),
              height = height,
              pixelRatioOpt = pxRatioOpt
            )
            (Makers.Block, dscr)
        }
        val margs = MakeArgs(bgImg, mad.blockMeta, args.szMult, Some(dscr))
        maker.icompile(margs)
          .map { Some.apply }
      // Нет фоновой картинки
      case None =>
        Future successful Option.empty[IMakeResult]
    }
  }


  /**
   * Рендер указанной рекламной карточки
   * @param adArgs Данные по рендеру.
   * @param mad карточка для рендера.
   * @return Фьючерс с байтами картинки.
   */
  def renderAd2img(adArgs: OneAdQsArgs, mad: MAdT): Future[Array[Byte]] = {
    val sourceAdSz = mad.blockMeta
    // Высота отрендеренной карточки с учетом мультипликатора
    lazy val width0 = szMulted(sourceAdSz.width, adArgs.szMult)
    val height = szMulted(sourceAdSz.height, adArgs.szMult)
    // Eсли запрошен широкий рендер, то нужно рассчитывать кроп и размер экрана с учётом квантования фоновой картинки.
    // Внешняя полная ширина отрендеренной широкой карточки.
    // Если Без wide, значит можно рендерить карточку as-is.
    val extWidth = adArgs.wideOpt.fold(width0)(_.width)
    // Запускаем генерацию результата
    val fmt = adArgs.imgFmt
    val renderArgs = AdRenderArgs.RENDERER.forArgs(
      src         = adImgLocalUrl(adArgs),
      scrSz       = MImgInfoMeta(width = extWidth, height = height),
      outFmt      = fmt
    )
    renderArgs.renderCached
  }


}
