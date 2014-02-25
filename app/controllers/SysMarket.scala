package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.{IsMartShopAdmin, IsSuperuser}
import models._
import views.html.sys1.market._
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.ym.model.{UsernamePw, MCompany}
import MShop.ShopId_t, MMart.MartId_t, MCompany.CompanyId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client

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
    "name" -> nonEmptyText(maxLength = 64)
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
  val martFormM = Form(tuple(
    "name" -> nonEmptyText(maxLength = 64)
      .transform(strTrimF, strIdentityF),
    "address" -> nonEmptyText(minLength = 10, maxLength = 128)
      .transform(strTrimF, strIdentityF),
    "site_url" -> optional(urlStrMapper)
  ))

  /** Рендер страницы с формой добавления торгового центра. */
  def martAddForm(company_id: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.isExist(company_id) map {
      case true  => Ok(mart.martAddFormTpl(company_id, martFormM))
      case false => companyNotFound(company_id)
    }
  }

  /** Сабмит формы добавления торгового центра. */
  def martAddFormSubmit(company_id: CompanyId_t) = IsSuperuser.async { implicit request =>
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        NotAcceptable(mart.martAddFormTpl(company_id, formWithErrors))
      },
      {case (name, address, site_url) =>
        val mmart = MMart(name=name, company_id=company_id, address=address, site_url=site_url)
        mmart.save map { mmartSavedId =>
          Redirect(routes.SysMarket.martShow(mmartSavedId))
        }
      }
    )
  }

  /** Отображение одного ТЦ. */
  def martShow(mart_id: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(mart_id) flatMap {
      case Some(mmart) =>
        val martShopsFut = MShop.getByMartId(mart_id)
        for {
          ownerCompanyOpt <- mmart.company
          martShops       <- martShopsFut
        } yield {
          Ok(mart.martShowTpl(mmart, martShops, ownerCompanyOpt))
        }

      case None => martNotFound(mart_id)
    }
  }

  private def martNotFound(mart_id: MartId_t) = NotFound("Mart not found: " + mart_id)

  /** Рендер страницы с формой редактирования торгового центра. */
  def martEditForm(mart_id: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(mart_id) map {
      case Some(mmart) =>
        val form = martFormM.fill((mmart.name, mmart.address, mmart.site_url))
        Ok(mart.martEditFormTpl(mmart, form))

      case None => martNotFound(mart_id)
    }
  }

  /** Сабмит формы редактирования торгового центра. */
  def martEditFormSubmit(mart_id: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.getById(mart_id) flatMap {
      case Some(mmart) =>
        martFormM.bindFromRequest().fold(
          {formWithErrors =>
            NotAcceptable(mart.martEditFormTpl(mmart, formWithErrors))
          },
          {case (name, address, site_url) =>
            mmart.name = name
            mmart.address = address
            mmart.site_url = site_url
            mmart.save map { _martId =>
              Redirect(routes.SysMarket.martShow(_martId))
            }
          }
        )

      case None => martNotFound(mart_id)
    }
  }

  /** Удалить торговый центр из системы. */
  def martDeleteSubmit(mart_id: MartId_t) = IsSuperuser.async { implicit request =>
    MMart.deleteById(mart_id) map {
      case false => martNotFound(mart_id)
      case true =>
        Redirect(routes.SysMarket.martsList())
          .flashing("success" -> s"Mart $mart_id deleted.")
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
    "name"         -> nonEmptyText(minLength = 1, maxLength = 64),
    "mart_id"      -> esIdM,
    "company_id"   -> esIdM,
    "description"  -> optional(text(maxLength = 2048)),
    "mart_floor"   -> optional(number(min = -10, max=200)),
    "mart_section" -> optional(number(min=0, max=200000))
  )
  // apply()
  {(name, mart_id, company_id, description, mart_floor, mart_section) =>
    MShop(name=name, mart_id=mart_id, company_id=company_id, description=description, mart_floor=mart_floor, mart_section=mart_section)
  }
  // unapply()
  {mshop =>
    Some((mshop.name, mshop.mart_id, mshop.company_id, mshop.description, mshop.mart_floor, mshop.mart_section))
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
        getAllCompaniesAndMarts map { case (companies, marts) =>
          NotAcceptable(shop.shopAddFormTpl(formWithErrors, companies, marts))
        }
      },
      {mshop =>
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
  def shopEditForm(shop_id: ShopId_t) = IsMartShopAdmin(shop_id).async { implicit request =>
    MShop.getById(shop_id) flatMap {
      case Some(mshop) =>
        getAllCompaniesAndMarts map { case (companies, marts) =>
          val form = shopFormM.fill(mshop)
          Ok(shop.shopEditFormTpl(mshop, form, companies, marts))
        }

      case None => shopNotFound(shop_id)
    }
  }

  /** Сабмит формы редактирования магазина. */
  def shopEditFormSubmit(shop_id: ShopId_t) = IsSuperuser.async { implicit request =>
    MShop.getById(shop_id) flatMap {
      case Some(mshop) =>
        shopFormM.bindFromRequest().fold(
          {formWithErrors =>
            getAllCompaniesAndMarts map { case (companies, marts) =>
              NotAcceptable(shop.shopEditFormTpl(mshop, formWithErrors, companies, marts))
            }
          },
          {newShop =>
            mshop.loadFrom(newShop)
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
        MShop.getById(shop_id) map {
          case Some(mshop) => NotAcceptable(shop.pricelist.splAddFormTpl(mshop, formWithErrors))
          case None => shopNotFound(shop_id)
        }
      },
      {case (url, auth_info) =>
        MShopPriceList(shop_id=shop_id, url=url, auth_info=auth_info).save map { mspl =>
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
        Redirect(routes.SysMarket.shopShow(mspl.shop_id))

      case None => NotFound("No such shop pricelist with id = " + spl_id)
    }
  }

}

