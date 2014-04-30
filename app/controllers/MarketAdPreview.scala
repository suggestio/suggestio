package controllers

import util.PlayMacroLogsImpl
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.img.ImgFormUtil._
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import util.img._
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play.current
import io.suggest.ym.parsers.Price
import io.suggest.model.MUserImgMetadata
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 */

object MarketAdPreview extends SioController with PlayMacroLogsImpl {
  import LOGGER._

  /** Объект, содержащий дефолтовые значения для preview-формы. Нужен для возможности простого импорта значений
    * в шаблон формы и для изоляции области видимости от другого кода. */
  object PreviewFormDefaults {
    /** Дефолтовый id картинки, когда она не задана. */
    val IMG_ID = "TODO_IMG_ID"   // TODO Нужен id для дефолтовой картинки.

    val TEXT_COLOR = "000000"
    val TEXT_FONT  = AOFieldFont(TEXT_COLOR)

    object Product {
      val PRICE_VALUE = Price(100F)
      val OLDPRICE_VALUE = Price(200F)
    }

    object Discount {
      val TPL_ID    = DISCOUNT_TPL_ID_MIN
      val DISCOUNT  = 50F
    }

    object Text {
      val TEXT = "Пример текста"
    }
  }


  private val prevCatIdKM = CAT_ID_K -> optional(userCatIdM)
  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM(blockM: Mapping[BlockData]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> default(
      mapping = imgIdJpegM,
      value = OrigImgIdKey(PreviewFormDefaults.IMG_ID)
    ),
    LOGO_IMG_ID_K -> optional(ImgFormUtil.logoImgIdM(imgIdM)),
    "ad" -> mapping(
      prevCatIdKM,
      OFFER_K -> blockM
    )(adFormApply)(adFormUnapply)
  ))

  private def detectAdPreviewForm(implicit request: Request[collection.Map[String, Seq[String]]]) = {
    getAdPreviewForm(request.body)
  }

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def getAdPreviewForm(reqBody: collection.Map[String, Seq[String]]): Either[AdFormM, (BlockConf, AdFormM)] = {
    // TODO adModes пора выпиливать. И этот Either заодно.
    val adModes = reqBody.get("ad.offer.mode") getOrElse Nil
    adModes.headOption.flatMap { adModeStr =>
      AdOfferTypes.maybeWithName(adModeStr)
    } map {
      case AdOfferTypes.BLOCK =>
        val blockId: Int = reqBody.get("ad.offer.blockId")
          .getOrElse(Nil)
          .headOption
          .map(_.toInt)
          .getOrElse(1)
        val blockConf: BlockConf = BlocksConf(blockId)
        blockConf -> getPreviewAdFormM(blockConf.strictMapping)
    } match {
      case Some(result) =>
        Right(result)

      case None =>
        warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
        val form = getPreviewAdFormM(MarketAd.dfltBlock.strictMapping)
          .withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
        Left(form)
    }
  }

  import views.html.market.showcase._single_offer

  def adFormPreviewSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
    import request.adnNode
    detectAdPreviewForm match {
      case Right((bc, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
            NotAcceptable("Preview form bind failed.")
          },
          {case (iik, logoOpt, mad) =>
            val fallbackLogoOptFut: Future[Option[MImgInfo]] = {
              MAdnNodeCache.maybeGetByIdCached(adnNode.adn.supId) map { parentAdnOpt =>
                parentAdnOpt.flatMap(_.logoImgOpt)
              }
            }
            val imgMetaFut = previewPrepareImgMeta(iik)
            mad.logoImgOpt = logoOpt
            mad.producerId = adnId
            for {
              imgMeta <- imgMetaFut
              fallbackLogoOpt <- fallbackLogoOptFut
            } yield {
              mad.imgOpt = Some(MImgInfo(iik.key, meta = imgMeta))
              Ok(_single_offer(mad, adnNode, fallbackLogo = fallbackLogoOpt ))
            }
          }
        )

      case Left(formWithGlobalError) =>
        NotAcceptable("Form mode invalid")
    }
  }

  /** Награбить метаданные по картинке для генерации превьюшки. */
  private def previewPrepareImgMeta(iik: ImgIdKey): Future[Option[MImgInfoMeta]] = {
    iik match {
      case tiik: TmpImgIdKey =>
        Future successful ImgFormUtil.getMetaForTmpImgCached(tiik)
      case oiik: OrigImgIdKey if oiik.meta.isDefined =>
        Future successful oiik.meta
      case oiik: OrigImgIdKey =>
        // Метаданных нет, но данные уже в базе. Надо бы прочитать метаданные из таблицы
        MUserImgMetadata.getById(oiik.key) map { muimOpt =>
          muimOpt.flatMap { muim =>
            muim.md.get("w").flatMap { widthStr =>
              muim.md.get("h").map { heightStr =>
                MImgInfoMeta(height = heightStr.toInt, width = widthStr.toInt)
              }
            }
          }
        } recover {
          case ex: Exception =>
            error(s"previewPrepareImgMeta($iik): Failed to fetch img metadata from hbase", ex)
            None
        }
    }
  }


  /** Экшен смены блока редактора. */
  def adBlockSwitchEditor(adnId: String) = IsAdnNodeAdmin(adnId).apply(parse.urlFormEncoded) { implicit request =>
    detectAdPreviewForm match {
      case Right((bc, adForm)) =>
        val formBinded = adForm.bindFromRequest().discardingErrors
        Ok(bc.renderEditor(formBinded))

      case Left(formWithErrors) =>
        Ok(views.html.blocks.editor._blockEditorTpl(formWithErrors))
    }
  }

}
