package controllers.sc

import controllers.SioControllerApi
import io.suggest.common.css.OnlyOneAdTopLeft
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.blk.{OneAdQsArgs, RenderArgs, szMulted}
import util.acl.GetAnyAd
import util.adr.AdRenderUtil
import util.adv.AdvUtil
import views.html.blocks.common.standaloneTpl
import views.html.sc._adTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 16:48
 * Description: Аддон для контроллера, который занимается раздаванием ровно одной карточки.
 */
final class ScOnlyOneAd @Inject() (
                                    sioControllerApi: SioControllerApi,
                                  )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.ec
  import sioControllerApi.mCommonDi.current.injector


  private lazy val getAnyAd = injector.instanceOf[GetAnyAd]
  private lazy val adRenderUtil = injector.instanceOf[AdRenderUtil]
  private lazy val advUtil = injector.instanceOf[AdvUtil]


  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   *
   * @param args Настройки выборки и отображения результата.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def onlyOneAd(args: OneAdQsArgs) = getAnyAd(args.adId).async { implicit request =>
    import request.mad
    val bgImgOptFut = adRenderUtil.getBgImgOpt(mad, args)
    // Рендер, когда асинхронные операции будут завершены.
    for (bgImgOpt <- bgImgOptFut) yield {
      // Собираем аргументы для рендера карточки.
      val brArgs = RenderArgs(
        mad           = mad,
        szMult        = args.szMult,
        withEdit      = false,
        inlineStyles  = true,
        bgImg         = bgImgOpt,
        topLeft       = {
          for {
            wideCtx <- bgImgOpt
            if wideCtx.isWide
            mainTpl      <- advUtil.getAdvMainBlock(mad)
            widthPx      <- mainTpl.rootLabel.props1.widthPx
          } yield {
            // TODO Нужно сдвигать sm-block div согласно запланированной в BgImg центровке, а не на середину.
            val blockWidth = szMulted(widthPx, args.szMult)
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
  def onlyOneAdAsImage(adArgs: OneAdQsArgs) = getAnyAd(adArgs.adId).async { implicit request =>
    for {
      imgFile <- adRenderUtil.renderAd2img(adArgs, request.mad)
    } yield {
      Ok.sendFile(imgFile,  inline = true,  onClose = {() => imgFile.delete()})
        .as(adArgs.imgFmt.mime)
    }
  }

}
