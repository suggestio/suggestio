package controllers.sc

import controllers.SioController
import models._
import models.blk.OneAdQsArgs
import models.im.{DevScreen, OutImgFmts}
import util.PlayMacroLogsI
import util.acl.GetAnyAd
import util.blocks.{BgImg, BlocksConf}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.img.WkHtmlUtil
import views.html.blocks.common.standaloneTpl
import views.html.sc._adTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 16:48
 * Description: Аддон для контроллера, который занимается раздаванием ровно одной карточки.
 */
trait ScOnlyOneAd extends SioController with PlayMacroLogsI {

  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   * @param args Настройки выборки и отображения результата.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def onlyOneAd(args: OneAdQsArgs) = GetAnyAd(args.adId).async { implicit request =>
    import request.mad
    val bc = BlocksConf applyOrDefault mad.blockMeta.blockId
    // Генерация wideCtx на основе args.
    val wideFutOpt = for {
      wide        <- args.wideOpt
      bgImgInfo   <- bc.getMadBgImg(mad)
    } yield {
      val dscr = DevScreen(
        width  = (wide.width * args.szMult).toInt,
        height = (mad.blockMeta.height * args.szMult).toInt,
        pixelRatioOpt = None    // TODO А какой надо выставлять?
      )
      BgImg.wideBgImgArgs(bgImgInfo, mad.blockMeta, args.szMult, Some(dscr))
        .map { Some.apply }
    }
    val wideOptFut = wideFutOpt getOrElse Future.successful(None)
    // Рендер, когда асинхронные операции будут завершены.
    wideOptFut map { wideCtxOpt =>
      val brArgs = blk.RenderArgs(
        szMult        = args.szMult,
        withEdit      = false,
        inlineStyles  = true,
        wideBg        = wideCtxOpt,
        blockStyle    = wideCtxOpt.map { _ => "position: absolute; top: 0px; left: 0px;" }
      )
      // TODO Нужен title, на основе имени узла-продьюсера например.
      val render = standaloneTpl() {
        _adTpl(mad, brArgs)
      }
      Ok(render)
    }
  }

  /**
   * Рендер одной указанной карточки в виде картинки.
   * Для рендера используется внешняя утилита wkhtml2image, которая обращается к соответсвующему html-экшену.
   * @param adArgs Настройки выборки и отображения результата.
   * @return 200 Ok с картинкой.
   */
  def onlyOneAdAsImage(adArgs: OneAdQsArgs) = GetAnyAd(adArgs.adId).async { implicit request =>
    val fmt = OutImgFmts.JPEG
    WkHtmlUtil.renderAd2img(adArgs, request.mad, fmt)
      .map { imgBytes =>
        Ok(imgBytes).withHeaders(
          CONTENT_TYPE -> fmt.mime
        )
      }
  }

}
