package controllers

import util.{Context, PlayMacroLogsImpl}
import views.html.market.lk.ad._
import models._, MShop.ShopId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.img.ImgFormUtil._
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import util.img.{ImgInfo, ImgFormUtil, OrigImgIdKey}
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  object FormModes extends Enumeration {
    type FormMode = Value
    val PRODUCT  = Value("p")
    val DISCOUNT = Value("d")

    /** Безопасная форма withName(). */
    def maybeWithName(n: String): Option[FormMode] = {
      try {
        Some(withName(n))
      } catch {
        case ex: Exception =>
          debug("Failed to parse form mode from string: " + n)
          None
      }
    }
  }
  import FormModes.FormMode

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
  // applyF()
  {(vendor, price, oldPriceOpt) =>
    MMartAdProduct(vendor=vendor, model = None, oldPrice=oldPriceOpt, price=price)
  }
  // unapplyF()
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
    "mode" -> nonEmptyText()
      .transform(
        FormModes.maybeWithName(_),
        {fmOpt: Option[FormMode] => fmOpt.map(_.toString) getOrElse "" }
      )
      .verifying("form.mode.unknown", _.isDefined)
      .transform(_.get, { fm: FormMode => Some(fm) }),
    "product"  -> optional(adProductM),
    "discount" -> optional(adDiscountM)
  )
  // Убедится, что задан ровно один из вариантов заполнения.
  .verifying("ad.bad.defined", { t => t match {
    case (FormModes.PRODUCT, pOpt @ Some(p), _)  => true
    case (FormModes.DISCOUNT, _, dOpt @ Some(d)) => true
    case _ => false
  }})
  // Выбрать только один из результатов.
  .transform[MMartAdOfferT](
    { case (m @ FormModes.PRODUCT, pOpt @ Some(p), _)  => p
      case (m @ FormModes.DISCOUNT, _, dOpt @ Some(d)) => d
      case other => throw new IllegalArgumentException("Unexpected input: " + other) },   // Should never occur
    { case p: MMartAdProduct  => (FormModes.PRODUCT, Some(p), None)
      case d: MMartAdDiscount => (FormModes.DISCOUNT, None, Some(d)) }
  )

  /** Форма добавления/редактирования рекламируемого продукта. */
  val adFormM = Form(mapping(
    "catId" -> userCatIdM,
    "image_key"  -> imgIdM,
    "panelColor" -> colorM
      .transform(
        { MMartAdPanelSettings.apply },
        { mmaps: MMartAdPanelSettings => mmaps.color }
      ),
    "ad"    -> adBodyM,
    "textAlign" -> textAlignsM
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

  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param shopId id магазина.
    */
  def createShopAdSubmit(shopId: ShopId_t) = IsShopAdmFull(shopId).async { implicit request =>
    val catOwnerId = request.mshop.martId getOrElse shopId
    val formBinded = adFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"createShopAdSubmit($shopId): Bind failed: ${formWithErrors.errors}")
        renderCreateFormWith(formWithErrors, catOwnerId, request.mshop) map {
          Ok(_)
        }
      },
      {case (imgKey, mmad) =>
        val needImgs = Seq(ImgInfo(imgKey, cropOpt = None))
        val imgFut = ImgFormUtil.updateOrigImg(needImgs, oldImgs = Nil)
        imgFut.flatMap {
          case List(imgIdSaved) =>
            // TODO Нужно проверить категорию.
            mmad.shopId = Some(shopId)
            mmad.companyId = request.mshop.companyId
            mmad.martId = request.mshop.martId.get
            mmad.picture = imgIdSaved
            // Сохранить изменения в базу
            mmad.save.map { adId =>
              Redirect(routes.MarketShopLk.showShop(shopId))
                .flashing("success" -> "Рекламная карточка создана.")
            }

          case Nil =>
            debug("Failed to handle img key: " + imgKey)
            val formWithError = formBinded.withGlobalError("error.image.save")
            renderCreateFormWith(formWithError, catOwnerId, request.mshop) map { render =>
              NotAcceptable(render)
            }
        }
      }
    )
  }

  /** Общий код рендера createAdTpl с запросом необходимых категорий. */
  private def renderCreateFormWith(af: Form[_], catOwnerId: String, mshop: MShop)(implicit ctx: Context) = {
    MMartCategory.findTopForOwner(catOwnerId) map { mmcats1 =>
      createAdTpl(mshop, mmcats1, af)
    }
  }

  private def renderEditFormWith(af: Form[_], catOwnerId:String, mshopFut: Future[Option[MShop]], mad: MMartAd)(implicit ctx: Context) = {
    for {
      mmcats1  <- MMartCategory.findTopForOwner(catOwnerId)
      mshopOpt <- mshopFut
    } yield {
      mshopOpt match {
        case Some(mshop) => Some(editAdTpl(mshop, mad, mmcats1, af))
        case None => None
      }
    }
  }

  private def renderFailedEditFormWith(af: Form[_], mad: MMartAd)(implicit ctx: Context) = {
    val shopId = mad.shopId.get
    // TODO Надо фетчить магазин и категории одновременно.
    val mshopFut = MShop.getById(shopId)
    renderEditFormWith(af, mad.martId, mshopFut, mad) map {
      case Some(render) => NotAcceptable(render)
      case None => shopNotFound(shopId)
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editShopAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    mad.shopId match {
      case Some(_shopId) =>
        val imgIdKey = OrigImgIdKey(mad.picture)
        val formFilled = adFormM fill (imgIdKey, mad)
        val mshopFut = MShop.getById(_shopId)
        renderEditFormWith(formFilled, mad.martId, mshopFut, mad) map {
          case Some(render) => Ok(render)
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
    val formBinded = adFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"editShopAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
        renderFailedEditFormWith(formWithErrors, mad)
      },
      {case (iik, mad2) =>
        // TODO Проверить категорию.
        // TODO И наверное надо проверить shopId-существование в исходной рекламе.
        val needImgs = Seq(ImgInfo(iik, cropOpt = None))
        ImgFormUtil.updateOrigImgIds(needImgs, oldImgIds = Seq(mad.picture)) flatMap {
          case List(savedImgId) =>
            mad2.id = mad.id
            mad2.martId = mad.martId
            mad2.shopId = mad.shopId
            mad2.companyId = mad.companyId
            mad2.picture = savedImgId
            mad2.save.map { _ =>
              Redirect(routes.MarketShopLk.showShop(mad.shopId.get))
                .flashing("success" -> "Изменения сохранены")
            }

          // Не удалось обработать картинку. Вернуть форму назад
          case Nil =>
            debug(s"editShopAdSubmit($adId): Failed to update iik = " + iik)
            val formWithError = formBinded.withGlobalError("error.image.save")
            renderFailedEditFormWith(formWithError, mad)
        }
      }
    )
  }

  /**
   * POST для удаления рекламной карточки.
   * @param adId id рекламы.
   * @return Редирект в магазин или ТЦ.
   */
  def deleteSubmit(adId: String) = IsAdEditor(adId).async { implicit request =>
    MMartAd.deleteById(adId) map { _ =>
      val route = request.mad.shopId match {
        // Невсегда ясно, куда редиректить. Поэтому угадываем истинного владельца рекламы (магазин или ТЦ).
        case Some(shopId) => routes.MarketShopLk.showShop(shopId)
        case None         => routes.MarketMartLk.martShow(request.mad.martId)
      }
      Redirect(route)
        .flashing("success" -> "Рекламная карточка удалена")
    }
  }

  private def shopNotFound(shopId: ShopId_t) = NotFound("shop not found: " + shopId)
  private def adEditWrong = Forbidden("Nobody cat edit this ad using this action.")
}
