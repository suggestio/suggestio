package controllers.sc

import controllers.SioController
import io.suggest.common.css.OnlyOneAdTopLeft
import models._
import models.blk.{OneAdQsArgs, szMulted}
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
    val bgImgOptFut = AdRenderUtil.getBgImgOpt(mad, args)
    // Рендер, когда асинхронные операции будут завершены.
    bgImgOptFut map { bgImgOpt =>
      val brArgs = blk.RenderArgs(
        mad           = mad,
        szMult        = args.szMult,
        withEdit      = false,
        inlineStyles  = true,
        bgImg         = bgImgOpt,
        topLeft    = bgImgOpt.filter(_.isWide).map { wideCtx =>
          // TODO Нужно сдвигать sm-block div согласно запланированной в BgImg центровке, а не на середину.
          val blockWidth = szMulted(mad.blockMeta.width, args.szMult)
          val leftPx = (wideCtx.szCss.width - blockWidth) / 2
          OnlyOneAdTopLeft(leftPx)
        }
      )
      // TODO Нужен title, на основе имени узла-продьюсера например.
      val render = standaloneTpl() {
        _adTpl(brArgs)
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
    for {
      imgFile <- AdRenderUtil.renderAd2img(adArgs, request.mad)
    } yield {
      Ok.sendFile(imgFile,  inline = true,  onClose = {() => imgFile.delete()})
        .withHeaders(
          CONTENT_TYPE -> adArgs.imgFmt.mime
        )
    }
  }

}
