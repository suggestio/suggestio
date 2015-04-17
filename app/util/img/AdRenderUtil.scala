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


  /** Сгенерить контекст wide-рендера для рендера одинокой карточки. */
  def getWideCtxOpt(mad: MAd, args: OneAdQsArgs): Future[Option[IMakeResult]] = {
    // Генерация wideCtx на основе args.
    val wideFutOpt = for {
      wide        <- args.wideOpt
      bgImgInfo   <- BgImg.getBgImg(mad)
    } yield {
      val dscr = DevScreen(
        width  = wide.width,
        height = szMulted(mad.blockMeta.height, args.szMult),
        pixelRatioOpt = None    // TODO А какой надо выставлять?
      )
      val wArgs = MakeArgs(bgImgInfo, mad.blockMeta, args.szMult, Some(dscr))
      Makers.ScWide.icompile(wArgs)
        .map { Some.apply }
    }
    wideFutOpt getOrElse Future.successful(None)
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
