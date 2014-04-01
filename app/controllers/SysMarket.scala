package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.{IsShopAdm, IsSuperuser}
import models._
import views.html.sys1.market._
import views.html.market.lk
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.ym.model.{MMartSettings, MShopSettings, UsernamePw}
import MCompany.CompanyId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.{Context, IndicesUtil}
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
object SysMarket extends SioController with MacroLogsImpl {

  import LOGGER._

  /** Маппинг для формы добавления/редактирования компании. */
  val companyFormM = Form(
    "name" -> companyNameM
  )

  /** Индексная страница продажной части. Тут ссылки на дальнейшие страницы. */
  def index = IsSuperuser { implicit request =>
    Ok(marketIndexTpl())
  }

  /** Отрендерить sio-админу список всех компаний, зарегистрированных в системе. */
  def companiesList = IsSuperuser.async { implicit request =>
    MCompany.getAll.map { allCompanies =>
      val render = company.companiesListTpl(allCompanies)
      Ok(render)
    }
  }

  /** Отрендерить страницу с формой добавления новой компании. */
  def companyAddForm = IsSuperuser { implicit request =>
    Ok(company.companyAddFormTpl(companyFormM))
  }

  /** Самбит формы добавления новой компании. */
  def companyAddFormSubmit = IsSuperuser.async { implicit request =>
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        NotAcceptable(company.companyAddFormTpl(formWithErrors))
      },
      {name =>
        MCompany(name).save.map { companyId =>
          Redirect(routes.SysMarket.companyShow(companyId))
        }
      }
    )
  }

  /** Отобразить информацию по указанной компании.
    * @param companyId Числовой id компании.
    */
  def companyShow(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    val companyMartsFut = MMart.getByCompanyId(companyId)
    val companyShopsFut = MShop.getByCompanyId(companyId)
    MCompany.getById(companyId) flatMap {
      case Some(mc) =>
        for {
          marts <- companyMartsFut
          shops <- companyShopsFut
        } yield {
          Ok(company.companyShowTpl(mc, marts, shops))
        }

      case None => companyNotFound(companyId)
    }
  }


  /** Отрендерить страницу с формой редактирования компании. */
  def companyEditForm(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.getById(companyId) map {
      case Some(mc)  =>
        val form = companyFormM.fill(mc.name)
        Ok(company.companyEditFormTpl(mc, form))

      case None => companyNotFound(companyId)
    }
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.getById(companyId) flatMap {
      case Some(mc) =>
        companyFormM.bindFromRequest.fold(
          {formWithErrors =>
            NotAcceptable(company.companyEditFormTpl(mc, formWithErrors))
          },
          {name =>
            mc.name = name
            mc.save map { _ =>
              Redirect(routes.SysMarket.companyShow(companyId))
            }
          }
        )

      case None => companyNotFound(companyId)
    }
  }

  /** Админ приказал удалить указанную компанию. */
  def companyDeleteSubmit(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.deleteById(companyId) map {
      case true =>
        Redirect(routes.SysMarket.companiesList())
          .flashing("success" -> s"Company $companyId deleted.")

      case false => companyNotFound(companyId)
    }
  }


  /** Реакция на ошибку обращения к несуществующей компании. Эта логика расшарена между несколькими экшенами. */
  private def companyNotFound(companyId: CompanyId_t) = NotFound("Company not found: " + companyId)

  /** Бывает надо передать в шаблон карту всех контор. Тут фьючерс, который этим занимается. */
  private def allCompaniesMap = MCompany.getAll.map {
    _.map { mc => mc.id.get -> mc }.toMap
  }


  private def allMartsMap = MMart.getAll.map {
    _.map { mmart => mmart.id.get -> mmart }.toMap
  }


  /* Торговые центры и площади. */

  /** Рендер страницы со списком торговых центров. */
  def martsList = IsSuperuser.async { implicit request =>
    val allCompaniesMapFut = allCompaniesMap
    for {
      allMarts  <- MMart.getAll
      companies <- allCompaniesMapFut
    } yield {
      Ok(mart.martsListTpl(allMarts, companies=Some(companies)))
    }
  }

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(mapping(
    "name"      -> martNameM,
    "town"      -> townM,
    "address"   -> martAddressM,
    "siteUrl"   -> optional(urlStrMapper),
    "color"     -> optional(colorM),
    "phone"     -> optional(phoneM),
    "maxAds"    -> default(number(min = 0, max = 30), MMartSettings.MAX_L1_ADS_SHOWN)
  )
  {(name, town, address, siteUrlOpt, colorOpt, phoneOpt, maxAds) =>
    MMart(
      name = name,
      town = town,
      companyId = null,
      address = address,
      siteUrl = siteUrlOpt,
      phone = phoneOpt,
      personIds = Nil,
      color = colorOpt,
      settings = MMartSettings(maxAds)
    )
  }
  {mmart =>
    import mmart._
    Some((name, town, address, siteUrl, color, phone, settings.supL1MaxAdsShown))
  })


  /** Рендер страницы с формой добавления торгового центра. */
  def martAddForm(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.isExist(companyId) map {
      case true  => Ok(mart.martAddFormTpl(companyId, martFormM))
      case false => companyNotFound(companyId)
    }
  }

  /** Сабмит формы добавления торгового центра. */
  def martAddFormSubmit(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"martAddFormSubmt($companyId): Form bind failed: " + formWithErrors.errors)
        NotAcceptable(mart.martAddFormTpl(companyId, formWithErrors))
      },
      {mmart =>
        mmart.companyId = companyId
        mmart.personIds = Nil
        mmart.save map { mmartSavedId =>
          Redirect(routes.SysMarket.martShow(mmartSavedId))
        }
      }
    )
  }

  /** Отображение одного ТЦ. */
  def martShow(martId: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(martId) flatMap {
      case Some(mmart) =>
        val martShopsFut = MShop.findByMartId(martId)
        for {
          ownerCompanyOpt <- mmart.company
          martShops       <- martShopsFut
        } yield {
          Ok(mart.martShowTpl(mmart, martShops, ownerCompanyOpt))
        }

      case None => martNotFound(martId)
    }
  }

  private def martNotFound(martId: MartId_t) = NotFound("Mart not found: " + martId)

  /** Рендер страницы с формой редактирования торгового центра. */
  def martEditForm(mart_id: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(mart_id) map {
      case Some(mmart) =>
        val form = martFormM fill mmart
        Ok(mart.martEditFormTpl(mmart, form))

      case None => martNotFound(mart_id)
    }
  }

  /** Сабмит формы редактирования торгового центра. */
  def martEditFormSubmit(martId: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(martId) flatMap {
      case Some(mmart) =>
        martFormM.bindFromRequest().fold(
          {formWithErrors =>
            NotAcceptable(mart.martEditFormTpl(mmart, formWithErrors))
          },
          {mmart2 =>
            mmart.name = mmart2.name
            mmart.town = mmart2.town
            mmart.address = mmart2.address
            mmart.siteUrl = mmart2.siteUrl
            mmart.phone = mmart2.phone
            mmart.color = mmart2.color
            mmart.settings.supL1MaxAdsShown = mmart2.settings.supL1MaxAdsShown
            mmart.save map { _martId =>
              Redirect(routes.SysMarket.martShow(_martId))
            }
          }
        )

      case None => martNotFound(martId)
    }
  }

  /** Удалить торговый центр из системы. */
  def martDeleteSubmit(martId: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.deleteById(martId) map {
      case false => martNotFound(martId)
      case true =>
        Redirect(routes.SysMarket.martsList())
          .flashing("success" -> s"Mart $martId deleted.")
    }
  }

  // Инвайты на управление ТЦ

  val martInviteFormM = Form(
    "email" -> email
  )

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def martInviteForm(martId: MartId_t) = IsSuperuser.async { implicit request =>
    val eActsFut = EmailActivation.findByKey(martId)
    MMart.getById(martId) flatMap {
      case Some(mmart) =>
        eActsFut map { eActs =>
          Ok(mart.martInviteFormTpl(mmart, martInviteFormM, eActs))
        }
      case None => martNotFound(martId)
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def martInviteFormSubmit(martId: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(martId) flatMap {
      case Some(mmart) =>
        martInviteFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"martInviteFormSubmit($martId): Failed to bind form: ${formWithErrors.errors}")
            EmailActivation.findByKey(martId) map { eActs =>
              NotAcceptable(mart.martInviteFormTpl(mmart, formWithErrors, eActs))
            }
          },
          {email1 =>
            val eAct = EmailActivation(email=email1, key = martId)
            eAct.save.map { eActId =>
              eAct.id = Some(eActId)
              // Собираем и отправляем письмо адресату
              val mail = use[MailerPlugin].email
              mail.setSubject("Suggest.io | Ваш торговый центр")
              mail.setFrom("no-reply@suggest.io")
              mail.setRecipient(email1)
              val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
              mail.send(
                bodyText = views.txt.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx),
                bodyHtml = views.html.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx)
              )
              // Письмо отправлено, вернуть админа назад в магазин
              Redirect(routes.SysMarket.martShow(martId))
                .flashing("success" -> ("Письмо с приглашением отправлено на " + email1))
            }
          }
        )

      case None => martNotFound(martId)
    }
  }


  /* Магазины (арендаторы ТЦ). */

  /** Выдать страницу со списком всех магазинов в порядке их создания. */
  def shopsList = IsSuperuser.async { implicit request =>
    val mcsFut = allCompaniesMap
    val mmsFut = allMartsMap
    for {
      shops <- MShop.getAll
      mcs   <- mcsFut
      mms   <- mmsFut
    } yield {
      Ok(shop.shopsListTpl(shops, marts=Some(mms), companies=Some(mcs)))
    }
  }

  /** Форма добавления/редактирования магазина. */
  val shopFormM = Form(mapping(
    "name"         -> shopNameM,
    "mart_id"      -> optional(esIdM),
    "company_id"   -> esIdM,
    "description"  -> publishedTextOptM,
    "mart_floor"   -> optional(martFloorM),
    "mart_section" -> optional(martSectionM),
    "l3maxAds"     -> default(number(min=0, max=30), MShopSettings.MAX_LSHOP_ADS)
  )
  // apply()
  {(name, martId, companyId, description, martFloor, martSection, l3maxAds) =>
    MShop(
      name = name,
      martId = martId,
      companyId = companyId,
      description = description,
      martFloor = martFloor,
      martSection = martSection,
      personIds = null,
      settings = MShopSettings(supLShopMaxAdsShown = l3maxAds)
    )
  }
  // unapply()
  {mshop =>
    import mshop._
    Some((name, martId, companyId, description, martFloor, martSection, settings.supLShopMaxAdsShown))
  })


  private def getAllCompaniesAndMarts = {
    val companiesFut = MCompany.getAll
    for {
      marts <- MMart.getAll
      companies <- companiesFut
    } yield (companies, marts)
  }

  /** Рендер страницы добавления нового магазина. */
  def shopAddForm = IsSuperuser.async { implicit request =>
    getAllCompaniesAndMarts map { case (companies, marts) =>
      Ok(shop.shopAddFormTpl(shopFormM, companies, marts))
    }
  }

  /** Сабмит формы добавления нового магазина. */
  def shopAddFormSubmit = IsSuperuser.async { implicit request =>
    shopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopAddFormSubmit(): " + formWithErrors.errors)
        getAllCompaniesAndMarts map { case (companies, marts) =>
          NotAcceptable(shop.shopAddFormTpl(formWithErrors, companies, marts))
        }
      },
      {mshop =>
        mshop.personIds = List(request.pwOpt.get.personId)
        mshop.save map { mshopSavedId =>
          Redirect(routes.SysMarket.shopShow(mshopSavedId))
        }
      }
    )
  }

  /** Рендер страницы, содержащей информацию по указанному магазину. */
  def shopShow(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    MShop.getById(shop_id) flatMap {
      case Some(mshop) =>
        val splsFut = mshop.priceLists
        val martOptFut = mshop.mart
        for {
          ownerOpt <- mshop.company
          spls     <- splsFut
          mmartOpt <- martOptFut
        } yield {
          Ok(shop.shopShowTpl(mshop, spls, ownerOpt, mmartOpt))
        }

      case None => shopNotFound(shop_id)
    }
  }

  /** Рендер ошибки, если магазин не найден в базе. */
  private def shopNotFound(shop_id: ShopId_t) = NotFound("Shop not found: " + shop_id)

  /** Отрендерить страницу с формой редактирования магазина. */
  def shopEditForm(shop_id: ShopId_t) = IsShopAdm(shop_id).async { implicit request =>
    getAllCompaniesAndMarts map { case (companies, marts) =>
      import request.mshop
      val form = shopFormM.fill(mshop)
      Ok(shop.shopEditFormTpl(mshop, form, companies, marts))
    }
  }

  /** Сабмит формы редактирования магазина. */
  def shopEditFormSubmit(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    MShop.getById(shop_id) flatMap {
      case Some(mshop) =>
        shopFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"shopEditFormSubmit($shop_id): form bind failed: " + formWithErrors.errors)
            getAllCompaniesAndMarts map { case (companies, marts) =>
              NotAcceptable(shop.shopEditFormTpl(mshop, formWithErrors, companies, marts))
            }
          },
          {newShop =>
            mshop.loadStringsFrom(newShop)
            mshop.save map { _ =>
              Redirect(routes.SysMarket.shopShow(shop_id))
                .flashing("success" -> "Changes saved.")
            }
          }
        )

      case None => shopNotFound(shop_id)
    }
  }

  /** Админ нажал кнопку удаления магазина. Сделать это. */
  def shopDeleteSubmit(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    MShop.deleteById(shop_id) map {
      case true => Redirect(routes.SysMarket.shopsList())
        .flashing("success" -> "Shop deleted")
      case false => shopNotFound(shop_id)
    }
  }


  /* Ссылки на прайс-листы магазинов, а именно их изменение. */

  /** Маппинг для формы добавления/редактирования ссылок на прайс-листы. */
  val splFormM = Form(mapping(
    "url"       -> urlStrMapper,
    "username"  -> optional(text(maxLength = 64)),
    "password"  -> optional(text(maxLength = 64))
  )
  // apply()
  {(url, usernameOpt, passwordOpt) =>
    val auth_info = if (usernameOpt.isDefined) {
      // TODO Убрать второй UserNamePw и использовать датум?
      Some(UsernamePw(usernameOpt.get, password=passwordOpt.getOrElse("")))
    } else {
      None
    }
    url -> auth_info
  }
  // unapply()
  {case (url, auth_info) =>
    Some(url, auth_info.map(_.username), auth_info.map(_.password))
  })


  /** Рендер формы добавления ссылки на прайс-лист к магазину. */
  def splAddForm(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    MShop.getById(shop_id) map {
      case Some(mshop) =>
        Ok(shop.pricelist.splAddFormTpl(mshop, splFormM))

      case None => shopNotFound(shop_id)
    }
  }

  /** Сабмит формы добавления прайс-листа. */
  def splAddFormSubmit(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    splFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"splAddFormSubmit($shop_id): form bind failed: " + formWithErrors.errors)
        MShop.getById(shop_id) map {
          case Some(mshop) => NotAcceptable(shop.pricelist.splAddFormTpl(mshop, formWithErrors))
          case None => shopNotFound(shop_id)
        }
      },
      {case (url, auth_info) =>
        MShopPriceList(shopId=shop_id, url=url, authInfo=auth_info).save map { mspl =>
          Redirect(routes.SysMarket.shopShow(shop_id))
           .flashing("success" -> "Pricelist added.")
        }
      }
    )
  }

  /** Удалить ранее созданный прайс лист по его id. */
  def splDeleteSubmit(spl_id: String) = IsSuperuser.async { implicit request =>
    MShopPriceList.getById(spl_id) map {
      case Some(mspl) =>
        mspl.delete onFailure {
          case ex => error("Unable to delete MSPL id=" + spl_id, ex)
        }
        Redirect(routes.SysMarket.shopShow(mspl.shopId))

      case None => NotFound("No such shop pricelist with id = " + spl_id)
    }
  }


  /** Отображение категорий яндекс-маркета */
  def showYmCats = IsSuperuser.async { implicit request =>
    MYmCategory.getAllTree.map { cats =>
      Ok(cat.ymCatsTpl(cats))
    }
  }

  /** Полный сброс дерева категорий YM. */
  def resetYmCatsSubmit = IsSuperuser.async { implicit request =>
    // TODO WARNING DANGER ACHTUNG Эту функцию надо выпилить после запуска.
    warn("Resetting MYmCategories...")
    MYmCategory.resetMapping map { _ =>
      Redirect(routes.SysMarket.showYmCats())
        .flashing("success" -> "Все категории удалены.")
    }
  }

  /** Импорт дерева категорий из [[io.suggest.ym.cat.YmCategory.CAT_TREE]]. */
  def importYmCatsSubmit = IsSuperuser.async { implicit request =>
    // TODO WARNING DANGER ACHTUNG Эту функцию надо выпилить после запуска.
    warn("Inserting categories into MYmCategories...")
    MYmCategory.insertYmCats.map { _ =>
      Redirect(routes.SysMarket.showYmCats())
        .flashing("succes" -> "Импорт сделан.")
    }
  }

  // ======================================================================
  // inx2

  /** Имитация действий системы в IndicesUtil при добавлении нового ТЦ (без реального добавления. ТЦ уже добавлен). */
  def inx2handleMartAdd(martId: MartId_t) = IsSuperuser.async { implicit request =>
    IndicesUtil.handleMartAdd(martId) map { inx2 =>
      Ok("OK: " + inx2)
    }
  }

  /** Имитация действий системы в IndicesUtil при удалении указанного ТЦ (без реального удаления). */
  def inx2handleMartDelete(martId: MartId_t) = IsSuperuser.async { implicit request =>
    IndicesUtil.handleMartDelete(martId) map { _ =>
      Ok("Deleted ok.")
    }
  }


  // ======================================================================
  // отладка email-сообщений

  /** Отобразить html/txt email-сообщение активации без отправки куда-либо чего-либо. Нужно для отладки. */
  def showShopEmailActMsgHtml(shopId: ShopId_t, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    for {
      mshop <- MShop.getById(shopId).map(_.get)
      mmart <- MMart.getById(mshop.martId.get).map(_.get)
    } yield {
      val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
      if (isHtml)
        Ok(lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct))
      else
        Ok(views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct) : String)
    }
  }


  /** Отобразить технический список реклам магазина. */
  def showShopAds(shopId: ShopId_t) = IsSuperuser.async { implicit request =>
    val madsFut = MMartAd.findForShop(shopId)
    val adFreqsFut = MAdStat.findAdByActionFreqs(shopId)
    for {
      mshopOpt <- MShop.getById(shopId)
      adFreqs  <- adFreqsFut
      mads     <- madsFut
    } yield {
      Ok(shop.shopAdsTpl(mads, mshopOpt.get, adFreqs))
    }
  }

  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    MMartAd.getById(adId) flatMap {
      case Some(mad) =>
        val mmartFut = MMart.getById(mad.martId)
        for {
          mshopOpt <- MShop.getById(mad.shopId.get)
          mmartOpt <- mmartFut
        } yield {
          val reason = "Причина отключения ТЕСТ причина отключения 123123 ТЕСТ причина отключения."
          if (isHtml) {
            Ok(views.html.market.lk.shop.ad.emailAdDisabledByMartTpl(mmartOpt.get, mshopOpt.get, mad, reason))
          } else {
            Ok(views.txt.market.lk.shop.ad.emailAdDisabledByMartTpl(mmartOpt.get, mshopOpt.get, mad, reason): String)
          }
        }

      case None => NotFound("ad not found: " + adId)
    }
  }

  /** Отрендериить тела email-сообщений инвайта передачи прав на ТЦ. */
  def showMartEmailInvite(martId: MartId_t, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    MMart.getById(martId) map {
      case Some(mmart) =>
        val eAct = EmailActivation("asdasd@kde.org", key=martId, id = Some("123123asdasd_-123"))
        val ctx = implicitly[Context]
        if (isHtml)
          Ok(views.html.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx))
        else
          Ok(views.txt.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx) : String)

      case None => martNotFound(martId)
    }
  }

}

