package controllers

import io.suggest.model.n2.edge.MNodeEdges
import models.blk.SzMult_t
import models.im.make.Makers
import models.msc.AdBodyTplArgs
import play.twirl.api.Html
import util.PlayMacroLogsI
import models._
import util.acl._
import scala.concurrent.Future
import controllers.ad.MarketAdFormUtil
import util.blocks.BgImg
import views.html.sc._adNormalTpl
import views.html.sc.foc._adFullTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 * 2015.feb.11: В этом контроллере так и остался ровно один экшен. Он был объединён с MarketAd через trait.
 */

trait MarketAdPreview
  extends SioController
  with PlayMacroLogsI
  with IsAdnNodeAdmin
{

  /**
   * Сабмит формы редактирования карточки для генерации превьюшки.
   * @param adnId id узла, в рамках которого происходит работа.
   * @param isFull true - надо полноэкранную преьюшку, false - нужен обычный размер.
   * @return 200 Ok с рендером.
   *         406 Not Acceptable при ошибочной форме.
   */
  def adFormPreviewSubmit(adnId: String, isFull: Boolean) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    MarketAdFormUtil.adFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
        NotAcceptable("Preview form bind failed.")
      },
      {r =>
        val bc = BlocksConf.DEFAULT
        for {
          imgs <- bc.asSavedImgs(r.bim)
          html <- {
            val mad = r.mad.copy(
              edges = r.mad.edges.copy(
                out = MNodeEdges.edgesToMap1 {
                  r.mad.edges.out.valuesIterator ++
                    imgs ++
                    Seq( MEdge(MPredicates.OwnedBy, adnId) )
                }
              )
            )
            if (isFull) {
              renderFull(mad)
            } else {
              renderSmall(mad)
            }
          }
        } yield {
          Ok(html)
        }
      }
    )
  }

  /** Рендер полноэкранного варианта отображения. */
  private def renderFull(mad: MNode)(implicit request: AbstractRequestForAdnNode[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 2.0F
    // Поддержка wideBg:
    val bgOptFut = BgImg.maybeMakeBgImg(mad, szMult, ctx.deviceScreenOpt)
    bgOptFut map { bgImgOpt =>
      val _brArgs = blk.RenderArgs(
        mad           = mad,
        withEdit      = false,
        bgImg         = bgImgOpt,
        inlineStyles  = true,
        szMult        = szMult,
        cssClasses    = Seq("__popup"),
        isFocused     = true
      )
      val args = AdBodyTplArgs(_brArgs, request.adnNode, 1, 1, is3rdParty = false)
      _adFullTpl(args)(ctx)
    }
  }

  /** Рендер маленькой превьюшки, прямо в редакторе. */
  private def renderSmall(mad: MNode)(implicit request: AbstractRequestForAdnNode[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 1.0F
    val bgOptFut = BgImg.maybeMakeBgImgWith(mad, Makers.Block, szMult, ctx.deviceScreenOpt)
    bgOptFut map { bgOpt =>
      val args = blk.RenderArgs(
        mad           = mad,
        withEdit      = true,
        szMult        = szMult,
        inlineStyles  = true,
        bgImg         = bgOpt,
        isFocused     = false
      )
      _adNormalTpl(args)(ctx)
    }
  }

}
