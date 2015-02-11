package controllers

import util.PlayMacroLogsI
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.FormUtil._
import play.api.data._
import util.acl._
import scala.concurrent.Future
import play.api.mvc.Request
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import util.blocks.BlockMapperResult
import views.html.market.showcase._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 * 2015.feb.11: В этом контроллере так и остался ровно один экшен. Он был объединён с MarketAd через trait.
 */

trait MarketAdPreview extends SioController with PlayMacroLogsI {

  /** Сборка маппинга формы рекламной карточки. */
  protected def getAdFormM(catIdM: Mapping[Set[String]], blockM: Mapping[BlockMapperResult]): AdFormM

  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = {
    getAdFormM(adCatIdsM, blockM)
  }

  private def detectAdPreviewForm(adnNode: MAdnNode)(implicit request: Request[collection.Map[String, Seq[String]]]) = {
    maybeGetAdPreviewFormM(adnNode, request.body)
  }


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
        .filter { block => MarketAd.blockIdsFor(adnNode) contains block.id }
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
          {case (mad, bim) =>
            val imgsFut: Future[Imgs_t] = Future.traverse(bim) {
              case (k, i4s) =>
                i4s.getImageWH map {
                  imgMetaOpt  =>  k -> MImgInfo(i4s.fileName, meta = imgMetaOpt)
                }
            } map {
              _.toMap
            }
            mad.producerId = adnId
            for {
              imgs <- imgsFut
            } yield {
              mad.imgs = imgs
              val render = if (isFull) {
                // TODO Нужна поддержка wideBg здесь.
                val args = blk.RenderArgs(
                  withEdit      = false,
                  isStandalone  = false,
                  inlineStyles  = true,
                  szMult        = 2.0F,
                  withCssClasses = Seq("popup")
                )
                _single_offer_w_description(mad, producer = request.adnNode, args = args)
              } else {
                val args = blk.RenderArgs(withEdit = true, isStandalone = false, szMult = 1, inlineStyles = true)
                _single_offer(mad, args = args)
              }
              Ok(render)
            }
          }
        )

      case Left(formWithGlobalError) =>
        NotAcceptable("Form mode invalid")
    }
  }

}
