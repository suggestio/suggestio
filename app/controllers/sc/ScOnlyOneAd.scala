package controllers.sc

import controllers.SioController
import models._
import models.blk.OneAdQsArgs
import models.im.OutImgFmts
import util.PlayMacroLogsI
import util.acl.GetAnyAd
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.img.AdRenderUtil
import views.html.blocks.common.standaloneTpl
import views.html.sc._adTpl

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
    val wideOptFut = AdRenderUtil.getWideCtxOpt(mad, args)
    // Рендер, когда асинхронные операции будут завершены.
    wideOptFut map { wideCtxOpt =>
      val brArgs = blk.RenderArgs(
        szMult        = args.szMult,
        withEdit      = false,
        inlineStyles  = true,
        wideBg        = wideCtxOpt,
        blockStyle    = wideCtxOpt.map { wideCtx =>
          // TODO Нужно сдвигать sm-block div согласно запланированной в BgImg центровке, а не на середину.
          val blockWidth = (mad.blockMeta.width * args.szMult).toInt
          val leftPx = (wideCtx.szCss.width - blockWidth) / 2
          s"position: absolute; top: 0px; left: ${leftPx}px;"
        }
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
    AdRenderUtil.renderAd2img(adArgs, request.mad, fmt)
      .map { imgBytes =>
        Ok(imgBytes).withHeaders(
          CONTENT_TYPE -> fmt.mime
        )
      }
  }

}
