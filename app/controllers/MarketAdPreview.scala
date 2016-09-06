package controllers

import io.suggest.model.n2.edge.MNodeEdges
import models.blk.SzMult_t
import models.mctx.Context
import models.msc.AdBodyTplArgs
import models.req.INodeReq
import play.twirl.api.Html
import util.PlayMacroLogsI
import models._
import util.acl._
import util.ad.IMarketAdFormUtil

import scala.concurrent.Future
import util.blocks.{BgImg, IBlkImgMakerDI}
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
  with IBlkImgMakerDI
  with IMarketAdFormUtil
{

  import mCommonDi._

  /**
   * Сабмит формы редактирования карточки для генерации превьюшки.
   * @param adnId id узла, в рамках которого происходит работа.
   * @param isFull true - надо полноэкранную преьюшку, false - нужен обычный размер.
   * @return 200 Ok с рендером.
   *         406 Not Acceptable при ошибочной форме.
   */
  def adFormPreviewSubmit(adnId: String, isFull: Boolean) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    marketAdFormUtil.adFormM.bindFromRequest().fold(
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
                  val ownEdge = MEdge(
                    predicate = MPredicates.OwnedBy,
                    nodeIds   = request.mnode.id.toSet
                  )
                  r.mad.edges.out.valuesIterator ++
                    imgs ++
                    Seq( ownEdge )
                }
              )
            )
            if (isFull) {
              renderFull(mad, bc)
            } else {
              renderSmall(mad, bc)
            }
          }
        } yield {
          Ok(html)
        }
      }
    )
  }

  /** Рендер полноэкранного варианта отображения. */
  private def renderFull(mad: MNode, bc: BlockConf)(implicit request: INodeReq[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 2.0F
    // Поддержка wideBg:
    for {
      bgImgOpt <- BgImg.maybeMakeBgImg(mad, szMult, ctx.deviceScreenOpt)
    } yield {
      val _brArgs = blk.RenderArgs(
        mad           = mad,
        bc            = bc,
        withEdit      = false,
        bgImg         = bgImgOpt,
        inlineStyles  = true,
        szMult        = szMult,
        cssClasses    = Seq("__popup"),
        isFocused     = true
      )
      val args = AdBodyTplArgs(_brArgs, request.mnode, 1, 1, is3rdParty = false)
      _adFullTpl(args)(ctx)
    }
  }

  /** Рендер маленькой превьюшки, прямо в редакторе. */
  private def renderSmall(mad: MNode, bc: BlockConf)(implicit request: INodeReq[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 1.0F

    for {
      bgOpt <- BgImg.maybeMakeBgImgWith(mad, blkImgMaker, szMult, ctx.deviceScreenOpt)
    } yield {
      val args = blk.RenderArgs(
        mad           = mad,
        bc            = bc,
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
