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
import util.img.{ImgIdKey, ImgInfo, ImgFormUtil, OrigImgIdKey}
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play.current

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

    def maybeFormWithName(n: String): Option[(FormMode, AdFormM)] = {
      maybeWithName(n).map { m =>
        val form = getForm(m)
        m -> form
      }
    }

    val getForm: PartialFunction[FormMode, AdFormM] = {
      case PRODUCT  => adProductFormM
      case DISCOUNT => adDiscountFormM
    }

    val getForClass: PartialFunction[MMartAdOfferT, FormMode] = {
      case _: MMartAdProduct  => PRODUCT
      case _: MMartAdDiscount => DISCOUNT
    }

    def getFormForClass(c: MMartAdOfferT): AdFormM = {
      getForm(getForClass(c))
    }
  }

  import FormModes.FormMode

  // Есть шаблоны для шаблона скидки. Они различаются по id. Тут min и max для допустимых id.
  val DISCOUNT_TPL_ID_MIN = current.configuration.getInt("ad.discount.tpl.id.min") getOrElse 1
  val DISCOUNT_TPL_ID_MAX = current.configuration.getInt("ad.discount.tpl.id.max") getOrElse 6

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
    val discountTplM = mapping(
      "id"    -> number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
      "color" -> colorM
    )
    { DiscountTemplate.apply }
    { DiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> mmaFloatFieldM(discountValueM),
      "template"  -> discountTplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { MMartAdDiscount.apply }
    { MMartAdDiscount.unapply }
  }

  // Дублирующиеся куски маппина выносим за пределы метода.
  private val catIdKM = "catId" -> userCatIdM
  private val adImgIdKM = "image_key"  -> imgIdM
  private val panelColorKM = "panelColor" -> colorM
    .transform(
      { MMartAdPanelSettings.apply },
      { mmaps: MMartAdPanelSettings => mmaps.color }
    )
  private val textAlignKM = "textAlign" -> textAlignsM

  type AdFormM = Form[(ImgIdKey, MMartAd)]

  /** Генератор форм добавления/редактирования рекламируемого продукта в зависимости от вкладок. */
  private def getAdFormM[T <: MMartAdOfferT](adBodyM: Mapping[T]): AdFormM = Form(mapping(
    catIdKM,
    adImgIdKM,
    panelColorKM,
    "ad" -> adBodyM,
    textAlignKM
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
    if (panel.isDefined && userCatId.isDefined && !offers.isEmpty) {
      val adBody = offers.head.asInstanceOf[T]  // TODO Надо что-то решать с подтипами офферов. Параметризация типов MMartAd - геморрой.
      Some((userCatId.get, imgKey, panel.get, adBody, textAligns))
    } else {
      warn("Unexpected ad object received into ad-product form: " + mmad)
      None
    }
  })
  
  val adProductFormM  = getAdFormM(adProductM)
  val adDiscountFormM = getAdFormM(adDiscountM)

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.mode не валиден, то будет Left с формой с global error. */
  private def detectAdForm(implicit request: Request[collection.Map[String, Seq[String]]]): Either[AdFormM, (FormMode, AdFormM)] = {
    val adModes = request.body.get("ad.mode") getOrElse Nil
    adModes.headOption.flatMap { adMode =>
      FormModes.maybeFormWithName(adMode)
    } match {
      case Some(result) =>
        Right(result)

      case None =>
        warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
        val form = adProductFormM.withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
        Left(form)
    }
  }

  /**
   * Страница, занимающаяся создание рекламной карточки.
   * @param shopId id магазина.
   */
  def createShopAd(shopId: String) = IsShopAdm(shopId).async { implicit request =>
    MMartCategory.findTopForOwner(shopId) map { mmcats1 =>
      Ok(createAdTpl(request.mshop, mmcats1, adProductFormM))
    }
  }

  /** Рендер ошибки в create-форме. Довольно общий, но асинхронный код. */
  private def createShopAdFormError(formWithErrors: AdFormM, catOwnerId: ShopId_t, mshop: MShop)(implicit ctx: util.Context) = {
    renderCreateFormWith(formWithErrors, catOwnerId, mshop) map {
      NotAcceptable(_)
    }
  }

  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param shopId id магазина.
    */
  def createShopAdSubmit(shopId: ShopId_t) = IsShopAdm(shopId).async(parse.urlFormEncoded) { implicit request =>
    import request.mshop
    val catOwnerId = request.mshop.martId getOrElse shopId
    lazy val logPrefix = s"createShopAdSubmit($shopId): "
    detectAdForm match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" +
              formWithErrors.errors.map { e => "  " + e.key + " -> " + e.message }.mkString("\n"))
            createShopAdFormError(formWithErrors, catOwnerId, mshop)
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
                debug(logPrefix + "Failed to handle img key: " + imgKey)
                val formWithError = formBinded.withGlobalError("error.image.save")
                renderCreateFormWith(formWithError, catOwnerId, request.mshop) map { render =>
                  NotAcceptable(render)
                }
            }
          }
        )

      // Не ясно, как именно надо биндить тело реквеста на маппинг формы.
      case Left(formWithGlobalError) =>
        warn(logPrefix + "AD mode is undefined or invalid. Returning form back.")
        val formWithErrors = formWithGlobalError.bindFromRequest()
        createShopAdFormError(formWithErrors, catOwnerId, mshop)
    }
  }

  /** Общий код рендера createAdTpl с запросом необходимых категорий. */
  private def renderCreateFormWith(af: AdFormM, catOwnerId: String, mshop: MShop)(implicit ctx: Context) = {
    MMartCategory.findTopForOwner(catOwnerId) map { mmcats1 =>
      createAdTpl(mshop, mmcats1, af)
    }
  }

  private def renderEditFormWith(af: AdFormM, catOwnerId:String, mshopFut: Future[Option[MShop]], mad: MMartAd)(implicit ctx: Context) = {
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

  private def renderFailedEditFormWith(af: AdFormM, mad: MMartAd)(implicit ctx: Context) = {
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
        val formFilled = FormModes.getFormForClass(mad.offers.head) fill (imgIdKey, mad)
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
  def editShopAdSubmit(adId: String) = IsAdEditor(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    detectAdForm match {
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
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

      case Left(formWithGlobalError) =>
        val formWithErrors = formWithGlobalError.bindFromRequest()
        renderFailedEditFormWith(formWithErrors, mad)
    }

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
