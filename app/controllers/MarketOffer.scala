package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.IsSuperuser
import models._
import views.html.market.lk.shop.offers._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.data._, Forms._
import io.suggest.ym.{OfferTypes, YmColors, YmlSax}, YmColors.YmColor
import util.FormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Управления офферами, т.к. коммерческими предложениями. Конкретные продавцы, владельцы магазинов
 * или кто-то ещё могут вносить коррективы в список представленных товаров.
 * Следует помнить, что предложения носят неточный характер и носят промо-характер.
 */

// TODO Когда будут юзеры, тут не должно быть никаких IsSuperuser

object MarketOffer extends SioController with MacroLogsImpl {
  import LOGGER._

  // Мапперы полей сборных форм.

  /** Маппер производителя товара/услуги. Используется для [[io.suggest.ym.OfferTypes.Simple]] и
    * [[io.suggest.ym.OfferTypes.VendorModel]]. */
  val vendorM = "vendor" -> nonEmptyText(maxLength = YmlSax.VENDOR_MAXLEN)

  /** Некая точная группа товаров/категория. Используется если дерева категорий недостаточно. */
  val typePrefixM = "typePrefix" -> text(maxLength = YmlSax.OFFER_TYPE_PREFIX_MAXLEN)

  /** Название комерческого предложения. Для [[io.suggest.ym.OfferTypes.VendorModel]] не доступно: там используется
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
    .transform(strTrimF, strIdentityF)   // TODO Юниты надо проверять по множеству допустимых.

  /** Маппер для цены товара. */
  val priceM = "price" -> float

  /** Маппер формы создания/редактирования промо-оффера в режиме vendor.model. */
  val vmPromoOfferFormM = Form(mapping(
    vendorM, modelM, colorsM, sizesM, sizeUnitsM, priceM
  )
  // apply()
  {(vendor, model, colors, sizes, sizeUnits, price) =>
    val offer = new MShopPromoOffer
    import offer.datum
    datum.vendor = vendor
    datum.model = model
    datum.colors = colors
    datum.sizesOrig = sizes
    datum.sizesUnitsOrig = sizeUnits
    datum.price = price
    offer
  }
  // unapply()
  {mOffer =>
    import mOffer.datum._
    Some((vendor getOrElse "",  model getOrElse "",  colors.toList,  sizesOrig,  sizesUnitsOrig getOrElse "",  price))
  })

  /** Показать список офферов указанного магазина. */
  def showShopPromoOffers(shopId: Int) = IsSuperuser.async { implicit request =>
    MShopPromoOffer.getAllForShop(shopId).map { offers =>
      Ok(listOffersTpl(shopId, offers))
    }
  }

  /** Рендер страницы с формой добавления оффера. */
  def addPromoOfferForm(shopId: Int) = IsSuperuser { implicit request =>
    Ok(form.addPromoOfferFormTpl(shopId, vmPromoOfferFormM))
  }

  /** Сабмит формы добавления оффера. Надо отправить оффер в хранилище. */
  def addPromoOfferFormSubmit(shopId: Int) = IsSuperuser.async { implicit request =>
    vmPromoOfferFormM.bindFromRequest().fold(
      {formWithErrors =>
        NotAcceptable(form.addPromoOfferFormTpl(shopId, formWithErrors))
      },
      {mpo =>
        mpo.datum.shopId = shopId
        mpo.datum.offerType = OfferTypes.VendorModel
        mpo.save.map { mpoSaved =>
          // TODO Нужен редирект на сохранённый товар.
          Ok("Saved")
        }
      }
    )
  }

}
