package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.{IsSuperuserAdnNode, IsShopAdm, IsSuperuser}
import models._
import views.html.sys1.market._
import views.html.market.lk
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.ym.model.UsernamePw
import MCompany.CompanyId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.Context
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current
import io.suggest.ym.ad.ShowLevelsUtil
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
object SysMarket extends SioController with MacroLogsImpl with ShopMartCompat {

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
    MCompany.getAll().map { allCompanies =>
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
    val companyAdnmsFut = MAdnNode.findByCompanyId(companyId)
      .map { _.groupBy(_.adn.memberType) }
    MCompany.getById(companyId) flatMap {
      case Some(mc) =>
        for {
          adnms <- companyAdnmsFut
        } yield {
          Ok(company.companyShowTpl(mc, adnms))
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
  private def allCompaniesMap = MCompany.getAll().map {
    _.map { mc => mc.id.get -> mc }.toMap
  }

  /* Унифицированные узлы ADN */
  import views.html.sys1.market.adn._

  def adnNodesList = IsSuperuser.async { implicit request =>
    val adnNodesFut = MAdnNode.getAll(maxResults = 1000)
    val companiesFut = MCompany.getAll(maxResults = 1000)
      .map { _.map {c => c.id.get -> c }.toMap }
    for {
      adnNodes <- adnNodesFut
      companies <- companiesFut
    } yield {
      Ok(adnNodesListTpl(adnNodes, Some(companies)))
    }
  }

  def showAdnNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val slavesFut = MAdnNode.findBySupId(adnId)
    val companyOptFut = MCompany.getById(adnNode.companyId)
    for {
      slaves <- slavesFut
      companyOpt <- companyOptFut
    } yield {
      Ok(adnNodeShowTpl(adnNode, slaves, companyOpt))
    }
  }

  def deleteAdnNodeSubmit(adnId: String) = IsSuperuser.async { implicit request =>
    MAdnNode.deleteById(adnId) map {
      case true =>
        Redirect(routes.SysMarket.shopsList())
          .flashing("success" -> "Shop deleted")
      case false => NotFound("ADN node not found: " + adnId)
    }
  }


  /* Торговые центры и площади. */

  /** Рендер страницы со списком торговых центров. */
  def martsList = IsSuperuser.async { implicit request =>
    val allCompaniesMapFut = allCompaniesMap
    for {
      allMarts  <- MAdnNode.findAllByType(AdNetMemberTypes.MART)
      companies <- allCompaniesMapFut
    } yield {
      Ok(mart.martsListTpl(allMarts, companies=Some(companies)))
    }
  }


  val martMetaM = mapping(
    "name"      -> nameM,
    "town"      -> townM,
    "address"   -> martAddressM,
    "siteUrl"   -> optional(urlStrMapper),
    "color"     -> optional(colorM),
    "phone"     -> optional(phoneM)
  )
  {(name, town, address, siteUrlOpt, colorOpt, phoneOpt) =>
    AdnMMetadata(
      name    = name,
      town    = Some(town),
      address = Some(address),
      siteUrl = siteUrlOpt,
      phone   = phoneOpt,
      color   = colorOpt
    )
  }
  {meta =>
    import meta._
    Some((name, town.getOrElse(""), address.getOrElse(""), siteUrl, color, phone))
  }

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(tuple(
    "meta"   -> martMetaM,
    "isEnabled" -> boolean,
    "maxAds" -> default(number(min = 0, max = 30), ShowLevelsUtil.MART_LVL_IN_START_PAGE_DFLT)
  ))


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
      {case (mmartMeta, isEnabled, maxAds) =>
        val mmart = MAdnNode(
          companyId = companyId,
          personIds = Set.empty,
          meta = mmartMeta,
          adn = {
            val _adn = AdNetMemberTypes.MART.getAdnInfoDflt
            _adn.isEnabled = isEnabled
            _adn
          }
        )
        mmart.save map { mmartSavedId =>
          Redirect(routes.SysMarket.martShow(mmartSavedId))
        }
      }
    )
  }

  /** Отображение одного ТЦ. */
  def martShow(martId: String) = IsSuperuser.async { implicit request =>
    getMartById(martId) flatMap {
      case Some(mmart) =>
        val martShopsFut = MAdnNode.findBySupId(martId)
        for {
          ownerCompanyOpt <- mmart.company
          martShops       <- martShopsFut
        } yield {
          Ok(mart.martShowTpl(mmart, martShops, ownerCompanyOpt))
        }

      case None => martNotFound(martId)
    }
  }

  private def martNotFound(martId: String) = NotFound("Mart not found: " + martId)

  /** Рендер страницы с формой редактирования торгового центра. */
  def martEditForm(mart_id: String) = IsSuperuser.async { implicit request =>
    getMartById(mart_id) map {
      case Some(mmart) =>
        val maxOwnAdsOnStartPage = mmart.adn.showLevelsInfo.maxOutAtLevel(AdShowLevels.LVL_START_PAGE)
        val form = martFormM.fill((mmart.meta, mmart.adn.isEnabled, maxOwnAdsOnStartPage))
        Ok(mart.martEditFormTpl(mmart, form))

      case None => martNotFound(mart_id)
    }
  }

  /** Сабмит формы редактирования торгового центра. */
  def martEditFormSubmit(martId: String) = IsSuperuser.async { implicit request =>
    getMartById(martId) flatMap {
      case Some(mmart) =>
        martFormM.bindFromRequest().fold(
          {formWithErrors =>
            NotAcceptable(mart.martEditFormTpl(mmart, formWithErrors))
          },
          {case (martMeta, isEnabled, maxOwnAdsOnStartPage) =>
            mmart.meta.name = martMeta.name
            mmart.meta.town = martMeta.town
            mmart.meta.address = martMeta.address
            mmart.meta.siteUrl = martMeta.siteUrl
            mmart.meta.color = martMeta.color
            mmart.meta.phone = martMeta.phone
            mmart.adn.showLevelsInfo.setMaxOutAtLevel(AdShowLevels.LVL_START_PAGE, maxOwnAdsOnStartPage)
            mmart.adn.isEnabled = isEnabled
            mmart.save map { _martId =>
              Redirect(routes.SysMarket.martShow(_martId))
            }
          }
        )

      case None => martNotFound(martId)
    }
  }

  /** Удалить торговый центр из системы. */
  def martDeleteSubmit(martId: String) = IsSuperuser.async { implicit request =>
    MAdnNode.deleteById(martId) map {
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
  def martInviteForm(martId: String) = IsSuperuser.async { implicit request =>
    val eActsFut = EmailActivation.findByKey(martId)
    getMartById(martId) flatMap {
      case Some(mmart) =>
        eActsFut map { eActs =>
          Ok(mart.martInviteFormTpl(mmart, martInviteFormM, eActs))
        }
      case None => martNotFound(martId)
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def martInviteFormSubmit(martId: String) = IsSuperuser.async { implicit request =>
    getMartById(martId) flatMap {
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
    val mmsFut = MAdnNode.findAllByType(AdNetMemberTypes.MART).map {
      _.map { mmart  =>  mmart.id.get -> mmart }.toMap
    }
    for {
      shops <- MAdnNode.findAllByType(AdNetMemberTypes.SHOP)
      mcs   <- mcsFut
      mms   <- mmsFut
    } yield {
      Ok(shop.shopsListTpl(shops, marts=Some(mms), companies=Some(mcs)))
    }
  }

  val shopMetaM = mapping(
    "name"    -> shopNameM,
    "descr"   -> publishedTextOptM,
    "floor"   -> optional(floorM),
    "section" -> optional(sectionM)
  )
  {(name, descriptionOpt, floorOpt, sectionOpt) =>
    AdnMMetadata(
      name = name,
      description = descriptionOpt,
      floor = floorOpt,
      section = sectionOpt
    )
  }
  {meta =>
    import meta._
    Some((name, description, floor, section))
  }

  /** Форма добавления/редактирования магазина. */
  val shopFormM = Form(mapping(
    "meta"       -> shopMetaM,
    "martId"     -> optional(esIdM),
    "companyId"  -> esIdM,
    "l3maxAds"   -> default(number(min=0, max=30), ShowLevelsUtil.SHOP_LVL_OUT_MEMBER_DLFT)
  )
  // apply()
  {(meta, martIdOpt, companyId, l3maxAds) =>
    val adnShop = AdNetMemberTypes.SHOP.getAdnInfoDflt
    adnShop.supId = martIdOpt
    adnShop.showLevelsInfo.setMaxOutAtLevel(AdShowLevels.LVL_MEMBER, l3maxAds)
    MAdnNode(
      meta = meta,
      companyId = companyId,
      adn = adnShop,
      personIds = Set.empty
    )
  }
  // unapply()
  {mshop =>
    import mshop._
    val l3maxAds = adn.showLevelsInfo.maxOutAtLevel(AdShowLevels.LVL_MEMBER)
    Some((meta, adn.supId, companyId, l3maxAds))
  })


  private def getAllCompaniesAndMarts = {
    val companiesFut = MCompany.getAll()
    for {
      marts     <- MAdnNode.findAllByType(AdNetMemberTypes.MART)
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
        debug(s"shopAddFormSubmit(): " + formatFormErrors(formWithErrors))
        getAllCompaniesAndMarts map { case (companies, marts) =>
          NotAcceptable(shop.shopAddFormTpl(formWithErrors, companies, marts))
        }
      },
      {mshop =>
        mshop.personIds = Set(request.pwOpt.get.personId)
        mshop.save flatMap { mshopSavedId =>
          // Нужно добавить новый магазин в список продьюсеров ТЦ.
          val martUpdateFut: Future[_] = mshop.adn.supId match {
            case Some(martId) => MAdnNodeCache.getByIdCached(martId) flatMap { mmartOpt =>
              val mmart = mmartOpt.get
              mmart.adn.producerIds += mshopSavedId
              mmart.save
            }
            case None => Future successful ()
          }
          martUpdateFut map { _=>
            Redirect(routes.SysMarket.shopShow(mshopSavedId))
          }
        }
      }
    )
  }

  /** Рендер страницы, содержащей информацию по указанному магазину. */
  def shopShow(shopId: String) = IsSuperuser.async { implicit request =>
    getShopById(shopId) flatMap {
      case Some(mshop) =>
        val splsFut = MShopPriceList.getForShop(shopId)
        val martOptFut = mshop.getSup
        for {
          ownerOpt <- mshop.company
          spls     <- splsFut
          mmartOpt <- martOptFut
        } yield {
          Ok(shop.shopShowTpl(mshop, spls, ownerOpt, mmartOpt))
        }

      case None => shopNotFound(shopId)
    }
  }

  /** Рендер ошибки, если магазин не найден в базе. */
  private def shopNotFound(shopId: String) = NotFound("Shop not found: " + shopId)

  /** Отрендерить страницу с формой редактирования магазина. */
  def shopEditForm(shop_id: String) = IsShopAdm(shop_id).async { implicit request =>
    getAllCompaniesAndMarts map { case (companies, marts) =>
      import request.mshop
      val form = shopFormM.fill(mshop)
      Ok(shop.shopEditFormTpl(mshop, form, companies, marts))
    }
  }

  /** Сабмит формы редактирования магазина. */
  def shopEditFormSubmit(shopId: String) = IsSuperuser.async { implicit request =>
    getShopById(shopId) flatMap {
      case Some(mshop) =>
        shopFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"shopEditFormSubmit($shopId): form bind failed: " + formatFormErrors(formWithErrors))
            getAllCompaniesAndMarts map { case (companies, marts) =>
              NotAcceptable(shop.shopEditFormTpl(mshop, formWithErrors, companies, marts))
            }
          },
          {newShop =>
            mshop.meta.name = newShop.meta.name
            mshop.meta.description = newShop.meta.description
            mshop.meta.floor = newShop.meta.floor
            mshop.meta.section = newShop.meta.section
            mshop.adn.supId = newShop.adn.supId
            mshop.companyId = newShop.companyId
            val l3max = newShop.adn.showLevelsInfo.maxOutAtLevel(AdShowLevels.LVL_MEMBER)
            mshop.adn.showLevelsInfo.setMaxOutAtLevel(AdShowLevels.LVL_MEMBER, l3max)
            mshop.save map { _ =>
              Redirect(routes.SysMarket.shopShow(shopId))
                .flashing("success" -> "Changes saved.")
            }
          }
        )

      case None => shopNotFound(shopId)
    }
  }

  /** Админ нажал кнопку удаления магазина. Сделать это. */
  def shopDeleteSubmit(shop_id: String) = IsSuperuser.async { implicit request =>
    // TODO Эта логика повторяет martDeleteSubmit().
    MAdnNode.deleteById(shop_id) map {
      case true =>
        Redirect(routes.SysMarket.shopsList())
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
    val authInfo = usernameOpt.map { username =>
      // TODO Убрать второй UserNamePw и использовать датум?
      UsernamePw(
        username,
        password = passwordOpt.getOrElse("")
      )
    }
    url -> authInfo
  }
  // unapply()
  {case (url, authInfoOpt) =>
    Some((url, authInfoOpt.map(_.username), authInfoOpt.map(_.password)))
  })


  /** Рендер формы добавления ссылки на прайс-лист к магазину. */
  def splAddForm(shopId: String) = IsSuperuser.async { implicit request =>
    getShopById(shopId) map {
      case Some(mshop) =>
        Ok(shop.pricelist.splAddFormTpl(mshop, splFormM))

      case None => shopNotFound(shopId)
    }
  }

  /** Сабмит формы добавления прайс-листа. */
  def splAddFormSubmit(shopId: String) = IsSuperuser.async { implicit request =>
    splFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"splAddFormSubmit($shopId): form bind failed: " + formatFormErrors(formWithErrors))
        getShopById(shopId) map {
          case Some(mshop) => NotAcceptable(shop.pricelist.splAddFormTpl(mshop, formWithErrors))
          case None => shopNotFound(shopId)
        }
      },
      {case (url, authInfo) =>
        MShopPriceList(shopId=shopId, url=url, authInfo=authInfo).save map { mspl =>
          Redirect(routes.SysMarket.shopShow(shopId))
           .flashing("success" -> "Pricelist added.")
        }
      }
    )
  }

  /** Удалить ранее созданный прайс лист по его id. */
  def splDeleteSubmit(splId: String) = IsSuperuser.async { implicit request =>
    MShopPriceList.getById(splId) map {
      case Some(mspl) =>
        mspl.delete onFailure {
          case ex => error("Unable to delete MSPL id=" + splId, ex)
        }
        Redirect(routes.SysMarket.shopShow(mspl.shopId))

      case None => NotFound("No such shop pricelist with id = " + splId)
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
  // отладка email-сообщений

  /** Отобразить html/txt email-сообщение активации без отправки куда-либо чего-либо. Нужно для отладки. */
  def showShopEmailActMsgHtml(shopId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    for {
      mshop <- getShopById(shopId).map(_.get)
      mmart <- getMartById(mshop.adn.supId.get).map(_.get)
    } yield {
      val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
      if (isHtml)
        Ok(lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct))
      else
        Ok(views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct) : String)
    }
  }


  /** Отобразить технический список реклам магазина. */
  def showShopAds(shopId: String) = IsSuperuser.async { implicit request =>
    val madsFut = MAd.findForProducer(shopId)
    val adFreqsFut = MAdStat.findAdByActionFreqs(shopId)
    for {
      mshopOpt <- getShopById(shopId)
      adFreqs  <- adFreqsFut
      mads     <- madsFut
    } yield {
      Ok(shop.shopAdsTpl(mads, mshopOpt.get, adFreqs))
    }
  }

  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        val mmartFut = mad.receivers.headOption match {
          case Some(rcvr) => MAdnNode.getById(rcvr._2.receiverId)
          case None       => MAdnNode.getAll(maxResults = 1).map { _.headOption }
        }
        for {
          mshopOpt <- getShopById(mad.producerId)
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
  def showMartEmailInvite(martId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    getMartById(martId) map {
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


  /** Из-за зоопарка разделов в /sys/ нужно куда-то редиректить админов при наличии adnId.
    * Тут редиректилка, которая отправит админа куда надо на основе adnId. */
  def adnRdr(adnId: String) = IsSuperuser.async { implicit request =>
    MAdnNodeCache.getByIdCached(adnId) map {
      case Some(node) =>
        import AdNetMemberTypes._
        val rdrCall = node.adn.memberType match {
          case MART => routes.SysMarket.martShow(node.id.get)
          case SHOP => routes.SysMarket.shopShow(node.id.get)
        }
        Redirect(rdrCall)

      case None => NotFound("Adn node not found: " + adnId)
    }
  }
}

