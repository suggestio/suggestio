package controllers

import models.blk.SzMult_t
import models.im.make.{IMakeResult, Makers}
import play.twirl.api.Html
import util.PlayMacroLogsI
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.FormUtil._
import play.api.data._
import util.acl._
import scala.concurrent.Future
import play.api.mvc.Request
import controllers.ad.MarketAdFormUtil
import util.blocks.{BgImg, BlockMapperResult}
import views.html.sc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 * 2015.feb.11: В этом контроллере так и остался ровно один экшен. Он был объединён с MarketAd через trait.
 */

trait MarketAdPreview extends SioController with PlayMacroLogsI {

  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = {
    MarketAdFormUtil.getAdFormM(adCatIdsM, blockM)
  }

  private def detectAdPreviewForm(adnNode: MAdnNode)(implicit request: Request[collection.Map[String, Seq[String]]]) = {
    maybeGetAdPreviewFormM(adnNode, request.body)
  }

  protected def blockIdsFor(adnNode: MAdnNode): Set[Int]

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def maybeGetAdPreviewFormM(adnNode: MAdnNode, reqBody: collection.Map[String, Seq[String]]): Either[AdFormM, (BlockConf, AdFormM)] = {
    // TODO adModes пора выпиливать. И этот Either заодно.
    val adModes = reqBody.getOrElse("ad.offer.mode", Nil)
    adModes.headOption.flatMap { adModeStr =>
      AdOfferTypes.maybeWithName(adModeStr)
    }.fold[Either[AdFormM, (BlockConf, AdFormM)]] {
      LOGGER.warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
      val form = getPreviewAdFormM( BlocksConf.DEFAULT.strictMapping )
        .withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
      Left(form)
    } { case AdOfferTypes.BLOCK =>
      val maybeBlockIdRaw = reqBody.get("ad.offer.blockId")
      maybeBlockIdRaw
        .getOrElse(Nil)
        .headOption
        .map[BlockConf] { blockIdStr => BlocksConf(blockIdStr.toInt) }
        .filter { block => blockIdsFor(adnNode) contains block.id }
        .fold[Either[AdFormM, (BlockConf, AdFormM)]] {
          // Задан пустой или скрытый/неправильный block_id.
          LOGGER.warn("detectAdForm(): valid block_id not found, raw block ids = " + maybeBlockIdRaw)
          val form = getPreviewAdFormM( BlocksConf.DEFAULT.strictMapping )
            .withGlobalError("ad.blockId.undefined.or.invalid")
          Left(form)
        } { blockConf =>
          val result = blockConf -> getPreviewAdFormM(blockConf.strictMapping)
          Right(result)
        }
    }
  }

  /** Сабмит формы редактирования карточки для генерации превьюшки.
    * @param adnId id узла, в рамках которого происходит работа.
    * @param isFull true - надо полноэкранную преьюшку, false - нужен обычный размер.
    * @return 200 Ok с рендером.
    *         406 Not Acceptable при ошибочной форме.
    */
  def adFormPreviewSubmit(adnId: String, isFull: Boolean) = IsAdnNodeAdminPost(adnId).async(parse.urlFormEncoded) { implicit request =>
    detectAdPreviewForm(request.adnNode) match {
      case Right((bc, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            LOGGER.debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
            NotAcceptable("Preview form bind failed.")
          },
          {case (mad0, bim) =>
            val imgsFut: Future[Imgs_t] = Future.traverse(bim) {
              case (k, i4s) =>
                i4s.getImageWH map {
                  imgMetaOpt  =>  k -> MImgInfo(i4s.fileName, meta = imgMetaOpt)
                }
            } map {
              _.toMap
            }
            imgsFut.flatMap { imgs =>
              val mad = mad0.copy(
                producerId = adnId,
                imgs = imgs
              )
              val renderFut: Future[Html] = if (isFull) {
                renderFull(mad)
              } else {
                renderSmall(mad)
              }
              renderFut
                .map { Ok(_) }
            }
          }
        )

      case Left(formWithGlobalError) =>
        NotAcceptable("Form mode invalid")
    }
  }

  /** Рендер полноэкранного варианта отображения. */
  private def renderFull(mad: MAd)(implicit request: AbstractRequestForAdnNode[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 2.0F
    // Поддержка wideBg:
    val bgOptFut = BgImg.maybeMakeBgImg(mad, szMult, ctx.deviceScreenOpt)
    bgOptFut map { bgImgOpt =>
      val args = blk.RenderArgs(
        withEdit      = false,
        bgImg         = bgImgOpt,
        inlineStyles  = true,
        szMult        = szMult,
        withCssClasses = Seq("__popup")
      )
      _adFullTpl(mad, producer = request.adnNode, args = args)(ctx)
    }
  }

  /** Рендер маленькой превьюшки, прямо в редакторе. */
  private def renderSmall(mad: MAd)(implicit request: AbstractRequestForAdnNode[_], ctx: Context): Future[Html] = {
    val szMult: SzMult_t = 1.0F
    val bgOptFut = BgImg.maybeMakeBgImgWith(mad, Makers.Block, szMult, ctx.deviceScreenOpt)
    bgOptFut map { bgOpt =>
      val args = blk.RenderArgs(
        withEdit      = true,
        szMult        = szMult,
        inlineStyles  = true,
        bgImg         = bgOpt
      )
      _adNormalTpl(mad, args = args)(ctx)
    }
  }

}
