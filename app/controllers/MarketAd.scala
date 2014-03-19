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
import util.img._
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play.current
import models.AdShowLevels.AdShowLevel
import TextAlignValues.TextAlignValue
import MMartCategory.CollectMMCatsAcc_t
import scala.util.{Try, Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  type AdFormM = Form[(ImgIdKey, LogoOpt_t, MMartAd)]

  /** Режимы работы формы добавления рекламной карточки. Режимы отражают возможные варианты офферов. */
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

  /** Маппим необязательное Float-поле. */
  private def mmaFloatFieldOptM(m: Mapping[Float]) = mapping(
    "value" -> optional(m),
    "color" -> fontColorM
  )
  {(valueOpt, color) =>
    valueOpt map { MMAdFloatField(_, color) }
  }
  {_.map {
    mmaff => (Option(mmaff.value), mmaff.font) }
  }


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


  // Общие для ad-форм мапперы закончились. Пора запилить сами формы и формоспецифичные элементы.
  val adProductM = mapping(
    "vendor"    -> mmaStringFieldM(nonEmptyText(maxLength = 32)),
    "price"     -> mmaFloatFieldM(priceM),
    "oldPrice"  -> mmaFloatFieldOptM(priceM)
  )
  { MMartAdProduct.apply }
  { MMartAdProduct.unapply }

  /** Кусок формы, ориентированный на оформление скидочной рекламы. */
  val adDiscountM = {
    val discountTextM = nonEmptyText(maxLength = 64)
      .transform(strTrimBrOnlyF, strIdentityF)
    val discountValueM = float
      .verifying("discount.too.low", { _ <= 0F })
      .verifying("discount.too.big", { _ >= 200F })
    val tplM = mapping(
      "id"    -> number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
      "color" -> colorM
    )
    { DiscountTemplate.apply }
    { DiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> mmaFloatFieldM(discountValueM),
      "template"  -> tplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { MMartAdDiscount.apply }
    { MMartAdDiscount.unapply }
  }

  // Дублирующиеся куски маппина выносим за пределы метода.
  private val CAT_ID_K = "catId"
  private val catIdKM = CAT_ID_K -> userCatIdM
  private val AD_IMG_ID_K = "image_key"
  private val adImgIdKM = AD_IMG_ID_K  -> imgIdJpegM

  private val panelColorKM = "panelColor" -> colorM
    .transform(
      { MMartAdPanelSettings.apply },
      { mmaps: MMartAdPanelSettings => mmaps.color }
    )
  private val textAlignKM = "textAlign" -> textAlignM


  /** Генератор маппинга для MMartAd-части общей формы. */
  private def getAdM[T <: MMartAdOfferT](offerM: Mapping[T]) = mapping(
    catIdKM,
    panelColorKM,
    "offer" -> offerM,
    textAlignKM
  )
  // applyF()
  {(userCatId, panelSettings, adBody, textAlign) =>
    MMartAd(
      martId      = null,
      offers      = List(adBody),
      picture     = null,
      shopId      = null,
      panel       = Some(panelSettings),
      userCatId   = Some(userCatId),
      textAlign   = textAlign,
      companyId   = null
    )
  }
  // unapplyF()
  {mmad =>
    import mmad._
    if (panel.isDefined && userCatId.isDefined && !offers.isEmpty) {
      val adBody = offers.head.asInstanceOf[T]  // TODO Надо что-то решать с подтипами офферов. Параметризация типов MMartAd - геморрой.
      Some((userCatId.get, panel.get, adBody, textAlign))
    } else {
      warn("Unexpected ad object received into ad-product form: " + mmad)
      None
    }
  }

  /** Генератор форм добавления/редактирования рекламируемого продукта в зависимости от вкладок. */
  private def getAdFormM[T <: MMartAdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    adImgIdKM,
    MarketShopLk.logoImgOptIdKM,
    "ad" -> getAdM(offerM)
  ))

  val adProductFormM  = getAdFormM(adProductM)
  val adDiscountFormM = getAdFormM(adDiscountM)



  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def detectAdForm(implicit request: Request[collection.Map[String, Seq[String]]]): Either[AdFormM, (FormMode, AdFormM)] = {
    val adModes = request.body.get("ad.offer.mode") getOrElse Nil
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
    import request.mshop
    renderCreateFormWith(
      af = adProductFormM,
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
          {case (imgKey, logoImgIdOpt, mmad) =>
            // Асинхронно обрабатываем логотип.
            updateShopLogo(logoImgIdOpt, mshop) onComplete shopLogoUpdatePf(shopId)
            ImgFormUtil.updateOrigImg(Some(ImgInfo(imgKey)), oldImgs = None) flatMap {
              case imgIdsSaved if !imgIdsSaved.isEmpty =>
                // TODO Нужно проверить категорию.
                mmad.shopId = Some(shopId)
                mmad.companyId = request.mshop.companyId
                mmad.martId = request.mshop.martId.get
                mmad.picture = imgIdsSaved.head
                // Сохранить изменения в базу
                mmad.save.map { adId =>
                  Redirect(routes.MarketShopLk.showShop(shopId))
                    .flashing("success" -> "Рекламная карточка создана.")
                }

              case _ =>
                debug(logPrefix + "Failed to handle img key: " + imgKey)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
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
    val catIdOpt = af(CAT_ID_K).value.filter { _ => af.errors(CAT_ID_K).isEmpty }
    val mmcatsFut: Future[CollectMMCatsAcc_t] = catIdOpt match {
      case Some(catId) =>
        nearCatsList(catOwnerId=catOwnerId, catId=catId)
          .filter { _.isEmpty }
          .recoverWith { case ex: NoSuchElementException => topCatsAsAcc(catOwnerId) }

      case None => topCatsAsAcc(catOwnerId)
    }
    mmcatsFut map { mmcats =>
      createAdTpl(mshop, mmcats, af)
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

  private def renderEditFormWith(af: AdFormM, mshopOpt: Option[MShop], mad: MMartAd)(implicit ctx: Context) = {
    val catOwnerId = mad.martId
    val mmcatsFut: Future[CollectMMCatsAcc_t] = mad.userCatId match {
      case Some(catId) => nearCatsList(catOwnerId=catOwnerId, catId=catId)
      case None => topCatsAsAcc(catOwnerId)
    }
    for {
      mmcats   <- mmcatsFut
    } yield {
      mshopOpt map { mshop =>
        editAdTpl(mshop, mad, mmcats, af)
      }
    }
  }

  private def renderFailedEditFormWith(af: AdFormM, mad: MMartAd)(implicit ctx: Context) = {
    val shopId = mad.shopId.get
    // TODO Надо фетчить магазин и категории одновременно.
    MShop.getById(shopId) flatMap { mshopOpt =>
      renderEditFormWith(af, mshopOpt, mad) map {
        case Some(render) => NotAcceptable(render)
        case None => shopNotFound(shopId)
      }
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
        MShop.getById(_shopId) flatMap { mshopOpt =>
          val logoImgId = mshopOpt
            .flatMap { _.logoImgId }
            .map { logoImgId => ImgInfo(OrigImgIdKey(logoImgId)) }
          val formFilled = FormModes.getFormForClass(mad.offers.head) fill ((imgIdKey, logoImgId, mad))
          renderEditFormWith(formFilled, mshopOpt, mad) map {
            case Some(render) => Ok(render)
            case None => shopNotFound(_shopId)
          }
        }

      // Магазин в карточке не указан. Вероятно, это карточка должна редактироваться через какой-то другой экшен
      case None => adEditWrong
    }
  }

  /** Асинхронно обновить логотип магазина-бренда. */
  private def updateShopLogo(logoImgIdOpt: LogoOpt_t, mshop: MShop): Future[_] = {
    ImgFormUtil.updateOrigImgId(
      needImg = logoImgIdOpt,
      oldImgId = mshop.logoImgId
    ) flatMap {
      case Nil if logoImgIdOpt.isDefined =>
        Future failed new NoSuchElementException(s"Cannot save new logo for shop=${mshop.id.get} . Ignoring...")
      case savedImgIds =>
        mshop.logoImgId = savedImgIds.headOption
        mshop.save
    }
  }

  private def shopLogoUpdatePf(shopId: ShopId_t): PartialFunction[Try[_], Unit] = {
    case Success(_)  => trace(s"Logo for shop=$shopId updated ok")
    case Failure(ex) => error(s"Cannot update logo for shop=$shopId", ex)
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
          {case (iik, logoImgIdOpt, mad2) =>
            // Надо обработать логотип, который приходит в составе формы. Это можно делать независимо от самой MMartAd.
            // Если выставлен tmp-логотип, то надо запустить обновление mshop.
            val shopId = mad.shopId.get
            MShop.getById(shopId) flatMap {
              case Some(mshop)  => updateShopLogo(logoImgIdOpt, mshop)
              case None         => Future failed new NoSuchElementException(s"Shop not found: " + shopId)
            } onComplete shopLogoUpdatePf(shopId)
            // TODO Проверить категорию.
            // TODO И наверное надо проверить shopId-существование в исходной рекламе.
            ImgFormUtil.updateOrigImgId(
              needImg = Some(ImgInfo(iik)),
              oldImgId = Some(mad.picture)
            ) flatMap { savedImgIds =>
              // В списке сохраненных id картинок либо 1 либо 0 картинок.
              if (!savedImgIds.isEmpty) {
                mad2.id = mad.id
                mad2.martId = mad.martId
                mad2.shopId = mad.shopId
                mad2.companyId = mad.companyId
                mad2.picture = savedImgIds.head
                mad2.save.map { _ =>
                  Redirect(routes.MarketShopLk.showShop(mad.shopId.get))
                    .flashing("success" -> "Изменения сохранены")
                }

              } else {
                // Не удалось обработать картинку. Вернуть форму назад
                debug(s"editShopAdSubmit($adId): Failed to update iik = " + iik)
                val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                renderFailedEditFormWith(formWithError, mad)
              }
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
        import request.mad.showLevels
        val showLevels1 = if(isLevelEnabled) {
          showLevels + levelId
        } else {
          showLevels - levelId
        }
        MMartAd.setShowLevels(adId, showLevels1) map { _ =>
          Ok("Updated ok.")
        }
      }
    )
  }


  private def shopNotFound(shopId: ShopId_t) = NotFound("shop not found: " + shopId)
  private def adEditWrong = Forbidden("Nobody cat edit this ad using this action.")
}
