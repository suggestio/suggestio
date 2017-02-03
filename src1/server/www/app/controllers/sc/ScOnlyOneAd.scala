package controllers.sc

import controllers.SioController
import io.suggest.common.css.OnlyOneAdTopLeft
import io.suggest.util.logs.IMacroLogs
import models._
import models.blk.{OneAdQsArgs, szMulted}
import util.acl.GetAnyAd
import util.adr.IAdRenderUtilDi
import views.html.blocks.common.standaloneTpl
import views.html.sc._adTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 16:48
 * Description: Аддон для контроллера, который занимается раздаванием ровно одной карточки.
 */
trait ScOnlyOneAd
  extends SioController
  with IMacroLogs
  with GetAnyAd
  with IAdRenderUtilDi
{

  import mCommonDi._

  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   *
   * @param args Настройки выборки и отображения результата.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def onlyOneAd(args: OneAdQsArgs) = GetAnyAd(args.adId).async { implicit request =>
    import request.mad
    val bgImgOptFut = adRenderUtil.getBgImgOpt(mad, args)
    val bc = BlocksConf.applyOrDefault( request.mad )
    // Рендер, когда асинхронные операции будут завершены.
    for (bgImgOpt <- bgImgOptFut) yield {
      // Собираем аргументы для рендера карточки.
      val brArgs = blk.RenderArgs(
        mad           = mad,
        bc            = bc,
        szMult        = args.szMult,
        withEdit      = false,
        inlineStyles  = true,
        bgImg         = bgImgOpt,
        topLeft       = {
          for {
            wideCtx <- bgImgOpt
            if wideCtx.isWide
            bm      <- mad.ad.blockMeta
          } yield {
            // TODO Нужно сдвигать sm-block div согласно запланированной в BgImg центровке, а не на середину.
            val blockWidth = szMulted(bm.width, args.szMult)
            val leftPx = (wideCtx.szCss.width - blockWidth) / 2
            OnlyOneAdTopLeft(leftPx)
          }
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
   *
   * @param adArgs Настройки выборки и отображения результата.
   * @return 200 Ok с картинкой.
   */
  def onlyOneAdAsImage(adArgs: OneAdQsArgs) = GetAnyAd(adArgs.adId).async { implicit request =>
    for {
      imgFile <- adRenderUtil.renderAd2img(adArgs, request.mad)
    } yield {
      Ok.sendFile(imgFile,  inline = true,  onClose = {() => imgFile.delete()})
        .as(adArgs.imgFmt.mime)
    }
  }

}
