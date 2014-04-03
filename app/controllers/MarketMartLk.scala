package controllers

import _root_.util.{Context, PlayMacroLogsImpl}
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import MMart.MartId_t
import MShop.ShopId_t
import io.suggest.model.EsModel
import views.html.market.lk.mart._
import play.api.data._, Forms._
import util.FormUtil._
import MarketShopLk.{shopFormM, shopFullFormM}
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current
import util.img._
import net.sf.jmimemagic.Magic
import scala.Some
import util.acl.IsMartAdminShop
import util.img.ImgInfo4Save
import util.img.OrigImgIdKey
import play.api.libs.json._
import scala.concurrent.Future
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Security.username

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:54
 * Description: Личный кабинет для sio-маркета. Тут управление торговым центром и магазинами в нём.
 */
object MarketMartLk extends SioController with PlayMacroLogsImpl with BruteForceProtect with LogoSupport {

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
        Some(EsModel.MART_FLOOR_ESFN)
      } else {
        None
      }
    }
  }

  import ShopSort._

  val martM = mapping(
    "name"      -> martNameM,
    "town"      -> townM,
    "address"   -> martAddressM,
    "site_url"  -> optional(urlStrMapper),
    "phone"     -> optional(phoneM)
  )
  // applyF()
  {(name, town, address, siteUrlOpt, phoneOpt) =>
    MMart(name=name, town=town, address=address, siteUrl=siteUrlOpt, companyId=null, phone=phoneOpt, personIds=null)
  }
  // unapplyF()
  {mmart =>
    import mmart._
    Some((name, town, address, siteUrl, phone))
  }


  /** Маппер для необязательного логотипа магазина. */
  val martLogoImgIdOptKM = ImgFormUtil.getLogoKM("mart.logo.invalid", marker=MART_TMP_LOGO_MARKER)

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(tuple(
    "mart" -> martM,
    martLogoImgIdOptKM
  ))


  /** Маппинг формы приглашения магазина в систему. */
  val inviteShopFormM = Form(mapping(
    "name"          -> shopNameM,
    "email"         -> email,
    "mart_floor"    -> martFloorM,
    "mart_section"  -> martSectionM
  )
  // applyF()
  {(name, email, martFloor, martSection) =>
    email -> MShop(name=name, martFloor=Some(martFloor), martSection=Some(martSection), companyId=null, personIds=Nil)
  }
  // unapplyF()
  {case (_email, mshop) =>
    import mshop._
    Some((name, _email, martFloor.get, martSection.get))
  })

  /**
   * Рендер раздачи страницы с личным кабинетом торгового центра.
   * @param martId id ТЦ
   * @param newAdIdOpt Запрошено отображение для указанной карточки в реальном времени. Необходимо, если новая карточка
   *                была добавлена на предыдущей странице.
   */
  def martShow(martId: MartId_t, newAdIdOpt: Option[String]) = IsMartAdmin(martId).async { implicit request =>
    // Бывает, что есть дополнительная реклама, которая появится в выдаче только по наступлению index refresh. Тут костыль для отработки этого.
    val extAdOptFut = newAdIdOpt match {
      case Some(newAdId) => MMartAd.getById(newAdId).map { _.filter { mad => mad.martId == martId } }
      case None => Future successful None
    }
    for {
      mads      <- MMartAd.findForMartRt(martId, shopMustMiss = true)
      extAdOpt  <- extAdOptFut
    } yield {
      // Если есть карточка в extAdOpt, то надо добавить её в начало списка, который отсортирован по дате создания.
      val mads2 = if (extAdOpt.isDefined  &&  mads.headOption.flatMap(_.id) != newAdIdOpt) {
        extAdOpt.get :: mads
      } else {
        mads
      }
      Ok(martShowTpl(request.mmart, mads2))
    }
  }

  /**
   * Рендер страницы со списком арендаторов.
   * @param martId id ТЦ
   * @param sortByRaw Сортировка магазинов по указанному полю. Если не задано, то порядок не определён.
   * @param isReversed Если true, то будет сортировка в обратном порядке. Иначе в прямом.
   */
  def shopsShow(martId: MartId_t, sortByRaw: Option[String], isReversed: Boolean) = IsMartAdmin(martId).async { implicit request =>
    val sortBy = sortByRaw flatMap handleShopsSortBy
    MShop.findByMartId(martId, sortBy, isReversed) map { shops =>
      Ok(shopsShowTpl(request.mmart, shops))
    }
  }

  /**
   * Рендер страницы с формой редактирования ТЦ в личном кабинете.
   * @param martId id ТЦ.
   */
  def martEditForm(martId: MartId_t) = IsMartAdmin(martId).apply { implicit request =>
    import request.mmart
    val martLogoOpt = mmart.logoImgId.map { imgId => ImgInfo4Save(OrigImgIdKey(imgId)) }
    val formFilled = martFormM.fill((mmart, martLogoOpt))
    Ok(martEditFormTpl(mmart, formFilled))
  }

  /**
   * Сабмит формы редактирования ТЦ.
   * @param martId id ТЦ.
   */
  def martEditFormSubmit(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"martEditFormSubmit($martId): Failed to bind form: " + formWithErrors.errors)
        NotAcceptable(martEditFormTpl(mmart, formWithErrors))
          .flashing("error" -> "Ошибка заполнения формы.")
      },
      {case (mmart2, logoImgIdOpt) =>
        // В фоне обновляем логотип ТЦ
        val savedLogoFut = ImgFormUtil.updateOrigImgId(logoImgIdOpt, oldImgId = mmart2.logoImgId)
        mmart.name = mmart2.name
        mmart.town = mmart2.town
        mmart.address = mmart2.address
        mmart.siteUrl = mmart2.siteUrl
        mmart.phone = mmart2.phone
        savedLogoFut flatMap { savedLogos =>
          mmart.logoImgId = savedLogos.headOption.map(_.id)
          mmart.save.map { _ =>
            Redirect(routes.MarketMartLk.martShow(martId))
              .flashing("success" -> "Изменения сохранены.")
          }
        }
      }
    )

  }


  /**
   * Рендер страницы с формой инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopForm(martId: MartId_t) = IsMartAdmin(martId).apply { implicit request =>
    Ok(shop.shopInviteFormTpl(request.mmart, inviteShopFormM))
  }

  /**
   * Сабмит формы инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopFormSubmit(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    inviteShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"inviteShopFormSubmit($martId): Bind failed: " + formWithErrors.errors)
        NotAcceptable(shop.shopInviteFormTpl(mmart, formWithErrors))
      },
      {case (_email, mshop) =>
        mshop.martId = Some(martId)
        val mcFut = MCompany(mshop.name).save
        // Параллельно создать юзера и запись по его активации.
        mcFut.flatMap { companyId =>
          mshop.companyId = companyId
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
                bodyHtml = shop.emailShopInviteTpl(mmart, mshop, eAct)(ctx),
                bodyText = views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct)(ctx)
              )
              // Собственно, результат работы.
              Redirect(routes.MarketMartLk.martShow(martId))
                .flashing("success" -> s"Добавлен магазин: '${mshop.name}'.")
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
  def editShopForm(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    import request.mmart
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        MarketShopLk.fillFullForm(mshop) map { formBinded =>
          Ok(shop.shopEditFormTpl(mmart, mshop, formBinded))
        }

      case None => shopNotFound(shopId)
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
  def searchShops(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    searchFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"searchShops($martId): Failed to bind search form: " + formWithErrors.errors)
        NotAcceptable("Bad search request")
      },
      {q =>
        MShop.searchAll(q, martId = Some(martId)) map { result =>
          Ok(_martShopsTpl(martId, result))
        }
      }
    )
  }

  /**
   * Сабмит формы редактирования магазина-арендатора.
   * @param shopId id редактируемого магазина.
   */
  def editShopFormSubmit(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        shopFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"editShopFormSubmit($shopId): Form bind failed: " + formWithErrors.errors)
            MarketShopLk.fillFullForm(mshop) map { formWithErrors2 =>
              val fwe3 = formWithErrors2.bindFromRequest()
              NotAcceptable(shop.shopEditFormTpl(request.mmart, mshop, fwe3))
            }
          },
          {mshop2 =>
            // Пора накатить изменения на текущий магазин и сохранить
            mshop.name = mshop2.name
            mshop.description = mshop2.description
            mshop.martFloor = mshop2.martFloor
            mshop.martSection = mshop2.martSection
            mshop.save.map { _ =>
              Redirect(routes.MarketMartLk.showShop(shopId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        )

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Отобразить страницу по магазину.
   * @param shopId id магазина.
   */
  def showShop(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    import request.mmart
    val madsFut = MMartAd.findForShop(shopId)
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        madsFut map { mads =>
          Ok(shop.shopShowTpl(mmart, mshop, mads))
        }

      case None => shopNotFound(shopId)
    }
  }


  /**
   * Загрузка картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleMartTempLogo(martId: MartId_t) = IsMartAdmin(martId)(parse.multipartFormData) { implicit request =>
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
  def shopOnOffForm(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    MShop.getById(shopId).map {
      case Some(mshop) =>
        val formBinded = shopOnOffFormM.fill((false, mshop.settings.supDisableReason))
        Ok(shop._onOffFormTpl(mshop, formBinded))

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Владелец ТЦ включает/выключает состояние магазина.
   * @param shopId id магазина.
   * @return 200 Ok если всё ок.
   */
  def shopOnOffSubmit(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    shopOnOffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopOnOffSubmit($shopId): Bind form failed: " + formWithErrors.errors)
        NotAcceptable("Bad request body.")
      },
      {case (isEnabled, reason) =>
        MShop.setIsEnabled(shopId, isEnabled = isEnabled, reason = reason) map { _ =>
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
    val shopId = ad.shopId.get
    MShop.getById(shopId) map {
      case Some(mshop) =>
        Ok(shop._shopAdHideFormTpl(mshop, ad, request.mmart, shopAdHideFormM))

      case None => shopNotFound(shopId)
    }
  }

  /** Сабмит формы сокрытия/удаления формы. */
  def shopAdHideFormSubmit(adId: String) = IsMartAdminShopAd(adId).async { implicit request =>
    // TODO Надо поразмыслить над ответами. Вероятно, тут нужны редиректы или jsonp-команды.
    val rdr = Redirect(routes.MarketMartLk.showShop(request.ad.shopId.get))
    shopAdHideFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopAdHideFormSubmit($adId): Form bind failed: " + formWithErrors.errors)
        rdr.flashing("error" -> "Необходимо указать причину")
      },
      {case (HideShopAdActions.HIDE, reason) =>
        request.ad.showLevels = Set.empty
        request.ad.saveShowLevels map { _ =>
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
  private def notyfyAdDisabled(reason: String)(implicit request: ShopMartAdRequest[_]) {
    import request.{mmart, ad}
    ad.shopId.foreach { shopId =>
      MShop.getById(shopId) onSuccess { case Some(mshop) =>
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
  }


  /** Форма, которая используется при обработке сабмита о переключении доступности магазину функции отображения рекламы
    * на верхнем уровне ТЦ. */
  val shopTopLevelFormM = Form(
    "isEnabled" -> boolean
  )

  /** Владелец ТЦ дергает за переключатель доступности top-level выдачи для магазина. */
  def setShopTopLevelAvailable(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    shopTopLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopSetTopLevel($shopId): Form bind failed: " + formWithErrors.errors)
        NotAcceptable("Cannot parse req body.")
      },
      {isTopEnabled =>
        MShop.getById(shopId) flatMap {
          case Some(mshop) =>
            mshop.settings.supWithLevels = if (isTopEnabled) {
              mshop.settings.supWithLevels + AdShowLevels.LVL_MART_SHOWCASE
            } else {
              mshop.settings.supWithLevels - AdShowLevels.LVL_MART_SHOWCASE
            }
            mshop.saveShopLevels map { _ =>
              Ok("updated ok")
            }

          case None => shopNotFound(shopId)
        }
      }
    )
  }


  import views.html.market.lk.mart.{invite => martInvite}

  // Обработка инвайтов на управление ТЦ.
  val martInviteAcceptM = Form(optional(passwordWithConfirmM))

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def martInviteAcceptForm(martId: MartId_t, eActId: String) = inviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    Ok(martInvite.inviteAcceptFormTpl(mmart, eAct, martInviteAcceptM))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def martInviteAcceptFormSubmit(martId: MartId_t, eActId: String) = inviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = martInviteAcceptM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"martInviteAcceptFormSubmit($martId, act=$eActId): Form bind failed: ${formWithErrors.errors}")
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
                mmart.personIds ::= personId
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

  private def inviteAcceptCommon(martId: MartId_t, eaId: String)(f: (EmailActivation, MMart) => AbstractRequestWithPwOpt[AnyContent] => Future[Result]) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtect flatMap { _ =>
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == martId =>
            MMart.getById(martId) flatMap {
              case Some(mmart) => f(eAct, mmart)(request)
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


  private def martNotFound(martId: MartId_t) = NotFound("mart not found: " + martId)  // TODO Нужно дергать 404-шаблон.
  private def shopNotFound(shopId: ShopId_t) = NotFound("Shop not found: " + shopId)  // TODO

}
