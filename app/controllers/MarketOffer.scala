package controllers

import io.suggest.util.MacroLogsImpl
import util.acl._
import models._
import views.html.market.lk.shop.offers._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.data._, Forms._
import io.suggest.ym.{OfferTypes, YmColors, YmlSax}, YmColors.YmColor
import util.FormUtil._
import util.img._
import ImgFormUtil._
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Управления офферами, т.к. коммерческими предложениями. Конкретные продавцы, владельцы магазинов
 * или кто-то ещё могут вносить коррективы в список представленных товаров.
 * Следует помнить, что предложения носят неточный характер и носят промо-характер.
 */

object MarketOffer extends SioController with MacroLogsImpl {
  import LOGGER._

  // Мапперы полей сборных форм.

  /** Маппер производителя товара/услуги. Используется для io.suggest.ym.OfferTypes.Simple
    * io.suggest.ym.OfferTypes.VendorModel. */
  val vendorM = "vendor" -> nonEmptyText(maxLength = YmlSax.VENDOR_MAXLEN)

  /** Некая точная группа товаров/категория. Используется если дерева категорий недостаточно. */
  val typePrefixM = "typePrefix" -> text(maxLength = YmlSax.OFFER_TYPE_PREFIX_MAXLEN)

  /** Название комерческого предложения. Для io.suggest.ym.OfferTypes.VendorModel не доступно: там используется
    * связка typePrefix + vendor + model. */
  val nameM   = "name"   -> nonEmptyText(maxLength = YmlSax.OFFER_NAME_MAXLEN)

  /** vendor.model: Конкретная модель конкретного товара. Для неточных офферов тут серия или пострипанное имя модели,
    * т.к. конкретная модель товара нередко содержит и размер, и цвет, и все остальные хар-ки. */
  val modelM  = "model"  -> nonEmptyText(maxLength = YmlSax.OFFER_MODEL_MAXLEN)

  /** Маппер списка доступных цветов (чекбоксы) разбирается и причёсывается тут. */
  val colorsM = "colors" -> list(text(maxLength = 16))
    .transform(
      { _.flatMap { colorRaw => YmColors.maybeWithName(colorRaw).toList } },
      { l: List[YmColor] => l.map(_.toString) }
    )

  /** Базовый статический маппер размеров товара. Сейчас размеры передаются через запятую. */
  val sizesM = "sizes" -> text(maxLength = 64)
    .transform(
      _.split("\\s*,\\s*").toSeq.distinct.sorted,
      {l: Seq[String] => l.mkString(", ")}
    )

  /** Ввод единиц измерения. */
  val sizeUnitsM = "sizeUnits" -> text(maxLength = 4)
    .transform(strTrimSanitizeF, strIdentityF)   // TODO Юниты надо проверять по множеству допустимых.

  /** Маппер для цены товара. */
  val priceM = "price" -> floatM

  /** Маппер "старых" цен. Старых цен может быть 0, 1 или более. Для упрощения пока только одна цена максимум. */
  val oldPriceM = "oldPrice" -> optional(floatM)
    .transform(_.toList, {l: List[Float] => l.headOption})

  /** Маппер формы создания/редактирования промо-оффера в режиме vendor.model. */
  val vmPromoOfferFormM = Form(mapping(
    vendorM, modelM, colorsM, sizesM, sizeUnitsM, priceM, oldPriceM,
    // TODO Изначально была поддержка list(tmpImgIdM), но эта форма стала тестовым полигоном для картинок сразу после 6b6694f83444, поэтому пока работа идёт с одной картинкой.
    "image_key"  -> imgIdM
  )
  // apply()
  {(vendor, model, colors, sizes, sizeUnits, price, oldPriceOpt, iik) =>
    val offer = new MShopPromoOffer
    import offer.datum
    datum.vendor = vendor
    datum.model = model
    datum.colors = colors
    datum.sizesOrig = sizes
    datum.sizeUnitsOrig = sizeUnits
    datum.price = price
    datum.oldPrices = oldPriceOpt
    // Подхватываем тут tempImg*. Сначала мержим id картинок и их кропы
    val imgInfo = ImgInfo4Save(iik)
    (offer, imgInfo)
  }
  // unapply()
  {case (mOffer, imgInfo)=>
    import mOffer.datum._
    val vendor1 = vendor getOrElse ""
    val model1 = model getOrElse ""
    val szUnits = sizeUnitsOrig getOrElse ""
    Some((vendor1, model1,  colors.toList,  sizesOrig,  szUnits,  price,  oldPrices, imgInfo.iik))
  })



  /** Показать список офферов указанного магазина для владельца магазина. (или владельца ТЦ?)
    * TODO Нужно разобраться с этой фунцией и тут пофиксить ACL: смотреть офферы в ЛК могут как админы ТЦ, так и владелец магазина.
    * TODO Эту функцию надо перепилить для владельца ТЦ: они могут видеть лишь опубликованные офферы. */
  def showShopPromoOffers(shopId: String) = IsShopAdm(shopId).async { implicit request =>
    MShopPromoOffer.getAllForShop(shopId) map { offers =>
      Ok(listOffersTpl(request.mshop, offers))
    }
  }

  /** Рендер страницы с формой добавления оффера. */
  def addPromoOfferForm(shopId: String) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    Ok(form.addPromoOfferFormTpl(shopId, vmPromoOfferFormM, mshop))
  }

  /** Сабмит формы добавления оффера. Надо отправить оффер в хранилище. */
  def addPromoOfferFormSubmit(shopId: String) = IsShopAdm(shopId).async { implicit request =>
    vmPromoOfferFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"addPromoOfferFormSubmit($shopId): " + formWithErrors.errors)
        NotAcceptable(form.addPromoOfferFormTpl(shopId, formWithErrors, request.mshop))
      },
      {case (mpo, imgInfo) =>
        mpo.datum.shopId = shopId
        mpo.datum.offerType = OfferTypes.VendorModel
        // Картинки: нужно их перегнать в постоянное хранилище.
        ImgFormUtil.updateOrigImgFull(Some(imgInfo), oldImgs = None) flatMap { imgsIdsSaved =>
          // Выставить сохраненные картинки в датум и сохранить его.
          mpo.datum.pictures = imgsIdsSaved.map(_.filename)
          mpo.save.map { mpoSavedId =>
            // Редирект на созданный промо-оффер.
            rdrToOffer(mpoSavedId)
              .flashing("success" -> "Created ok.")
          }
        }
      }
    )
  }

  /** Отобразить страницу с указанным оффером. */
  def showPromoOffer(offerId: String) = IsPromoOfferAdminFull(offerId).async { implicit request =>
    MAdnNode.getByIdType(request.shopId, AdNetMemberTypes.SHOP) map {
      case Some(mshop) =>
        Ok(showPromoOfferTpl(request.offer, mshop))
      case None => shopNotFound(request.shopId)
    }
  }

  /** Юзер нажал кнопку удаления оффера. */
  def deletePromoOfferSubmit(offerId: String) = IsPromoOfferAdmin(offerId).async { implicit request =>
    MShopPromoOffer.deleteById(offerId) map { isDeleted =>
      val flash = if (isDeleted) {
        "success" -> "Deleted ok."
      } else {
        "error"   -> s"Unexpectedly not found: offer $offerId"
      }
      Redirect(routes.MarketOffer.showShopPromoOffers(request.shopId))
        .flashing(flash)
    }
  }

  /** Рендер страницы редактирования промо-оффера. */
  def editPromoOfferForm(offerId: String) = IsPromoOfferAdminFull(offerId).async { implicit request =>
    MAdnNode.getByIdType(request.shopId, AdNetMemberTypes.SHOP) map {
      case Some(mshop) =>
        import request.offer
        val oiik = OrigImgIdKey(offer.datum.pictures.head)
        val imgInfo = ImgInfo4Save(oiik)
        val f = vmPromoOfferFormM fill (offer -> imgInfo)
        Ok(form.editPromoOfferTpl(offer, f, mshop))

      case None => shopNotFound(request.shopId)
    }
  }

  /** Самбит формы редактирования оффера. */
  def editPromoOfferFormSubmit(offerId: String) = IsPromoOfferAdminFull(offerId).async { implicit request =>
    import request.offer
    vmPromoOfferFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editPromoOfferFormSubmit($offerId): Failed to bind form: " + formatFormErrors(formWithErrors))
        MAdnNode.getByIdType(request.shopId, AdNetMemberTypes.SHOP) map {
          case Some(mshop) => NotAcceptable(form.editPromoOfferTpl(offer, formWithErrors, mshop))
          case None        => shopNotFound(request.shopId)
        }
      },
      {case (mpo, imgInfo) =>
        ImgFormUtil.updateOrigImgId(Some(imgInfo), offer.datum.pictures.headOption) flatMap { updatedImgIds =>
          mpo.datum.offerType = offer.datum.offerType
          mpo.datum.shopId = offer.datum.shopId
          mpo.id = offer.id
          mpo.datum.pictures = updatedImgIds.map(_.filename)
          mpo.save.map { _ =>
            rdrToOffer(offerId).flashing("success" -> "Saved ok.")
          }
        }
      }
    )
  }

  private def shopNotFound(shopId: String) = NotFound("Shop not found: " + shopId)
  private def rdrToOffer(offerId: String) = Redirect(routes.MarketOffer.showPromoOffer(offerId))

}
