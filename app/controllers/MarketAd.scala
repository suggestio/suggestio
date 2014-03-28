package controllers

import util.{UserInputParsers, Context, PlayMacroLogsImpl}
import views.html.market.lk.ad._
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
import TextAlignValues.TextAlignValue
import MMartCategory.CollectMMCatsAcc_t
import scala.util.{Try, Failure, Success}
import util.HtmlSanitizer.adTextFmtPolicy
import io.suggest.ym.parsers.Price

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  type AdFormM = Form[(ImgIdKey, LogoOpt_t, MMartAd)]

  type FormDetected_t = Option[(MMartAdOfferType, AdFormM)]

  /** Режимы работы формы добавления рекламной карточки. Режимы перенесены в MMartAdOfferTypes. */
  object FormModes {
    import MMartAdOfferTypes._

    def maybeShopFormWithName(n: String): FormDetected_t = {
      maybeWithName(n).map { m =>
        val form = getShopForm(m)
        m -> form
      }
    }

    def maybeMartFormWithName(n: String): FormDetected_t = {
      maybeWithName(n).map { m =>
        val form = getMartForm(m)
        m -> form
      }
    }

    val getShopForm: PartialFunction[MMartAdOfferType, AdFormM] = {
      case PRODUCT  => shopAdProductFormM
      case DISCOUNT => shopAdDiscountFormM
      case TEXT     => shopAdTextFormM
    }

    val getMartForm: PartialFunction[MMartAdOfferType, AdFormM] = {
      case PRODUCT  => martAdProductFormM
      case DISCOUNT => martAdDiscountFormM
      case TEXT     => martAdTextFormM
    }

    val getForClass: PartialFunction[MMartAdOfferT, MMartAdOfferType] = {
      case _: MMartAdProduct  => PRODUCT
      case _: MMartAdDiscount => DISCOUNT
      case _: MMartAdText     => MMartAdOfferTypes.TEXT
    }

    def getMartFormForClass(c: MMartAdOfferT): AdFormM = getMartForm(getForClass(c))
    def getShopFormForClass(c: MMartAdOfferT): AdFormM = getShopForm(getForClass(c))
  }

  // Есть шаблоны для шаблона скидки. Они различаются по id. Тут min и max для допустимых id.
  val DISCOUNT_TPL_ID_MIN = current.configuration.getInt("ad.discount.tpl.id.min") getOrElse 1
  val DISCOUNT_TPL_ID_MAX = current.configuration.getInt("ad.discount.tpl.id.max") getOrElse 6

  /** Шрифт пока что характеризуется только цветом. Поэтому маппим поле цвета на шрифт. */
  private val fontColorM = colorM
    .transform(
      { MMAdFieldFont.apply },
      { mmAdFont: MMAdFieldFont => mmAdFont.color }
    )

  /** Маппим строковое поле с настройками шрифта. */
  private def mmaStringFieldM(m : Mapping[String]) = mapping(
    "value" -> m,
    "color" -> fontColorM
  )
  { MMAdStringField.apply }
  { MMAdStringField.unapply }

  /** Маппим числовое (Float) поле. */
  private def mmaFloatFieldM(m: Mapping[Float]) = mapping(
    "value" -> m,
    "color" -> fontColorM
  )
  { MMAdFloatField.apply }
  { MMAdFloatField.unapply }

  /** Поле с ценой. Является вариацией float-поля. */
  private val mmaPriceM = mapping(
    "value" -> priceStrictM,
    "color" -> fontColorM
  )
  {case ((rawPrice, price), font) =>
    MMAdPrice(price.price, price.currency.getCurrencyCode, rawPrice, font) }
  {mmadp =>
    import mmadp._
    Some((orig, Price(value, currency)), font)
  }

  /** Поле с необязательной ценой. Является вариацией float-поля. Жуткий говнокод. */
  private val mmaPriceOptM = mapping(
    "value" -> optional(priceStrictM),
    "color" -> fontColorM
  )
  {(pricePairOpt, font) =>
    pricePairOpt.map { case (rawPrice, price) =>
      MMAdPrice(price.price, price.currency.getCurrencyCode, rawPrice, font)
    }
  }
  {_.map { mmadp =>
    import mmadp._
    (Some(orig -> Price(value, currency)), font)
  }}


  /** Маппим необязательное Float-поле. */
  private def mmaFloatFieldOptM(m: Mapping[Float]) = mapping(
    "value" -> optional(m),
    "color" -> fontColorM
  )
  {(valueOpt, color) =>
    valueOpt map { MMAdFloatField(_, color) }
  }
  {_.map { mmaff =>
    (Option(mmaff.value), mmaff.font)
  }}


  // Мапперы для textAlign'ов
  /** Какие-то данные для text-align'a. */
  val textAlignRawM = nonEmptyText(maxLength = 16)
    .transform(strTrimSanitizeLowerF, strIdentityF)
    .transform(
      { TextAlignValues.maybeWithName },
      { tavOpt: Option[TextAlignValue] => tavOpt.map(_.toString) getOrElse "" }
    )
    .verifying("text.align.value.invalid", { _.isDefined })
    // Переводим результаты обратно в строки для более надежной работы reflections в TA-моделях.
    .transform(
      _.get.toString,
      //{ tavStr: String => TextAlignValues.maybeWithName(tavStr) }
      { TextAlignValues.maybeWithName }
    )

  /** Маппинг для textAlign.phone -- параметры размещения текста на экране телефона. */
  val taPhoneM = textAlignRawM
    .transform(
      { MMartAdTAPhone.apply },
      { taPhone: MMartAdTAPhone => taPhone.align }
    )

  /** Маппинг для textAlign.tablet -- параметров размещения текста на планшете. */
  val taTabletM = mapping(
    "top"    -> textAlignRawM,
    "bottom" -> textAlignRawM
  )
  { MMartAdTATablet.apply }
  { MMartAdTATablet.unapply }

  /** Маппинг для всего textAlign. */
  val textAlignM = mapping(
    "phone"  -> taPhoneM,
    "tablet" -> taTabletM
  )
  { MMartAdTextAlign.apply }
  { MMartAdTextAlign.unapply }


  private val VENDOR_MAXLEN = 32

  /** apply() для product-маппингов. */
  private def adProductMApply(vendor: MMAdStringField, price: MMAdPrice, oldPrice: Option[MMAdPrice]) = {
    MMartAdProduct.apply(vendor, price, oldPrice)
  }

  /** unapply() для product-маппингов. */
  private def adProductMUnapply(adProduct: MMartAdProduct) = {
    Some((adProduct.vendor, adProduct.price, adProduct.oldPrice))
  }

  // Общие для ad-форм мапперы закончились. Пора запилить сами формы и формоспецифичные элементы.
  val adProductM = mapping(
    "vendor"    -> mmaStringFieldM(nonEmptyText(maxLength = VENDOR_MAXLEN)),
    "price"     -> mmaPriceM,
    "oldPrice"  -> mmaPriceOptM
  )
  { adProductMApply }
  { adProductMUnapply }


  private val DISCOUNT_TEXT_MAXLEN = 64

  /** Кусок формы, ориентированный на оформление скидочной рекламы. */
  val adDiscountM = {
    val discountTextM = nonEmptyText(maxLength = DISCOUNT_TEXT_MAXLEN)
      .transform(strTrimBrOnlyF, strIdentityF)
    val tplM = mapping(
      "id"    -> number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
      "color" -> colorM
    )
    { DiscountTemplate.apply }
    { DiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> mmaFloatFieldM(discountPercentM),
      "template"  -> tplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { MMartAdDiscount.apply }
    { MMartAdDiscount.unapply }
  }

  private val AD_TEXT_MAXLEN = 160
  /** Форма для задания текстовой рекламы. */
  val adTextM = {
    val textM = nonEmptyText(maxLength = 200)
      .transform({ adTextFmtPolicy.sanitize }, strIdentityF)
      .verifying("text.too.len", { _.length <= AD_TEXT_MAXLEN })

    mapping(
      "text" -> mmaStringFieldM(textM)
    )
    { MMartAdText.apply }
    { MMartAdText.unapply }
  }


  // Дублирующиеся куски маппина выносим за пределы метода.
  private val CAT_ID_K = "catId"
  private val AD_IMG_ID_K = "image_key"

  private val panelColorM = colorM
    .transform(
      { MMartAdPanelSettings.apply },
      { mmaps: MMartAdPanelSettings => mmaps.color }
    )
  private val PANEL_COLOR_K = "panelColor"
  private val OFFER_K = "offer"
  private val textAlignKM = "textAlign" -> textAlignM


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  private def adFormApply[T <: MMartAdOfferT](userCatId: Option[String], panelSettings: MMartAdPanelSettings, adBody: T, textAlign: MMartAdTextAlign) = {
    MMartAd(
      martId      = null,
      offers      = List(adBody),
      img         = null,
      shopId      = null,
      panel       = Some(panelSettings),
      userCatId   = userCatId,
      textAlign   = textAlign,
      companyId   = null
    )
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  private def adFormUnapply[T <: MMartAdOfferT](mmad: MMartAd) = {
    import mmad._
    if (panel.isDefined && userCatId.isDefined && !offers.isEmpty) {
      val adBody = offers.head.asInstanceOf[T]  // TODO Надо что-то решать с подтипами офферов. Параметризация типов MMartAd - геморрой.
      Some((userCatId, panel.get, adBody, textAlign))
    } else {
      warn("Unexpected ad object received into ad-product form: " + mmad)
      None
    }
  }

  private val shopCatIdKM = CAT_ID_K -> userCatIdOptM.verifying(_.isDefined)

  private val panelColorKM = PANEL_COLOR_K -> panelColorM

  /** Генератор форм добавления/редактирования рекламируемого продукта в зависимости от вкладок. */
  private def getShopAdFormM[T <: MMartAdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> imgIdJpegM,
    MarketShopLk.logoImgOptIdKM,
    "ad" -> mapping(
      shopCatIdKM,
      panelColorKM,
      OFFER_K -> offerM,
      textAlignKM
    )(adFormApply[T])(adFormUnapply[T])
  ))

  private val shopAdProductFormM  = getShopAdFormM(adProductM)
  private val shopAdDiscountFormM = getShopAdFormM(adDiscountM)
  private val shopAdTextFormM     = getShopAdFormM(adTextM)

  /** Извлекатель данных по логотипу из MShop/MMart. */
  implicit private def entityOpt2logoOpt(ent: Option[BuyPlaceT[_]]): LogoOpt_t = {
    ent.flatMap { _.logoImgId }
      .map { logoImgId => ImgInfo4Save(OrigImgIdKey(logoImgId)) }
  }

  type ReqSubmit = Request[collection.Map[String, Seq[String]]]
  type DetectForm_t = Either[AdFormM, (MMartAdOfferType, AdFormM)]

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectAdForm(dflt: AdFormM)(adMode2form: String => FormDetected_t)(implicit request: ReqSubmit): DetectForm_t = {
    val adModes = request.body.get("ad.offer.mode") getOrElse Nil
    adModes.headOption.flatMap(adMode2form) match {
      case Some(result) =>
        Right(result)

      case None =>
        warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
        val form = dflt.withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
        Left(form)
    }
  }

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectShopAdForm(implicit request: ReqSubmit): DetectForm_t = {
    detectAdForm(shopAdProductFormM) { FormModes.maybeShopFormWithName }
  }

  /**
   * Страница, занимающаяся создание рекламной карточки.
   * @param shopId id магазина.
   */
  def createShopAd(shopId: ShopId_t) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    renderCreateShopFormWith(
      af = shopAdProductFormM,
      catOwnerId = mshop.martId getOrElse shopId,
      mshop = mshop
    ) map {
      Ok(_)
    }
  }

  /** Рендер ошибки в create-форме. Довольно общий, но асинхронный код.
    * @param formWithErrors Форма для рендера.
    * @param catOwnerId id владельца категории. Обычно id ТЦ.
    * @param mshop Магазин, с которым происходит сейчас работа.
    * @return NotAcceptable со страницей с create-формой.
    */
  private def createShopAdFormError(formWithErrors: AdFormM, catOwnerId: String, mshop: MShop)(implicit ctx: util.Context) = {
    renderCreateShopFormWith(formWithErrors, catOwnerId, mshop) map {
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
    detectShopAdForm match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
            createShopAdFormError(formWithErrors, catOwnerId, mshop)
          },
          {case (imgKey, logoImgIdOpt, mmad) =>
            // Асинхронно обрабатываем логотип.
            val updateLogoFut = updateLogo(logoImgIdOpt, mshop)
            updateLogoFut onComplete entityLogoUpdatePf("shop", shopId)
            // Обработать MMartAd
            ImgFormUtil.updateOrigImg(Some(ImgInfo4Save(imgKey)), oldImgs = None) flatMap { imgIdsSaved =>
              if (!imgIdsSaved.isEmpty) {
                // TODO Нужно проверить категорию.
                mmad.shopId = Some(shopId)
                mmad.companyId = request.mshop.companyId
                mmad.martId = request.mshop.martId.get
                mmad.img = imgIdsSaved.head
                // Сохранить изменения в базу
                for {
                  adId <- mmad.save
                  _ <- updateLogoFut
                } yield {
                  Redirect(routes.MarketShopLk.showShop(shopId, newAdId = Some(adId)))
                    .flashing("success" -> "Рекламная карточка создана.")
                }

              } else {
                debug(logPrefix + "Failed to handle img key: " + imgKey)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                renderCreateShopFormWith(formWithError, catOwnerId, request.mshop) map { render =>
                  NotAcceptable(render)
                }
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

  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateShopFormWith(af: AdFormM, catOwnerId: String, mshop: MShop)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createShopAdTpl(mshop, mmcats, af, MMartAdOfferTypes.PRODUCT)
    }
  }

  private def topCatsAsAcc(catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    MMartCategory.findTopForOwner(catOwnerId) map {
      topCats => List(None -> topCats)
    }
  }
  

  /** Выдать над и под-категории по отношению к указанной категории. */
  private def nearCatsList(catOwnerId: String, catId: String): Future[CollectMMCatsAcc_t] = {
    val subcatsFut = MMartCategory.findDirectSubcatsOf(catId)
    for {
      upCats  <- MMartCategory.collectCatListsUpTo(catOwnerId=catOwnerId, currCatId=catId)
      subcats <- subcatsFut
    } yield {
      if (!subcats.isEmpty)
        upCats ++ List(None -> subcats)
      else
        upCats
    }
  }

  private def renderEditShopFormWith(af: AdFormM, mshopOpt: Option[MShop], mad: MMartAd)(implicit ctx: Context) = {
    getMMCatsForEdit(af, mad) map { mmcats =>
      mshopOpt map { mshop =>
        editShopAdTpl(mshop, mad, mmcats, af)
      }
    }
  }

  private def renderFailedEditShopFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    import request.mad
    // TODO Надо фетчить магазин и категории одновременно.
    request.mshopOptFut flatMap { mshopOpt =>
      renderEditShopFormWith(af, mshopOpt, mad) map {
        case Some(render) => NotAcceptable(render)
        case None => shopNotFound(mad.shopId.get)
      }
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editShopAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    request.mshopOptFut flatMap { mshopOpt =>
      val imgIdKey = OrigImgIdKey(mad.img.id)
      val formFilled = FormModes.getShopFormForClass(mad.offers.head) fill ((imgIdKey, mshopOpt, mad))
      renderEditShopFormWith(formFilled, mshopOpt, mad) map {
        case Some(render) => Ok(render)
        case None => shopNotFound(mad.shopId.get)
      }
    }
  }

  /** Импортировать выхлоп маппинга формы в старый экземпляр рекламы. Этот код вызывается во всех editAd-экшенах. */
  private def importFormAdData(oldMad: MMartAd, newMad: MMartAd) {
    oldMad.offers = newMad.offers
    oldMad.panel = newMad.panel
    oldMad.prio = newMad.prio
    oldMad.textAlign = newMad.textAlign
    oldMad.userCatId = newMad.userCatId
  }

  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editShopAdSubmit(adId: String) = IsAdEditor(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    detectShopAdForm match {
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(s"editShopAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
            renderFailedEditShopFormWith(formWithErrors)
          },
          {case (iik, logoImgIdOpt, mad2) =>
            // Надо обработать логотип, который приходит в составе формы. Это можно делать независимо от самой MMartAd.
            // Если выставлен tmp-логотип, то надо запустить обновление mshop.
            val shopId = mad.shopId.get
            val updateLogoFut = request.mshopOptFut flatMap {
              case Some(mshop)  => updateLogo(logoImgIdOpt, mshop)
              case None         => Future failed new NoSuchElementException(s"Shop not found: " + shopId)
            }
            updateLogoFut onComplete entityLogoUpdatePf("shop", shopId)
            // TODO Проверить категорию.
            // TODO И наверное надо проверить shopId-существование в исходной рекламе.
            ImgFormUtil.updateOrigImg(
              needImgs = Some(ImgInfo4Save(iik)),
              oldImgs  = Some(mad.img)
            ) flatMap { savedImgs =>
              // В списке сохраненных id картинок либо 1 либо 0 картинок.
              if (!savedImgs.isEmpty) {
                mad.img = savedImgs.head
                importFormAdData(oldMad = mad, newMad = mad2)
                for {
                  _ <- mad.save
                  _ <- updateLogoFut
                } yield {
                  Redirect(routes.MarketShopLk.showShop(shopId))
                    .flashing("success" -> "Изменения сохранены")
                }

              } else {
                // Не удалось обработать картинку. Вернуть форму назад
                debug(s"editShopAdSubmit($adId): Failed to update iik = " + iik)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                renderFailedEditShopFormWith(formWithError)
              }
            }
          }
        )

      case Left(formWithGlobalError) =>
        val formWithErrors = formWithGlobalError.bindFromRequest()
        renderFailedEditShopFormWith(formWithErrors)
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


  /** Форма для маппинга результатов  */
  val adShowLevelFormM = Form(tuple(
    // id уровня, прописано в чекбоксе
    "levelId" -> nonEmptyText(maxLength = 1)
      .transform(
        { AdShowLevels.maybeWithName },
        { slOpt: Option[AdShowLevel] => slOpt match {
          case Some(sl) => sl.toString
          case None => ""
        }}
      )
      .verifying("ad.show.level.undefined", { _.isDefined })
      .transform(_.get, { sl: AdShowLevel => Some(sl) })
    ,
    "levelEnabled" -> boolean   // Новое состояние чекбокса.
  ))

  /** Включение/выключение какого-то уровня отображения указанной рекламы.
    * Сабмит сюда должен отсылаться при нажатии на чекбоксы отображения на тех или иных экранах в _showAdsTpl:
    * [x] Выводить в общем каталоге
    * [x] Выводить в моём магазине
    * [x] Размещение на первом экране
    */
  def updateShowLevelSubmit(adId: String) = IsAdEditor(adId).async { implicit request =>
    adShowLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"updateShowLevelSubmit($adId): Failed to bind form: " + formWithErrors.errors)
        NotAcceptable("Request body invalid.")
      },
      {case (levelId, isLevelEnabled) =>
        import request.mad
        // Нужно, чтобы настройки отображения также повлияли на выдачу:
        val showLevels1 = if(isLevelEnabled) {
          mad.showLevels + levelId
        } else {
          mad.showLevels - levelId
        }
        mad.showLevels = showLevels1
        val ssFut = mad.saveShowLevels
        // Вернуть результат
        ssFut map { _ =>
          Ok("Updated ok.")
        }
      }
    )
  }



  // ============================ ТЦ ================================


  private val martCatIdKM = CAT_ID_K -> userCatIdOptM
  /** Генератор форм добавления/редактирования рекламиры в ТЦ в зависимости от вкладок.
    * Категория не обязательная, логотип от ТЦ. */
  private def getMartAdFormM[T <: MMartAdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> imgIdJpegM,
    MarketMartLk.martLogoImgIdOptKM,
    "ad" -> mapping(
      martCatIdKM,
      panelColorKM,
      OFFER_K -> offerM,
      textAlignKM
    )(adFormApply[T])(adFormUnapply[T])
  ))

  private val martAdProductFormM  = getMartAdFormM(adProductM)
  private val martAdDiscountFormM = getMartAdFormM(adDiscountM)
  private val martAdTextFormM     = getMartAdFormM(adTextM)

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectMartAdForm(implicit request: ReqSubmit): DetectForm_t = {
    detectAdForm(martAdProductFormM) { FormModes.maybeMartFormWithName }
  }

  /**
   * Экшен Страницы, которая занимается созданием рекламной карточки для ТЦ.
   * @param martId id ТЦ.
   */
  def createMartAd(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    renderCreateMartFormWith(
      af = martAdProductFormM,
      catOwnerId = martId,
      mmart = request.mmart
    ) map {
      Ok(_)
    }
  }


  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateMartFormWith(af: AdFormM, catOwnerId: String, mmart: MMart)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createMartAdTpl(mmart, mmcats, af, MMartAdOfferTypes.PRODUCT)
    }
  }
 
  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param martId id ТЦ
    */
  def createMartAdSubmit(martId: MartId_t) = IsMartAdmin(martId).async(parse.urlFormEncoded) { implicit request =>
    import request.mmart
    val catOwnerId = mmart.id.get 
    lazy val logPrefix = s"createMartAdSubmit($martId): "
    detectMartAdForm match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" +
              formWithErrors.errors.map { e => "  " + e.key + " -> " + e.message }.mkString("\n"))
            createMartAdFormError(formWithErrors, catOwnerId, mmart)
          },
          {case (imgKey, logoImgIdOpt, mmad) =>
            // Асинхронно обрабатываем логотип.
            val updateLogoFut = updateLogo(logoImgIdOpt, mmart)
            updateLogoFut onComplete entityLogoUpdatePf("mart", martId)
            // Обрабатываем данные по рекламе
            ImgFormUtil.updateOrigImg(Some(ImgInfo4Save(imgKey)), oldImgs = None) flatMap {
              case imgIdsSaved if !imgIdsSaved.isEmpty =>
                // TODO Нужно проверить категорию.
                mmad.shopId = None
                mmad.companyId = mmart.companyId
                mmad.martId = martId
                mmad.img = imgIdsSaved.head
                // Сохранить изменения в базу
                for {
                  adId <- mmad.save
                  _ <- updateLogoFut
                } yield {
                  Redirect(routes.MarketMartLk.martShow(martId, newAdId = Some(adId)))
                    .flashing("success" -> "Рекламная карточка создана.")
                }

              case _ =>
                debug(logPrefix + "Failed to handle img key: " + imgKey)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                renderCreateMartFormWith(formWithError, catOwnerId, mmart) map { render =>
                  NotAcceptable(render)
                }
            }
          }
        )

      // Не ясно, как именно надо биндить тело реквеста на маппинг формы.
      case Left(formWithGlobalError) =>
        warn(logPrefix + "AD mode is undefined or invalid. Returning form back.")
        val formWithErrors = formWithGlobalError.bindFromRequest()
        createMartAdFormError(formWithErrors, catOwnerId, mmart)
    }
  }
 
  


  /** Рендер ошибки в create-форме. Довольно общий, но асинхронный код.
    * @param formWithErrors Форма для рендера.
    * @param catOwnerId id владельца категории. Обычно id ТЦ.
    * @param mmart ТЦ, с которым происходит сейчас работа.
    * @return NotAcceptable со страницей с create-формой.
    */
  private def createMartAdFormError(formWithErrors: AdFormM, catOwnerId: String, mmart: MMart)(implicit ctx: util.Context) = {
    renderCreateMartFormWith(formWithErrors, catOwnerId, mmart) map {
      NotAcceptable(_)
    }
  }
  
  
  private def renderEditMartFormWith(af: AdFormM, mmartOpt: Option[MMart])(implicit request: RequestWithAd[_]) = {
    import request.mad
    getMMCatsForEdit(af, mad) map { mmcats =>
      mmartOpt map { mmart =>
        editMartAdTpl(mmart, mad, mmcats, af)
      }
    }
  }

  private def renderFailedEditMartFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    request.mmartOptFut flatMap { mmartOpt =>
      renderEditMartFormWith(af, mmartOpt) map {
        case Some(render) => NotAcceptable(render)
        case None => martNotFound(request.mad.martId)
      }
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editMartAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    val imgIdKey = OrigImgIdKey(mad.img.id)
    request.mmartOptFut flatMap { mmartOpt =>
      val formFilled = FormModes.getShopFormForClass(mad.offers.head) fill ((imgIdKey, mmartOpt, mad))
      renderEditMartFormWith(formFilled, mmartOpt) map {
        case Some(render) => Ok(render)
        case None => martNotFound(mad.martId)
      }
    }
  }


  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editMartAdSubmit(adId: String) = IsAdEditor(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    detectMartAdForm match {
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(s"editMartAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
            renderFailedEditMartFormWith(formWithErrors)
          },
          {case (iik, logoImgIdOpt, mad2) =>
            // Надо обработать логотип, который приходит в составе формы. Это можно делать независимо от самой MMartAd.
            // Если выставлен tmp-логотип, то надо запустить обновление mshop.
            val martId = mad.martId
            val updateLogoFut = request.mmartOptFut flatMap {
              case Some(mmart)  => updateLogo(logoImgIdOpt, mmart)
              case None         => Future failed new NoSuchElementException(s"Mart not found: " + martId)
            }
            updateLogoFut onComplete entityLogoUpdatePf("mart", martId)
            // Обрабатываем ad-часть формы
            // TODO Проверить категорию.
            ImgFormUtil.updateOrigImg(
              needImgs = Some(ImgInfo4Save(iik)),
              oldImgs = Some(mad.img)
            ) flatMap { savedImgIds =>
              // В списке сохраненных id картинок либо 1 либо 0 картинок.
              if (!savedImgIds.isEmpty) {
                mad.img = savedImgIds.head
                importFormAdData(oldMad = mad, newMad = mad2)
                for {
                  _ <- mad.save
                  _ <- updateLogoFut
                } yield {
                  Redirect(routes.MarketMartLk.martShow(martId))
                    .flashing("success" -> "Изменения сохранены")
                }

              } else {
                // Не удалось обработать картинку. Вернуть форму назад
                debug(s"editShopAdSubmit($adId): Failed to update iik = " + iik)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                renderFailedEditMartFormWith(formWithError)
              }
            }
          }
        )

      case Left(formWithGlobalError) =>
        val formWithErrors = formWithGlobalError.bindFromRequest()
        renderFailedEditShopFormWith(formWithErrors)
    }
  }



  // ========================== common-методы для Shop и ТЦ ==============================

  private def maybeAfCatId(af: AdFormM) = {
    val catIdK = "ad." + CAT_ID_K
    af(catIdK).value.filter { _ => af.errors(catIdK).isEmpty }
  }

  /** Получение списков категорий на основе формы и владельца категорий. */
  private def getMMCatsForCreate(af: AdFormM, catOwnerId: String): Future[MMartCategory.CollectMMCatsAcc_t] = {
    val catIdOpt = maybeAfCatId(af)
    catIdOpt match {
      case Some(catId) =>
        nearCatsList(catOwnerId=catOwnerId, catId=catId)
          .filter { !_.isEmpty }
          .recoverWith { case ex: NoSuchElementException => topCatsAsAcc(catOwnerId) }

      case None => topCatsAsAcc(catOwnerId)
    }
  }


  private def getMMCatsForEdit(af: AdFormM, mad: MMartAd): Future[CollectMMCatsAcc_t] = {
    val catOwnerId = mad.martId
    maybeAfCatId(af).orElse(mad.userCatId) match {
      case Some(catId) => nearCatsList(catOwnerId=catOwnerId, catId=catId)
      case None => topCatsAsAcc(catOwnerId)
    }
  }

  /** Асинхронно обновить логотип магазина или ТЦ. */
  private def updateLogo(logoImgIdOpt: LogoOpt_t, entity: BuyPlaceT[_]): Future[_] = {
    ImgFormUtil.updateOrigImgId(
      needImg = logoImgIdOpt,
      oldImgId = entity.logoImgId
    ) flatMap {
      case Nil if logoImgIdOpt.isDefined =>
        Future failed new NoSuchElementException(s"Cannot save new logo for mart=${entity.id.get} . Ignoring...")
      case savedImgIds =>
        val maybeNewLogo = savedImgIds.headOption.map(_.id)
        if (entity.logoImgId != maybeNewLogo) {
          entity.logoImgId = maybeNewLogo
          entity.save
        } else {
          Future successful ()
        }
    }
  }

  /** Логгинг для результатов асинхронного обновления логотипов. */
  private def entityLogoUpdatePf(ent: String, entId: String): PartialFunction[Try[_], Unit] = {
    case Success(_)  => trace(s"Logo for $ent=$entId updated ok")
    case Failure(ex) => error(s"Cannot update logo for $ent=$entId", ex)
  }

  private def shopNotFound(shopId: ShopId_t) = NotFound("Shop not found: " + shopId)
  private def martNotFound(martId: MartId_t) = NotFound("Mart not found: " + martId)
  private def adEditWrong = Forbidden("Nobody cat edit this ad using this action.")


  // ================================== preview-фунционал ========================================

  /** Объект, содержащий дефолтовые значения для preview-формы. Нужен для возможности простого импорта значений
    * в шаблон формы и для изоляции области видимости от другого кода. */
  object PreviewFormDefaults {
    /** Дефолтовый id картинки, когда она не задана. */
    val IMG_ID = "TODO_IMG_ID"   // TODO Нужен id для дефолтовой картинки.

    val TEXT_COLOR = "000000"
    val TEXT_FONT  = MMAdFieldFont(TEXT_COLOR)
    
    object Product {
      val PRICE_VALUE = Price(100F)
      val OLDPRICE_VALUE = Price(200F)
    }

    object Discount {
      val TPL_ID    = DISCOUNT_TPL_ID_MIN
      val DISCOUNT  = 50F
    }

    object Text {
      val TEXT = "Низкие цены в этом месяце"
    }
  }


  private val prevCatIdKM = CAT_ID_K -> optional(userCatIdM)
  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM[T <: MMartAdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
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

  private val floatInvalidIgnored = -1F
  private val floatFieldTolerantM = text.transform(
    { txt =>
      try {
        Math.max(txt.toFloat, 0F)
      } catch {
        case _: Exception => floatInvalidIgnored
      }
    },
    { value: Float =>
      if (value < 0F) "???" else value.toString }
  )

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
        value = MMAdStringField(vendorDflt, PreviewFormDefaults.TEXT_FONT)
      ),

      "price" -> mapping(
        "value" -> priceM,
        "color" -> fontColorM
      )
      {case ((rawPrice, priceOpt), font) =>
        val price = priceOpt getOrElse PreviewFormDefaults.Product.PRICE_VALUE
        MMAdPrice(price.price, price.currency.getCurrencyCode, rawPrice, font)
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
    val discountTextM = nonEmptyText(maxLength = DISCOUNT_TEXT_MAXLEN)
      .transform(strTrimBrOnlyF, strIdentityF)
    val tplM = mapping(
      "id"    -> default(
        mapping = number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
        value   = PreviewFormDefaults.Discount.TPL_ID
      ),
      "color" -> colorM
    )
    { DiscountTemplate.apply }
    { DiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> {
        val discountTolerantM = percentM.transform[Float](
          {case (_, pcOpt) => pcOpt getOrElse PreviewFormDefaults.Discount.DISCOUNT },
          {pc => adhocPercentFmt(pc) -> Some(pc) }
        )
        mmaFloatFieldOptM(discountTolerantM).transform(
          {dcOpt => dcOpt getOrElse MMAdFloatField(PreviewFormDefaults.Discount.DISCOUNT, PreviewFormDefaults.TEXT_FONT) },
          {dc: MMAdFloatField => Some(dc) }
        )
      },
      "template"  -> tplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { MMartAdDiscount.apply }
    { MMartAdDiscount.unapply }
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
    { MMartAdText.apply }
    { MMartAdText.unapply }
  }


  private def previewAdProductFormM(vendorDflt: String) = getPreviewAdFormM(previewProductM(vendorDflt))
  private val previewAdDiscountFormM = getPreviewAdFormM(previewAdDiscountM)
  private val previewAdTextFormM = getPreviewAdFormM(previewAdTextM)

  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectAdPreviewForm(vendorDflt: String)(implicit request: Request[collection.Map[String, Seq[String]]]): Either[AdFormM, (MMartAdOfferType, AdFormM)] = {
    val adModes = request.body.get("ad.offer.mode") getOrElse Nil
    adModes.headOption.flatMap { adModeStr =>
      MMartAdOfferTypes.maybeWithName(adModeStr)
    } map { adMode =>
      val adForm = adMode match {
        case MMartAdOfferTypes.PRODUCT  => previewAdProductFormM(vendorDflt)
        case MMartAdOfferTypes.DISCOUNT => previewAdDiscountFormM
        case MMartAdOfferTypes.TEXT     => previewAdTextFormM
      }
      adMode -> adForm
    } match {
      case Some(result) =>
        Right(result)

      case None =>
        warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
        val form = shopAdProductFormM.withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
        Left(form)
    }
  }

  import views.html.market.showcase._single_offer

  /** Магазин сабмиттит форму для preview. */
  def adFormPreviewShopSubmit(shopId: ShopId_t) = IsMartAdminShop(shopId).async(parse.urlFormEncoded) { implicit request =>
    MShop.getById(shopId) map {
      case Some(mshop) =>
        detectAdPreviewForm(mshop.name) match {
          case Right((offerType, adFormM)) =>
            adFormM.bindFromRequest().fold(
              {formWithErrors =>
                debug(s"adFormPreviewShopSubmit($shopId): form bind failed: " + formWithErrors.errors)
                NotAcceptable("Preview form bind failed.")
              },
              {case (iik, logoOpt, mad) =>
                mad.img = MImgInfo(iik.key)
                mshop.logoImgId = logoOpt.map(_.iik.key)
                mad.shopId = Some(shopId)
                mad.martId = request.martId
                Ok(_single_offer(mad, request.mmart, Some(mshop)))
              }
            )

          case Left(formWithGlobalError) =>
            NotAcceptable("Form mode invalid")
      }

      case None => shopNotFound(shopId)
    }
  }

  /** ТЦ сабмиттит форму для preview. */
  def adFormPreviewMartSubmit(martId: MartId_t) = IsMartAdmin(martId)(parse.urlFormEncoded) { implicit request =>
    import request.mmart
    detectAdPreviewForm(mmart.name) match {
      case Right((offerType, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"adFormPreviewMartSubmit($martId): form bind failed: " + formWithErrors.errors)
            NotAcceptable("Form bind failed")
          },
          {case (iik, logoOpt, mad) =>
            mad.img = MImgInfo(iik.key)
            mmart.logoImgId = logoOpt.map(_.iik.key)
            mad.shopId = None
            mad.martId = martId
            Ok(_single_offer(mad, mmart, None))
          }
        )

      case Left(formWithErrors) =>
        NotAcceptable("Form mode invalid.")
    }
  }

}

