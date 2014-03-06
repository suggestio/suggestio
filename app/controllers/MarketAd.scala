package controllers

import util.PlayMacroLogsImpl
import views.html.market.lk.ad._
import models._, MShop.ShopId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.img.ImgFormUtil._
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import util.img.OrigImgIdKey

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппер для поля, содержащего код цвета. */
  // TODO Нужно добавить верификацию тут какую-то. Например через YmColors.
  val colorM = nonEmptyText(maxLength = 16)

  /** Маппинг для задания цены. */
  val priceM = float
    .verifying("price.mustbe.nonneg", { _ >= 0F })
    .verifying("price.too.much", { _ < 100000000F })

  /** Маппим строковое поле с настройками шрифта. */
  private def mmaStringFieldM(m : Mapping[String]) = mapping(
    "value" -> m,
    "color" -> colorM
  )
  {(name, color) =>
    val font = MMAdFieldFont(color)
    MMAdStringField(name, font)
  }
  {mmasf => Some((mmasf.value, mmasf.font.color)) }
  
  /** Маппим числовое (Float) поле. */
  private def mmaFloatFieldM(m: Mapping[Float]) = mapping(
    "value" -> m,
    "color" -> colorM
  )
  {(value, color) =>
    val font = MMAdFieldFont(color)
    MMAdFloatField(value, font)
  }
  {mmaff => Some((mmaff.value, mmaff.font.color)) }

  val mmaFloatPriceM = mmaFloatFieldM(priceM)


  /** Какие-то данные для text-align'a. */
  val textAlignRawM = nonEmptyText(maxLength = 16) // TODO Нужен валидатор.
  private def textAlignM(id: String) = {
    id -> textAlignRawM
      .transform(
        {ta: String => MMartAdTextAlign(id=id, align=ta) },
        {mmata: MMartAdTextAlign => mmata.align }
      )
  }

  /** Расположение текста. Пока в виде строки. */
  val textAlignsM = mapping(
    textAlignM("phone"),
    textAlignM("tablet")
  )
  {(phoneTA, tabletTA) => List(phoneTA, tabletTA) }
  { case List(phoneTA, tabletTA) => Some((phoneTA, tabletTA)) }


  // Общие для ad-форм мапперы закончились. Пора запилить сами формы и формоспецифичные элементы.
  val adProductM = mapping(
    "vendor"    -> mmaStringFieldM(nonEmptyText(maxLength = 32)),
    "price"     -> mmaFloatPriceM,
    "oldPrice"  -> optional(mmaFloatPriceM)
  )
  {(vendor, price, oldPriceOpt) =>
    MMartAdProduct(vendor=vendor, model = None, oldPrice=oldPriceOpt, price=price)
  }
  {product =>
    import product._
    Some((vendor, price, oldPrice))
  }

  /** Кусок формы, ориентированный на оформление скидочной рекламы. */
  val adDiscountM = {
    val discountTextM = nonEmptyText(maxLength = 64)
      .transform(strTrimBrOnlyF, strIdentityF)
    val discountValueM = float
      .verifying("discount.too.low", { _ <= 0F })
      .verifying("discount.too.big", { _ >= 200F })
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> mmaFloatFieldM(discountValueM),
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    {(text1, discount, text2) =>
      MMartAdDiscount(text1=text1, discount=discount, text2=text2)
    }
    {mmadd =>
      import mmadd._
      Some((text1, discount, text2))
    }
  }

  /** Маппинг-маршрутизатор, который отвечает за выборку того, какую подформу надо выбирать из засабмиченного. */
  val adBodyM = tuple(
    "product"  -> optional(adProductM),
    "discount" -> optional(adDiscountM)
  )
  // Убедится, что задан ровно один из вариантов заполнения.
  .verifying("ad.undefined.or.bothdefined", { t => t match {
    case (pOpt, dOpt) => pOpt.isDefined ^ dOpt.isDefined
  }})
  // Выбрать из двух вариантов единственный заданный
  .transform[MMartAdOfferT](
    { case (pOpt, dOpt)       => (pOpt orElse dOpt).get },
    { case p: MMartAdProduct  => (Some(p), None)
      case d: MMartAdDiscount => (None, Some(d)) }
  )

  /** Форма добавления/редактирования рекламируемого продукта. */
  val adFormM = Form(mapping(
    "catId" -> userCatIdM,
    "imgId" -> imgIdM,
    "panel" -> colorM.transform(
      { MMartAdPanelSettings.apply },
      {mmaps: MMartAdPanelSettings => mmaps.color }
    ),
    "ad"    -> adBodyM,
    "textAligns" -> textAlignsM
  )
  // applyF()
  {(userCatId, imgKey, panelSettings, adBody, textAligns) =>
    val mmad = MMartAd(
      martId      = null,
      offers      = List(adBody),
      picture     = null,
      shopId      = null,
      panel       = Some(panelSettings),
      userCatId   = Some(userCatId),
      textAligns  = textAligns,
      companyId   = null
    )
    (imgKey, mmad)
  }
  // unapplyF()
  {case (imgKey, mmad) =>
    import mmad._
    if (panel.isDefined && userCatId.isDefined && !offers.isEmpty && offers.head.isInstanceOf[MMartAdProduct]) {
      val adBody = offers.head
      Some((userCatId.get, imgKey, panel.get, adBody, textAligns))
    } else {
      warn("Unexpected ad object received into ad-product form: " + mmad)
      None
    }
  })


  /**
   * Страница, занимающаяся создание рекламной карточки.
   * @param shopId id магазина.
   */
  def createShopAd(shopId: String) = IsShopAdmFull(shopId).async { implicit request =>
    MMartCategory.findTopForOwner(shopId) map { mmcats1 =>
      Ok(createAdTpl(request.mshop, mmcats1, adFormM))
    }
  }

  /** Сабмит формы добавления рекламной карточки товара/скидки. */
  def createShopAdSubmit(shopId: ShopId_t) = IsShopAdmFull(shopId).async { implicit request =>
    adFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createShopAdSubmit($shopId): Bind failed: ${formWithErrors.errors}")
        MMartCategory.findTopForOwner(shopId) map { mmcats1 =>
          NotAcceptable(createAdTpl(request.mshop, mmcats1, formWithErrors))
        }
      },
      {case (imgKey, mmad) =>
        // TODO Нужно проверить картинку. Нужно сохранить картинку.
        // TODO Нужно проверить категорию.
        mmad.shopId = Some(shopId)
        mmad.companyId = request.mshop.companyId
        mmad.martId = request.mshop.martId.get
        // Сохранить изменения в базу
        mmad.save.map { adId =>
          Redirect(routes.MarketShopLk.showShop(shopId))
            .flashing("success" -> "Рекламная карточка создана.")
        }
      }
    )
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editShopAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    mad.shopId match {
      case Some(_shopId) =>
        MShop.getById(_shopId) flatMap {
          case Some(mshop) =>
            MMartCategory.findTopForOwner(_shopId) map { mmcats1 =>
              val imgIdKey = OrigImgIdKey(mad.picture)
              val formFilled = adFormM fill (imgIdKey, mad)
              Ok(editAdTpl(mshop, mad, mmcats1, formFilled))
            }

          // TODO: Отсутсвие магазина не должно быть помехой
          case None => shopNotFound(_shopId)
        }

      // Магазин в карточке не указан. Вероятно, это карточка должна редактироваться через какой-то другой экшен
      case None => adEditWrong
    }
  }

  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editShopAdSubmit(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    adFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editShopAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
        mad.shopId match {
          case Some(_shopId) =>
            MShop.getById(_shopId) flatMap {
              case Some(mshop) =>
                MMartCategory.findTopForOwner(_shopId) map { mmcats1 =>
                  NotAcceptable(editAdTpl(mshop, mad, mmcats1, formWithErrors))
                }
              case None        => shopNotFound(_shopId)
            }

          // Магазин в карточке не указан. Вероятно, это карточка должна редактироваться через какой-то другой экшен
          case None => adEditWrong
        }
      },
      {case (iik, mad2) =>
        // TODO Нужно проверить картинку. И если прислана Orig-картинка, то нужно проверить её по исходному mad, чтобы не было подмены.
        // TODO Проверить категорию.
        // TODO И наверное надо проверить shopId-существование в исходной рекламе.
        mad2.martId = mad.martId
        mad2.shopId = mad.shopId
        mad2.companyId = mad.companyId
        mad2.save.map { _ =>
          Redirect(routes.MarketShopLk.showShop(mad.shopId.get))
            .flashing("success" -> "Изменения сохранены")
        }
      }
    )
  }

  private def shopNotFound(shopId: ShopId_t) = NotFound("shop not found: " + shopId)
  private def adEditWrong = Forbidden("Nobody cat edit this ad using this action.")
}
