package controllers.sc

import controllers.{routes, SioController}
import models._
import models.blk.OneAdQsArgs
import models.im.{OutImgFmts, WkHtmlArgs}
import util.PlayMacroLogsI
import util.acl.GetAnyAd
import util.blocks.BlocksConf
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.img.WkHtmlUtil
import util.play.PlayUtil.httpPort

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
  def onlyOneAd(args: OneAdQsArgs) = GetAnyAd(args.adId) { implicit request =>
    import request.mad
    val bc: BlockConf = BlocksConf applyOrDefault mad.blockMeta.blockId
    val brArgs = blk.RenderArgs(
      szMult        = args.szMult,
      isStandalone  = true,
      withEdit      = false,
      inlineStyles  = true
    )
    cacheControlShort {
      Ok( bc.renderBlock(mad, brArgs) )
    }
  }

  /**
   * Рендер одной указанной карточки в виде картинки.
   * Для рендера используется внешняя утилита wkhtml2image, которая обращается к соответсвующему html-экшену.
   * @param adArgs Настройки выборки и отображения результата.
   * @return 200 Ok с картинкой.
   */
  def onlyOneAdAsImage(adArgs: OneAdQsArgs) = GetAnyAd(adArgs.adId).async { implicit request =>
    val fmt = OutImgFmts.PNG
    val wkArgs = WkHtmlArgs(
      src     = s"http://localhost:$httpPort${routes.MarketShowcase.onlyOneAd(adArgs).url}",
      imgSize = request.mad.blockMeta,
      outFmt  = fmt,
      plugins = false
    )
    WkHtmlUtil.html2imgSimpleCached(wkArgs) map { imgBytes =>
      Ok(imgBytes).withHeaders(
        CONTENT_TYPE -> fmt.mime
      )
    }
  }

}
