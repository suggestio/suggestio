package controllers.sc

import controllers.SioController
import models._
import models.blk.OneAdQsArgs
import util.PlayMacroLogsI
import util.acl.GetAnyAd
import util.blocks.BlocksConf
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

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
    val bc: BlockConf = BlocksConf(mad.blockMeta.blockId)
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
   * @param args Настройки выборки и отображения результата.
   * @return 200 Ok с картинкой.
   */
  def onlyOneAdAsImage(args: OneAdQsArgs) = GetAnyAd(args.adId).async { implicit request =>
    ???
  }

}
