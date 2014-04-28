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
import util.HtmlSanitizer.adTextFmtPolicy
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
  private def getPreviewAdFormM[T <: AdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> default(
      mapping = imgIdJpegM,
      value = OrigImgIdKey(PreviewFormDefaults.IMG_ID)
    ),
    LOGO_IMG_ID_K -> optional(ImgFormUtil.logoImgIdM(imgIdM)),
    "ad" -> mapping(
      prevCatIdKM,
      panelColorKM,
      OFFER_K -> offerM,
      textAlignKM
    )(adFormApply[T])(adFormUnapply[T])
  ))

  // offer-mapping'и
  /** Толерантный к значениям маппинг для рекламной карточки продукта с ценой. */
  private def previewProductM(vendorDflt: String) = {
    mapping(
      "vendor"    -> default(
        mapping = mmaStringFieldM(
          text.transform(
            strTrimSanitizeF andThen { vendor =>
              if(vendor.isEmpty)
                vendorDflt
              else if (vendor.length > VENDOR_MAXLEN)
                vendor.substring(0, VENDOR_MAXLEN)
              else vendor
            },
            strIdentityF
          )
        ),
        value = AOStringField(vendorDflt, PreviewFormDefaults.TEXT_FONT)
      ),

      "price" -> mapping(
        "value" -> priceM,
        "color" -> fontColorM
      )
      {case ((rawPrice, priceOpt), font) =>
        val price = priceOpt getOrElse PreviewFormDefaults.Product.PRICE_VALUE
        AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font)
      }
      {mmadp =>
        import mmadp._
        Some((orig, Some(Price(value, currency))), font)
      },

      "oldPrice" -> mmaPriceOptM
    )
    { adProductMApply }
    { adProductMUnapply }
  }


  /** Кусок формы, ориентированный на оформление скидочной рекламы. */
  private val previewAdDiscountM = {
    val discountTextM = text(maxLength = 2 * DISCOUNT_TEXT_MAXLEN)
      .transform(
        strTrimSanitizeF andThen {s: String => if (s.length > DISCOUNT_TEXT_MAXLEN) s.substring(0, DISCOUNT_TEXT_MAXLEN) else s},
        strIdentityF
      )
    val tplM = mapping(
      "id"    -> default(
        mapping = number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
        value   = PreviewFormDefaults.Discount.TPL_ID
      ),
      "color" -> default(colorM, "ce2222")
    )
    { AODiscountTemplate.apply }
    { AODiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> {
        val discountTolerantM = percentM.transform[Float](
          {case (_, pcOpt) => pcOpt getOrElse PreviewFormDefaults.Discount.DISCOUNT },
          {pc => adhocPercentFmt(pc) -> Some(pc) }
        )
        mmaFloatFieldOptM(discountTolerantM).transform(
          {dcOpt => dcOpt getOrElse AOFloatField(PreviewFormDefaults.Discount.DISCOUNT, PreviewFormDefaults.TEXT_FONT) },
          {dc: AOFloatField => Some(dc) }
        )
      },
      "template"  -> tplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { AODiscount.apply }
    { AODiscount.unapply }
  }


  /** Форма для задания текстовой рекламы. */
  private val previewAdTextM = {
    val textM = default(
      mapping = text
        .transform({ adTextFmtPolicy.sanitize }, strIdentityF)
        .transform(
          { s => if (s.length > AD_TEXT_MAXLEN) s.substring(0, AD_TEXT_MAXLEN) else s},
          strIdentityF
        ),
      value = PreviewFormDefaults.Text.TEXT
    )
    mapping(
      "text" -> mmaStringFieldM(textM)
    )
    { AOText.apply }
    { AOText.unapply }
  }


  private def previewAdProductFormM(vendorDflt: String) = getPreviewAdFormM(previewProductM(vendorDflt))
  private val previewAdDiscountFormM = getPreviewAdFormM(previewAdDiscountM)
  private val previewAdTextFormM = getPreviewAdFormM(previewAdTextM)

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectAdPreviewForm(vendorDflt: String)(implicit request: Request[collection.Map[String, Seq[String]]]): Either[AdFormM, (AdOfferType, AdFormM)] = {
    val adModes = request.body.get("ad.offer.mode") getOrElse Nil
    adModes.headOption.flatMap { adModeStr =>
      AdOfferTypes.maybeWithName(adModeStr)
    } map { adMode =>
      val adForm = adMode match {
        case AdOfferTypes.RAW      =>
          val blockId: Int = request.body.get("ad.offer.blockId")
            .getOrElse(Nil)
            .headOption
            .map(_.toInt)
            .getOrElse(1)
          val offerM = BlocksConf(blockId).aoRawMapping
          getPreviewAdFormM(offerM)
        case AdOfferTypes.PRODUCT  => previewAdProductFormM(vendorDflt)
        case AdOfferTypes.DISCOUNT => previewAdDiscountFormM
        case AdOfferTypes.TEXT     => previewAdTextFormM
      }
      adMode -> adForm
    } match {
      case Some(result) =>
        Right(result)

      case None =>
        warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
        val form = MarketAd.shopAdProductFormM.withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
        Left(form)
    }
  }

  import views.html.market.showcase._single_offer

  def adFormPreviewSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
    import request.adnNode
    detectAdPreviewForm(adnNode.meta.name) match {
      case Right((offerType, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
            NotAcceptable("Preview form bind failed.")
          },
          {case (iik, logoOpt, mad) =>
            val fallbackLogoOptFut: Future[Option[MImgInfo]] = adnNode.adn.supId match {
              case Some(supId) =>
                MAdnNodeCache.getByIdCached(supId) map {
                  _.flatMap(_.logoImgOpt)
                }
              case None => Future successful None
            }
            val imgMetaFut = previewPrepareImgMeta(iik)
            mad.logoImgOpt = logoOpt
            mad.producerId = adnId
            for {
              imgMeta <- imgMetaFut
              fallbackLogoOpt <- fallbackLogoOptFut
            } yield {
              mad.img = MImgInfo(iik.key, meta = imgMeta)
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

}
