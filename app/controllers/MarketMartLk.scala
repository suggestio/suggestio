package controllers

import util.{Context, PlayMacroLogsImpl}
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import io.suggest.model.EsModel
import views.html.market.lk.mart._
import play.api.data._, Forms._
import util.FormUtil._
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current
import util.acl.IsMartAdminShop
import util.img._
import ImgFormUtil.imgInfo2imgKey
import play.api.libs.json._
import scala.concurrent.Future
import play.api.mvc.{Call, AnyContent, Result}
import play.api.mvc.Security.username
import scala.util.Success
import models._
import io.suggest.ym.model.common.EMAdnMMetadataStatic.META_FLOOR_ESFN
import controllers.adn.AdnShowLk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:54
 * Description: Личный кабинет для sio-маркета. Тут управление торговым центром и магазинами в нём.
 */
object MarketMartLk extends SioController with PlayMacroLogsImpl with BruteForceProtect with LogoSupport
with ShopMartCompat with AdnShowLk {

  import LOGGER._

  /** Маркер картинки для использования в качестве логотипа. */
  val MART_TMP_LOGO_MARKER = "martLogo"

  // Допустимые значения сортировки при выдаче магазинов.
  object ShopSort extends Enumeration {
    val SORT_BY_A_Z   = Value("a-z")
    val SORT_BY_CAT   = Value("cat")
    val SORT_BY_FLOOR = Value("floor")

    def handleShopsSortBy(sortRaw: String): Option[String] = {
      if (SORT_BY_A_Z.toString equalsIgnoreCase sortRaw) {
        Some(EsModel.NAME_ESFN)
      } else if (SORT_BY_CAT.toString equalsIgnoreCase sortRaw) {
        ???
      } else if (SORT_BY_FLOOR.toString equalsIgnoreCase sortRaw) {
        Some(META_FLOOR_ESFN)
      } else {
        None
      }
    }
  }

  import ShopSort._

  /** Маппер для метаданных. */
  val martMetaM = mapping(
    "name"      -> nameM,
    "town"      -> townM,
    "address"   -> martAddressM,
    "siteUrl"   -> optional(urlStrMapper),
    "phone"     -> optional(phoneM)
  )
  {(name, town, address, siteUrlOpt, phoneOpt) =>
    AdnMMetadata(
      name = name,
      town = Some(town),
      address = Some(address),
      siteUrl = siteUrlOpt,
      phone = phoneOpt
    )
  }
  {meta =>
    import meta._
    Some((name, town.getOrElse(""), address.getOrElse(""), siteUrl, phone)) }


  /** Маппер для необязательного логотипа магазина. */
  val martLogoImgIdOptKM = ImgFormUtil.getLogoKM("mart.logo.invalid", marker=MART_TMP_LOGO_MARKER)

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(tuple(
    "meta" -> martMetaM,
    "welcomeImgId" -> optional(ImgFormUtil.imgIdJpegM),
    martLogoImgIdOptKM
  ))


  val shopMetaM = mapping(
    "name"    -> shopNameM,
    "floor"   -> floorM,
    "section" -> sectionM
  )
  {(name, floor, section) =>
    AdnMMetadata(name, floor = Some(floor), section = Some(section))
  }
  {lei =>
    import lei._
    Some( (name, floor.getOrElse(""), section.getOrElse("")) )
  }

  /** Маппинг формы приглашения магазина в систему. */
  val inviteShopFormM = Form(tuple(
    "email" -> email,
    "meta" -> shopMetaM
  ))


  /** Форма на которой нельзя менять логотип, но можно настраивать разные поля.
    * Подходит для редактирования из ТЦ-аккаунта */
  val shopEditFormM = Form(
    MarketShopLk.shopMetaFullKM
  )

  
  /** Асинхронно получить welcome-ad-карточку. */
  private def getWelcomeAdOpt(mmart: MAdnNode): Future[Option[MWelcomeAd]] = {
    mmart.meta.welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }


  val showAdnNodeCtx = new ShowAdnNodeCtx {
    override def nodeEditCall(adnId: String): Call = routes.MarketMartLk.martEditForm(adnId)
    override def producersShowCall(adnId: String): Call = routes.MarketMartLk.shopsShow(adnId)
  }

  /**
   * Рендер раздачи страницы с личным кабинетом торгового центра.
   * @param martId id ТЦ
   * @param newAdIdOpt Запрошено отображение для указанной карточки в реальном времени. Необходимо, если новая карточка
   *                была добавлена на предыдущей странице.
   */
  def martShow(martId: String, newAdIdOpt: Option[String]) = IsMartAdmin(martId).async { implicit request =>
    renderShowAdnNode(request.mmart, martId, newAdIdOpt)
  }


  /**
   * Рендер страницы со списком арендаторов.
   * @param martId id ТЦ
   * @param sortByRaw Сортировка магазинов по указанному полю. Если не задано, то порядок не определён.
   * @param isReversed Если true, то будет сортировка в обратном порядке. Иначе в прямом.
   */
  def shopsShow(martId: String, sortByRaw: Option[String], isReversed: Boolean) = IsMartAdmin(martId).async { implicit request =>
    val sortBy = sortByRaw flatMap handleShopsSortBy
    MAdnNode.findBySupId(martId, sortBy, isReversed) map { shops =>
      Ok(shopsShowTpl(request.mmart, shops))
    }
  }

  /**
   * Рендер страницы с формой редактирования ТЦ в личном кабинете.
   * @param martId id ТЦ.
   */
  def martEditForm(martId: String) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    getWelcomeAdOpt(mmart) map { welcomeAdOpt =>
      val martLogoOpt = mmart.logoImgOpt.map { img =>
        ImgInfo4Save(imgInfo2imgKey(img))
      }
      val welcomeImgKey = welcomeAdOpt.map[OrigImgIdKey] { _.img }
      val formFilled = martFormM.fill((mmart.meta, welcomeImgKey, martLogoOpt))
      Ok(martEditFormTpl(mmart, formFilled, welcomeAdOpt))
    }
  }

  /**
   * Сабмит формы редактирования ТЦ.
   * @param martId id ТЦ.
   */
  def martEditFormSubmit(martId: String) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        val welcomeAdOptFut = getWelcomeAdOpt(mmart)
        debug(s"martEditFormSubmit($martId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        welcomeAdOptFut map { welcomeAdOpt =>
          NotAcceptable(martEditFormTpl(mmart, formWithErrors, welcomeAdOpt))
            .flashing("error" -> "Ошибка заполнения формы.")
        }
      },
      {case (martMeta, welcomeImgOpt, logoImgIdOpt) =>
        // В фоне обновляем логотип ТЦ
        val savedLogoFut = ImgFormUtil.updateOrigImg(logoImgIdOpt, oldImgs = mmart.logoImgOpt)
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut: Future[_] = getWelcomeAdOpt(request.mmart) flatMap { welcomeAdOpt =>
          ImgFormUtil.updateOrigImg(
            needImgs = welcomeImgOpt.map(ImgInfo4Save(_, withThumb = false)),
            oldImgs = welcomeAdOpt.map(_.img)
          ) flatMap { savedImgs =>
            savedImgs.headOption match {
              // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
              case None =>
                val deleteOldAdFut = mmart.meta.welcomeAdId
                  .fold [Future[_]] {Future successful ()} { MAd.deleteById }
                mmart.meta.welcomeAdId = None
                deleteOldAdFut

              // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
              case Some(newImgInfo) =>
                val welcomeAd = welcomeAdOpt
                  .map { welcomeAd =>
                  welcomeAd.img = newImgInfo
                  welcomeAd
                } getOrElse {
                  MWelcomeAd(producerId = martId, img = newImgInfo)
                }
                welcomeAd.save andThen {
                  case Success(welcomeAdId) =>
                    mmart.meta.welcomeAdId = Some(welcomeAdId)
                }
            }
          }
        }
        mmart.meta = martMeta
        savedLogoFut.flatMap { savedLogos =>
          mmart.logoImgOpt = savedLogos.headOption
          savedWelcomeImgsFut flatMap { _ =>
            mmart.save.map { _ =>
              Redirect(routes.MarketMartLk.martShow(martId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        }
      }
    )

  }


  /**
   * Рендер страницы с формой инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopForm(martId: String) = IsMartAdmin(martId).apply { implicit request =>
    Ok(shop.shopInviteFormTpl(request.mmart, inviteShopFormM))
  }

  /**
   * Сабмит формы инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopFormSubmit(martId: String) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    inviteShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"inviteShopFormSubmit($martId): Bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(shop.shopInviteFormTpl(mmart, formWithErrors))
      },
      {case (_email, meta) =>
        MCompany(meta.name).save.flatMap { companyId =>
          // Собираем дефолтовый магазин как будущий узел рекламной сети.
          val mshop = MAdnNode(
            companyId = companyId,
            personIds = Set.empty,
            adn = {
              val mi = AdNetMemberTypes.SHOP.getAdnInfoDflt
              mi.supId = Some(martId)
              mi.isEnabled = false
              mi.disableReason = Some("First run/inactive")
              mi
            },
            meta = meta
          )
          mshop.save.flatMap { shopId =>
            mshop.id = Some(shopId)
            val eAct = EmailActivation(email = _email, key = shopId)
            eAct.save.map { eaId =>
              eAct.id = Some(eaId)
              // Пора отправлять письмо юзеру с ссылкой для активации.
              trace(s"inviteShopFormSubmit($martId): shopId=$shopId companyId=$companyId eAct=$eAct :: Sending message to ${_email} ...")
              val mail = use[MailerPlugin].email
              mail.setSubject("Suggest.io | Подтверждение регистрации")
              mail.setFrom("no-reply@suggest.io")
              mail.setRecipient(_email)
              val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
              mail.send(
                bodyHtml = shop.emailShopInviteTpl(mmart, mshop=mshop, eAct)(ctx),
                bodyText = views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop=mshop, eAct)(ctx)
              )
              // Собственно, результат работы.
              Redirect(routes.MarketMartLk.martShow(martId))
                .flashing("success" -> s"Добавлен магазин: '${mshop.meta.name}'.")
            }
          }
        }
      }
    )
  }

  /**
   * Рендер страницы с формой редактирования магазина-арендатора.
   * Владельцу ТЦ доступны различные опции формы, недоступные для редактирования арендатору, поэтому для биндинга
   * используется именно расширенная форма.
   * @param shopId id магазина.
   */
  def editShopForm(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    import request.{mmart, mshop}
    MarketShopLk.fillFullForm(mshop) map { formBinded =>
      Ok(shop.shopEditFormTpl(mmart, mshop, formBinded))
    }
  }

  /** Поисковая форма. Сейчас в шаблонах она не используется, только в контроллере. */
  val searchFormM = Form(
    "q" -> nonEmptyText(maxLength = 64)
  )

  /**
   * Поиск по магазинам в рамках ТЦ.
   * @param martId id ТЦ.
   * @return 200 Отрендеренный список магазинов для отображения поверх существующей страницы.
   *         406 С сообщением об ошибке.
   */
  def searchShops(martId: String) = IsMartAdmin(martId).async { implicit request =>
    searchFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"searchShops($martId): Failed to bind search form: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad search request")
      },
      {q =>
        MAdnNode.searchAll(q, supId = Some(martId)) map { result =>
          Ok(_martShopsTpl(martId, result))
        }
      }
    )
  }

  /**
   * Сабмит формы редактирования магазина-арендатора.
   * @param shopId id редактируемого магазина.
   */
  def editShopFormSubmit(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    import request.mshop
    shopEditFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editShopFormSubmit($shopId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        MarketShopLk.fillFullForm(mshop) map { formWithErrors2 =>
          val fwe3 = formWithErrors2.bindFromRequest()
          NotAcceptable(shop.shopEditFormTpl(request.mmart, mshop, fwe3))
        }
      },
      {meta =>
        // Пора накатить изменения на текущий магазин и сохранить
        mshop.meta.name = meta.name
        mshop.meta.description = meta.description
        mshop.meta.floor = meta.floor
        mshop.meta.section = meta.section
        mshop.save.map { _ =>
          Redirect(routes.MarketMartLk.showShop(shopId))
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )

  }

  /**
   * Отобразить страницу по магазину.
   * @param shopId id магазина.
   */
  def showShop(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    import request.{mmart, mshop}
    MAd.findForProducer(shopId) map { mads =>
      Ok(shop.shopShowTpl(mmart, mshop, mads))
    }
  }


  /**
   * Загрузка картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleMartTempLogo(martId: String) = IsMartAdmin(martId)(parse.multipartFormData) { implicit request =>
    handleLogo(MartLogoImageUtil, MART_TMP_LOGO_MARKER)
  }

  /** Маппинг для задания причины при сокрытии сущности. */
  val hideEntityReasonM = nonEmptyText(maxLength = 512)
    .transform(strTrimSanitizeF, strIdentityF)

  /** Маппинг формы включения/выключения магазина. */
  val shopOnOffFormM = Form(tuple(
    "isEnabled" -> boolean,
    "reason"    -> optional(hideEntityReasonM)
  ))

  /**
   * Рендер блока с формой отключения магазина.
   * @param shopId id отключаемого магазина.
   * @return 200 с формой указания причины отключения магазина.
   *         404 если магазин не найден.
   */
  def shopOnOffForm(shopId: String) = IsMartAdminShop(shopId).apply { implicit request =>
    import request.mshop
    val formBinded = shopOnOffFormM.fill((false, mshop.adn.disableReason))
    Ok(shop._onOffFormTpl(mshop, formBinded))
  }

  /**
   * Владелец ТЦ включает/выключает состояние магазина.
   * @param shopId id магазина.
   * @return 200 Ok если всё ок.
   */
  def shopOnOffSubmit(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    shopOnOffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopOnOffSubmit($shopId): Bind form failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad request body.")
      },
      {case (isEnabled, reason) =>
        request.mshop.setIsEnabled(isEnabled, reason) map { _ =>
          val reply = JsObject(Seq(
            "isEnabled" -> JsBoolean(isEnabled),
            "shopId" -> JsString(shopId)
          ))
          Ok(reply)
        }
      }
    )
  }


  object HideShopAdActions extends Enumeration {
    type HideShopAdAction = Value
    // Тут пока только один вариант отключения карточки. Когда был ещё и DELETE.
    // Потом можно будет спилить варианты отключения вообще, если не понадобятся.
    val HIDE = Value

    def maybeWithName(n: String): Option[HideShopAdAction] = {
      try {
        Some(withName(n))
      } catch {
        case e: Exception => None
      }
    }
  }

  import HideShopAdActions.HideShopAdAction

  /** Форма сокрытия рекламы подчинённого магазина. */
  val shopAdHideFormM = Form(tuple(
    "action" -> nonEmptyText(maxLength = 10)
      .transform(
        strTrimF andThen { _.toUpperCase } andThen HideShopAdActions.maybeWithName,
        {aOpt: Option[HideShopAdAction] => (aOpt getOrElse "").toString }
      )
      .verifying("hide.action.invalid", { _.isDefined })
      .transform(
        _.get,
        { hsaa: HideShopAdAction => Some(hsaa) }
      ),
    "reason" -> hideEntityReasonM
  ))


  /** Рендер формы сокрытия какой-то рекламы. */
  def shopAdHideForm(adId: String) = IsMartAdminShopAd(adId).async { implicit request =>
    import request.ad
    val shopId = ad.producerId
    getShopByIdCache(shopId) map {
      case Some(mshop) =>
        Ok(shop._shopAdHideFormTpl(mshop, ad, request.mmart, shopAdHideFormM))

      case None => http404AdHoc
    }
  }

  /** Сабмит формы сокрытия/удаления формы. */
  def shopAdHideFormSubmit(adId: String) = IsMartAdminShopAd(adId).async { implicit request =>
    // TODO Надо поразмыслить над ответами. Вероятно, тут нужны редиректы или jsonp-команды.
    val rdr = Redirect(routes.MarketMartLk.showShop(request.ad.producerId))
    shopAdHideFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopAdHideFormSubmit($adId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        rdr.flashing("error" -> "Необходимо указать причину")
      },
      {case (HideShopAdActions.HIDE, reason) =>
        request.ad.receivers = Map.empty
        request.ad.saveReceivers map { _ =>
          // Отправить письмо магазину-владельцу рекламы
          notyfyAdDisabled(reason)
          rdr.flashing("success" -> "Объявление выключено")
        }
      }
    )
  }

  /**
   * Сообщить владельцу магазина, что его рекламу отключили.
   * @param reason Указанная причина отключения.
   */
  private def notyfyAdDisabled(reason: String)(implicit request: MartShopAdRequest[_]) {
    import request.{mmart, ad}
    val shopId = ad.producerId
    getShopByIdCache(shopId) onSuccess { case Some(mshop) =>
      mshop.mainPersonId.foreach { personId =>
        MPersonIdent.findAllEmails(personId) onSuccess { case emails =>
          if (emails.isEmpty) {
            warn(s"notifyAdDisabled(${ad.id.get}): No notify emails found for shop ${mshop.id.get}")
          } else {
            val mail = use[MailerPlugin].email
            mail.setSubject("Suggest.io | Отключена ваша рекламная карточка")
            mail.setFrom("no-reply@suggest.io")
            mail.setRecipient(emails : _*)
            val ctx = implicitly[Context]   // Нано-оптимизация: один контекст для обоих рендеров.
            mail.send(
              bodyHtml = views.html.market.lk.shop.ad.emailAdDisabledByMartTpl(mmart, mshop, ad, reason)(ctx),
              bodyText = views.txt.market.lk.shop.ad.emailAdDisabledByMartTpl(mmart, mshop, ad, reason)(ctx)
            )
          }
        }
      }
    }
  }


  /** Форма, которая используется при обработке сабмита о переключении доступности магазину функции отображения рекламы
    * на верхнем уровне ТЦ. */
  val shopTopLevelFormM = Form(
    "isEnabled" -> boolean
  )

  /** Владелец ТЦ дергает за переключатель доступности top-level выдачи для магазина. */
  def setShopTopLevelAvailable(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    shopTopLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopSetTopLevel($shopId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot parse req body.")
      },
      {isTopEnabled =>
        import request.mshop
        if (isTopEnabled)
          mshop.adn.showLevelsInfo.out += AdShowLevels.LVL_START_PAGE -> 1
        else
          mshop.adn.showLevelsInfo.out -= AdShowLevels.LVL_START_PAGE
        mshop.save map { _ =>
          Ok("updated ok")
        }
      }
    )
  }


  import views.html.market.lk.mart.{invite => martInvite}

  // Обработка инвайтов на управление ТЦ.
  val martInviteAcceptM = Form(optional(passwordWithConfirmM))

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def martInviteAcceptForm(martId: String, eActId: String) = inviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    Ok(martInvite.inviteAcceptFormTpl(mmart, eAct, martInviteAcceptM))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def martInviteAcceptFormSubmit(martId: String, eActId: String) = inviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = martInviteAcceptM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"martInviteAcceptFormSubmit($martId, act=$eActId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(martInvite.inviteAcceptFormTpl(mmart, eAct, formWithErrors))
      },
      {passwordOpt =>
        if (passwordOpt.isEmpty && !request.isAuth) {
          val form1 = formBinded
            .withError("pw1", "error.required")
            .withError("pw2", "error.required")
          NotAcceptable(martInvite.inviteAcceptFormTpl(mmart, eAct, form1))
        } else {
          // Сначала удаляем запись об активации, убедившись что она не была удалена асинхронно.
          eAct.delete.flatMap { isDeleted =>
            val newPersonIdOptFut: Future[Option[String]] = if (!request.isAuth) {
              MPerson(lang = lang.code).save flatMap { personId =>
                EmailPwIdent.applyWithPw(email = eAct.email, personId=personId, password = passwordOpt.get, isVerified = true)
                  .save
                  .map { emailPwIdentId => Some(personId) }
              }
            } else {
              Future successful None
            }
            // Для обновления полей MMart требуется доступ к personId. Дожидаемся сохранения юзера...
            newPersonIdOptFut flatMap { personIdOpt =>
              val personId = (personIdOpt orElse request.pwOpt.map(_.personId)).get
              if (!(mmart.personIds contains personId)) {
                mmart.personIds += personId
              }
              mmart.save.map { _martId =>
                Redirect(routes.MarketMartLk.martShow(martId))
                  .flashing("success" -> "Регистрация завершена.")
                  .withSession(username -> personId)
              }
            }
          }
        }
      }
    )
  }

  private def inviteAcceptCommon(martId: String, eaId: String)(f: (EmailActivation, MAdnNode) => AbstractRequestWithPwOpt[AnyContent] => Future[Result]) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtect flatMap { _ =>
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == martId =>
            getMartByIdCache(martId) flatMap {
              case Some(mmart) =>
                f(eAct, mmart)(request)

              case None =>
                // should never occur
                error(s"inviteAcceptCommon($martId, eaId=$eaId): Mart not found, but code for mart exist. This should never occur.")
                NotFound(martInvite.inviteInvalidTpl("mart.not.found"))
            }

          case other =>
            // Неверный код активации или id магазина. Если None, то код скорее всего истёк. Либо кто-то брутфорсит.
            debug(s"inviteAcceptCommon($martId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired?")
            // TODO Надо проверить, есть ли у юзера права на магазин, и если есть, то значит юзер дважды засабмиттил форму, и надо его сразу отредиректить в его магазин.
            // TODO Может и быть ситуация, что юзер всё ещё не залогинен, а второй сабмит уже тут. Нужно это тоже как-то обнаруживать. Например через временную сессионную куку из формы.
            warn(s"TODO I need to handle already activated requests!!!")
            NotFound(martInvite.inviteInvalidTpl("mart.activation.expired.or.invalid.code"))
        }
      }
    }
  }

}
