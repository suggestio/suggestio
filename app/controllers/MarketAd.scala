package controllers

import util.Context
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
import MMartCategory.CollectMMCatsAcc_t
import scala.util.{Try, Failure, Success}
import util.HtmlSanitizer.adTextFmtPolicy
import io.suggest.ym.model.common.AdNetMemberTypes
import io.suggest.ym.ad.ShowLevelsUtil
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with LogoSupport {

  import LOGGER._

  /** Режимы работы формы добавления рекламной карточки. Режимы перенесены в MMartAdOfferTypes. */
  object FormModes {
    import AdOfferTypes._

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

    val getShopForm: PartialFunction[AdOfferType, AdFormM] = {
      case PRODUCT  => shopAdProductFormM
      case DISCOUNT => shopAdDiscountFormM
      case TEXT     => shopAdTextFormM
    }

    val getMartForm: PartialFunction[AdOfferType, AdFormM] = {
      case PRODUCT  => martAdProductFormM
      case DISCOUNT => martAdDiscountFormM
      case TEXT     => martAdTextFormM
    }

    val getForClass: PartialFunction[AdOfferT, AdOfferType] = {
      case _: AOProduct  => PRODUCT
      case _: AODiscount => DISCOUNT
      case _: AOText     => AdOfferTypes.TEXT
    }

    def getMartFormForClass(c: AdOfferT): AdFormM = getMartForm(getForClass(c))
    def getShopFormForClass(c: AdOfferT): AdFormM = getShopForm(getForClass(c))
  }


  // Общие для ad-форм мапперы закончились. Пора запилить сами формы и формоспецифичные элементы.
  val adProductM = mapping(
    "vendor"    -> mmaStringFieldM(nonEmptyText(maxLength = VENDOR_MAXLEN)),
    "price"     -> mmaPriceM,
    "oldPrice"  -> mmaPriceOptM
  )
  { adProductMApply }
  { adProductMUnapply }


  /** Кусок формы, ориентированный на оформление скидочной рекламы. */
  val adDiscountM = {
    val discountTextM = nonEmptyText(maxLength = DISCOUNT_TEXT_MAXLEN)
      .transform(strTrimBrOnlyF, strIdentityF)
    val tplM = mapping(
      "id"    -> number(min = DISCOUNT_TPL_ID_MIN, max = DISCOUNT_TPL_ID_MAX),
      "color" -> colorM
    )
    { AODiscountTemplate.apply }
    { AODiscountTemplate.unapply }
    // Собираем итоговый маппинг для MMartAdDiscount.
    mapping(
      "text1"     -> optional(mmaStringFieldM(discountTextM)),
      "discount"  -> mmaFloatFieldM(discountPercentM),
      "template"  -> tplM,
      "text2"     -> optional(mmaStringFieldM(discountTextM))
    )
    { AODiscount.apply }
    { AODiscount.unapply }
  }

  /** Форма для задания текстовой рекламы. */
  val adTextM = {
    val textM = nonEmptyText(maxLength = 200)
      .transform({ adTextFmtPolicy.sanitize }, strIdentityF)
      .verifying("text.too.len", { _.length <= AD_TEXT_MAXLEN })

    mapping(
      "text" -> mmaStringFieldM(textM)
    )
    { AOText.apply }
    { AOText.unapply }
  }


  // Дублирующиеся куски маппина выносим за пределы метода.
  val CAT_ID_K = "catId"
  val AD_IMG_ID_K = "image_key"


  private val shopCatIdKM = CAT_ID_K -> userCatIdOptM.verifying(_.isDefined)


  val AD_TEMP_LOGO_MARKER = "adLogo"
  private val ad2ndLogoImgIdOptKM = ImgFormUtil.getLogoKM("ad.logo.invalid", marker=AD_TEMP_LOGO_MARKER)

  /** Генератор форм добавления/редактирования рекламируемого продукта в зависимости от вкладок. */
  private def getShopAdFormM[T <: AdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> imgIdJpegM,
    ad2ndLogoImgIdOptKM,
    "ad" -> mapping(
      shopCatIdKM,
      panelColorKM,
      OFFER_K -> offerM,
      textAlignKM
    )(adFormApply[T])(adFormUnapply[T])
  ))

  val shopAdProductFormM  = getShopAdFormM(adProductM)
  private val shopAdDiscountFormM = getShopAdFormM(adDiscountM)
  private val shopAdTextFormM     = getShopAdFormM(adTextM)

  implicit private def mad2logoOpt(mad: MAd): LogoOpt_t = {
    mad.logoImgOpt.map { logoImg =>
      ImgInfo4Save(OrigImgIdKey(logoImg.id, logoImg.meta))
    }
  }

  type ReqSubmit = Request[collection.Map[String, Seq[String]]]
  type DetectForm_t = Either[AdFormM, (AdOfferType, AdFormM)]

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
  def createShopAd(shopId: String) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    renderCreateShopFormWith(
      af = shopAdProductFormM,
      catOwnerId = mshop.adn.supId getOrElse shopId,
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
  private def createShopAdFormError(formWithErrors: AdFormM, catOwnerId: String, mshop: MAdnNode)(implicit ctx: util.Context) = {
    renderCreateShopFormWith(formWithErrors, catOwnerId, mshop) map {
      NotAcceptable(_)
    }
  }

  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param shopId id магазина.
    */
  def createShopAdSubmit(shopId: String) = IsShopAdm(shopId).async(parse.urlFormEncoded) { implicit request =>
    import request.mshop
    val catOwnerId = request.mshop.adn.supId getOrElse shopId
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
            val updateLogoFut = ImgFormUtil.updateOrigImgId(needImg = logoImgIdOpt, oldImgId = None)
            updateLogoFut onComplete entityLogoUpdatePf("shop", shopId)
            // Обработать MMartAd
            ImgFormUtil.updateOrigImg(Some(ImgInfo4Save(imgKey)), oldImgs = None) flatMap { imgIdsSaved =>
              updateLogoFut flatMap { savedLogos =>
                if (!imgIdsSaved.isEmpty) {
                  // TODO Нужно проверить категорию.
                  mmad.producerId = shopId
                  mmad.logoImgOpt = savedLogos.headOption
                  mmad.img = imgIdsSaved.head
                  // Сохранить изменения в базу
                  mmad.save.map { adId =>
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
  private def renderCreateShopFormWith(af: AdFormM, catOwnerId: String, mshop: MAdnNode)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createShopAdTpl(mshop, mmcats, af, AdOfferTypes.PRODUCT)
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

  private def renderEditShopFormWith(af: AdFormM, mshopOpt: Option[MAdnNode], mad: MAd)(implicit ctx: Context) = {
    val catOwnerId = mshopOpt.flatMap(_.adn.supId).getOrElse(mad.producerId)
    getMMCatsForEdit(af, mad, catOwnerId) map { mmcats =>
      mshopOpt map { mshop =>
        editShopAdTpl(mshop, mad, mmcats, af)
      }
    }
  }

  private def renderFailedEditShopFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    import request.mad
    // TODO Надо фетчить магазин и категории одновременно.
    request.producerOptFut flatMap { mshopOpt =>
      renderEditShopFormWith(af, mshopOpt, mad) map {
        case Some(render) => NotAcceptable(render)
        case None => shopNotFound(mad.producerId)
      }
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editShopAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    request.producerOptFut flatMap { mshopOpt =>
      val imgIdKey = OrigImgIdKey(mad.img.id)
      val formFilled = FormModes.getShopFormForClass(mad.offers.head) fill ((imgIdKey, mad, mad))
      renderEditShopFormWith(formFilled, mshopOpt, mad) map {
        case Some(render) => Ok(render)
        case None => shopNotFound(mad.producerId)
      }
    }
  }

  /** Импортировать выхлоп маппинга формы в старый экземпляр рекламы. Этот код вызывается во всех editAd-экшенах. */
  private def importFormAdData(oldMad: MAd, newMad: MAd) {
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
            // Надо обработать вторичный логотип, который приходит в составе формы. Это можно делать независимо от самой MMartAd.
            // Если выставлен tmp-логотип, то надо запустить обновление mshop.
            val shopId = mad.producerId
            val updateLogoFut = ImgFormUtil.updateOrigImg(needImgs = logoImgIdOpt, oldImgs = mad.logoImgOpt)
            updateLogoFut onComplete entityLogoUpdatePf("ad", adId)
            // TODO Проверить категорию.
            // TODO И наверное надо проверить shopId-существование в исходной рекламе.
            ImgFormUtil.updateOrigImg(
              needImgs = Some(ImgInfo4Save(iik)),
              oldImgs  = Some(mad.img)
            ) flatMap { savedImgs =>
              updateLogoFut flatMap { savedLogos =>
                // В списке сохраненных id картинок либо 1 либо 0 картинок.
                if (!savedImgs.isEmpty) {
                  mad.img = savedImgs.head
                  mad.logoImgOpt = savedLogos.headOption
                  importFormAdData(oldMad = mad, newMad = mad2)
                  mad.save.map { _ =>
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
    val producerOptFut = request.producerOptFut
    MAd.deleteById(adId) flatMap { _ =>
      producerOptFut map {
        case Some(adnNode) =>
          import AdNetMemberTypes._
          import request.mad.producerId
          val routeCall = adnNode.adn.memberType match {
            case MART => routes.MarketMartLk.martShow(producerId)
            case SHOP => routes.MarketShopLk.showShop(producerId)
            case other =>
              warn(s"deleteSubmit($adId): Redirect not yet implemented for memberType = $other")
              routes.MarketLk.lkList()
          }
          Redirect(routeCall)
            .flashing("success" -> "Рекламная карточка удалена")

        case None => http404AdHoc
      }
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
    lazy val logPrefix = s"updateShowLevelSubmit($adId): "
    adShowLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
        NotAcceptable("Request body invalid.")
      },
      {case (levelId, isLevelEnabled) =>
        import request.mad
        // Бывает, что ресиверы ещё не выставлены. Тогда нужно найти получателя и вписать его сразу.
        val additionalReceiversFut: Future[Receivers_t] = if (mad.receivers.isEmpty) {
          val rcvrsFut = detectReceivers(request.producerOptFut)
          rcvrsFut onSuccess {
            case result =>
              debug(logPrefix + "No receivers found in Ad. Generated new receivers map: " + result.valuesIterator.mkString("[", ", ", "]"))
          }
          rcvrsFut
        } else {
          Future successful Map.empty
        }
        additionalReceiversFut flatMap { addRcvrs =>
          mad.receivers ++= addRcvrs
          // Нужно, чтобы настройки отображения также повлияли на выдачу:
          val slUpdF: Set[AdShowLevel] => Set[AdShowLevel] = if (isLevelEnabled) {
            { asl => asl + levelId }
          } else {
            { asl => asl - levelId }
          }
          mad.updateAllWantLevels(slUpdF)
          request.producerOptFut flatMap {
            case Some(producer) =>
              mad.applyOutputConstraintsFor(producer) flatMap { appliedAds =>
                ShowLevelsUtil.saveAllReceivers(appliedAds)
              } map { _ =>
                Ok("Updated ok.")
              }
            case None => ???
          }
        }
      }
    )
  }


  /** Детектор получателей рекламы. Заглядывает к себе и к прямому родителю, если он указан. */
  private def detectReceivers(producerOptFut: Future[Option[MAdnNode]]): Future[Receivers_t] = {
    producerOptFut flatMap { producerOpt =>
      val supRcvrIdsFut: Future[Seq[String]] = producerOpt
        .flatMap {
          _.adn.supId
        } map { supId =>
          MAdnNodeCache.getByIdCached(supId)
            .map { _.filter(_.adn.isReceiver).map(_.idOrNull).toSeq }
        } getOrElse {
          Future successful Nil
        }
      val selfRcvrIds: Seq[String] = producerOpt
        .filter(_.adn.isReceiver)
        .map(_.idOrNull)
        .toSeq
      supRcvrIdsFut map { supRcvrIds =>
        val rcvrIds: Seq[String] = supRcvrIds ++ selfRcvrIds
        rcvrIds.distinct.map { rcvrId =>
          rcvrId -> AdReceiverInfo(rcvrId)
        }.toMap
      }
    }
  }


  // ============================ ТЦ ================================


  private val martCatIdKM = CAT_ID_K -> userCatIdOptM
  /** Генератор форм добавления/редактирования рекламиры в ТЦ в зависимости от вкладок.
    * Категория не обязательная, логотип от ТЦ. */
  private def getMartAdFormM[T <: AdOfferT](offerM: Mapping[T]): AdFormM = Form(tuple(
    AD_IMG_ID_K -> imgIdJpegM,
    ad2ndLogoImgIdOptKM,
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
  def createMartAd(martId: String) = IsMartAdmin(martId).async { implicit request =>
    renderCreateMartFormWith(
      af = martAdProductFormM,
      catOwnerId = martId,
      mmart = request.mmart
    ) map {
      Ok(_)
    }
  }


  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateMartFormWith(af: AdFormM, catOwnerId: String, mmart: MAdnNode)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createMartAdTpl(mmart, mmcats, af, AdOfferTypes.PRODUCT)
    }
  }
 
  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param martId id ТЦ
    */
  // TODO Дедублицировать этот код с create-кодом для shop
  def createMartAdSubmit(martId: String) = IsMartAdmin(martId).async(parse.urlFormEncoded) { implicit request =>
    import request.mmart
    val catOwnerId = mmart.id.get 
    lazy val logPrefix = s"createMartAdSubmit($martId): "
    detectMartAdForm match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((formMode, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
            createMartAdFormError(formWithErrors, catOwnerId, mmart)
          },
          {case (imgKey, logoImgIdOpt, mmad) =>
            // Асинхронно обрабатываем логотип.
            val updateLogoFut = ImgFormUtil.updateOrigImgId(needImg = logoImgIdOpt, oldImgId = None)
            updateLogoFut onComplete entityLogoUpdatePf("mart", martId)
            // Обрабатываем данные по рекламе
            ImgFormUtil.updateOrigImg(Some(ImgInfo4Save(imgKey)), oldImgs = None) flatMap { imgsSaved =>
              updateLogoFut.flatMap { savedLogos =>
                if (!imgsSaved.isEmpty) {
                  // TODO Нужно проверить категорию.
                  mmad.producerId = martId
                  // Добавляем самих себя в получатели
                  mmad.receivers = Map(martId -> AdReceiverInfo(martId))
                  mmad.img = imgsSaved.head
                  mmad.logoImgOpt = savedLogos.headOption
                  // Сохранить изменения в базу
                  mmad.save.map { adId =>
                    Redirect(routes.MarketMartLk.martShow(martId, newAdId = Some(adId)))
                      .flashing("success" -> "Рекламная карточка создана.")
                  }

                } else {
                  debug(logPrefix + "Failed to handle img key: " + imgKey)
                  val formWithError = formBinded.withError(AD_IMG_ID_K, "error.image.save")
                  renderCreateMartFormWith(formWithError, catOwnerId, mmart) map { render =>
                    NotAcceptable(render)
                  }
                }
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
 
 
  /**
   * Загрузка картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  // TODO Дедублицировать с MML.handleMartTempLogo
  def handleAdTempLogo = IsAuth(parse.multipartFormData) { implicit request =>
    handleLogo(AdLogoImageUtil, AD_TEMP_LOGO_MARKER)
  }
 


  /** Рендер ошибки в create-форме. Довольно общий, но асинхронный код.
    * @param formWithErrors Форма для рендера.
    * @param catOwnerId id владельца категории. Обычно id ТЦ.
    * @param mmart ТЦ, с которым происходит сейчас работа.
    * @return NotAcceptable со страницей с create-формой.
    */
  private def createMartAdFormError(formWithErrors: AdFormM, catOwnerId: String, mmart: MAdnNode)(implicit ctx: util.Context) = {
    renderCreateMartFormWith(formWithErrors, catOwnerId, mmart) map {
      NotAcceptable(_)
    }
  }
  
  
  private def renderEditMartFormWith(af: AdFormM, mmartOpt: Option[MAdnNode])(implicit request: RequestWithAd[_]) = {
    import request.mad
    val catOwnerId = mmartOpt.map(_.id.get).getOrElse(mad.producerId)
    getMMCatsForEdit(af, mad, catOwnerId) map { mmcats =>
      mmartOpt map { mmart =>
        editMartAdTpl(mmart, mad, mmcats, af)
      }
    }
  }

  private def renderFailedEditMartFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    request.producerOptFut flatMap { mmartOpt =>
      renderEditMartFormWith(af, mmartOpt) map {
        case Some(render) => NotAcceptable(render)
        case None => martNotFound(request.producerId)
      }
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editMartAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    val imgIdKey = OrigImgIdKey(mad.img.id)
    request.producerOptFut flatMap { mmartOpt =>
      val formFilled = FormModes.getShopFormForClass(mad.offers.head) fill ((imgIdKey, mad, mad))
      renderEditMartFormWith(formFilled, mmartOpt) map {
        case Some(render) => Ok(render)
        case None => martNotFound(request.producerId)
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
            debug(s"editMartAdSubmit($adId): Failed to bind form: " + formatFormErrors(formWithErrors))
            renderFailedEditMartFormWith(formWithErrors)
          },
          {case (iik, logoImgIdOpt, mad2) =>
            // Надо обработать логотип, который приходит в составе формы. Это можно делать независимо от самой MMartAd.
            // Если выставлен tmp-логотип, то надо запустить обновление mshop.
            val updateLogoFut = ImgFormUtil.updateOrigImg(needImgs = logoImgIdOpt, oldImgs = mad.logoImgOpt)
            updateLogoFut onComplete entityLogoUpdatePf("mart", mad.producerId)
            // Обрабатываем ad-часть формы
            // TODO Проверить категорию.
            ImgFormUtil.updateOrigImg(
              needImgs = Some(ImgInfo4Save(iik)),
              oldImgs = Some(mad.img)
            ) flatMap { savedImgIds =>
              updateLogoFut flatMap { savedLogos =>
                // В списке сохраненных id картинок либо 1 либо 0 картинок.
                if (!savedImgIds.isEmpty) {
                  mad.img = savedImgIds.head
                  mad.logoImgOpt = savedLogos.headOption
                  importFormAdData(oldMad = mad, newMad = mad2)
                  mad.save.map { _ =>
                    Redirect(routes.MarketMartLk.martShow(mad.producerId))
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


  private def getMMCatsForEdit(af: AdFormM, mad: MAd, catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    maybeAfCatId(af).orElse(mad.userCatId) match {
      case Some(catId) => nearCatsList(catOwnerId=catOwnerId, catId=catId)
      case None => topCatsAsAcc(catOwnerId)
    }
  }


  /** Логгинг для результатов асинхронного обновления логотипов. */
  private def entityLogoUpdatePf(ent: String, entId: String): PartialFunction[Try[_], Unit] = {
    case Success(_)  => trace(s"Logo for $ent=$entId updated ok")
    case Failure(ex) => error(s"Cannot update logo for $ent=$entId", ex)
  }

  private def shopNotFound(shopId: String) = NotFound("Shop not found: " + shopId)
  private def martNotFound(martId: String) = NotFound("Mart not found: " + martId)


}

