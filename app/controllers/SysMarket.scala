package controllers

import play.api.Play.current
import play.api.db.DB
import io.suggest.util.MacroLogsImpl
import util.acl.IsSuperuser
import models._
import views.html.sys1.market._
import play.api.data._, Forms._
import util.FormUtil._
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
object SysMarket extends SioController with MacroLogsImpl {

  /** Маппинг для формы добавления/редактирования компании. */
  val companyFormM = Form(
    "name" -> nonEmptyText(maxLength = 64)
  )

  /** Индексная страница продажной части. Тут ссылки на дальнейшие страницы. */
  def index = IsSuperuser { implicit request =>
    Ok(marketIndexTpl())
  }

  /** Отрендерить sio-админу список всех компаний, зарегистрированных в системе. */
  def companiesList = IsSuperuser { implicit request =>
    val allCompanies = DB.withConnection { implicit c =>
      MCompany.getAll
    }
    val render = company.companiesListTpl(allCompanies)
    Ok(render)
  }

  /** Отрендерить страницу с формой добавления новой компании. */
  def companyAddForm = IsSuperuser { implicit request =>
    Ok(company.companyAddFormTpl(companyFormM))
  }

  /** Самбит формы добавления новой компании. */
  def companyAddFormSubmit = IsSuperuser { implicit request =>
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        NotAcceptable(company.companyAddFormTpl(formWithErrors))
      },
      {name =>
        val company = DB.withConnection { implicit c =>
          MCompany(name).save
        }
        Redirect(routes.SysMarket.companyShow(company.id.get))
      }
    )
  }

  /** Отобразить информацию по указанной компании.
    * @param companyId Числовой id компании.
    */
  def companyShow(companyId: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MCompany.getById(companyId) match {
        case Some(mc) => Ok(company.companyShowTpl(mc))
        case None     => companyNotFound(companyId)
      }
    }
  }


  /** Отрендерить страницу с формой редактирования компании. */
  def companyEditForm(companyId: Int) = IsSuperuser { implicit request =>
    val companyOpt = DB.withConnection { implicit c =>
      MCompany.getById(companyId)
    }
    companyOpt match {
      case Some(mc)  =>
        val form = companyFormM.fill(mc.name)
        Ok(company.companyEditFormTpl(mc, form))

      case None => companyNotFound(companyId)
    }
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(companyId: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MCompany.getById(companyId) match {
        case Some(mc) =>
          companyFormM.bindFromRequest.fold(
            {formWithErrors =>
              NotAcceptable(company.companyEditFormTpl(mc, formWithErrors))
            },
            {name =>
              mc.name = name
              mc.saveUpdate
              Redirect(routes.SysMarket.companyShow(companyId))
            }
          )

        case None => companyNotFound(companyId)
      }
    }
  }

  /** Админ приказал удалить указанную компанию. */
  def companyDeleteSubmit(companyId: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MCompany.deleteById(companyId) match {
        case 1 =>
          Redirect(routes.SysMarket.companiesList())
            .flashing("success" -> s"Company $companyId deleted.")

        case 0 => companyNotFound(companyId)

        case rc => throw new IllegalStateException(s"Too many rows deleted ($rc). Rollback.")
      }
    }
  }


  /** Реакция на ошибку обращения к несуществующей компании. Эта логика расшарена между несколькими экшенами. */
  private def companyNotFound(companyId: Int) = NotFound("Company not found: " + companyId)


  /* Торговые центры и площади. */

  /** Рендер страницы со списком торговых центров. */
  def martsList = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      val allMarts = MMart.getAll
      Ok(mart.martsListTpl(allMarts, withCompany=Some(None)))
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
  def martAddForm(company_id: Int) = IsSuperuser { implicit request =>
    val isCompanyExist = DB.withConnection { implicit c =>
      MCompany.isExists(company_id)
    }
    if (isCompanyExist) {
      Ok(mart.martAddFormTpl(company_id, martFormM))
    } else {
      companyNotFound(company_id)
    }
  }

  /** Сабмит формы добавления торгового центра. */
  def martAddFormSubmit(company_id: Int) = IsSuperuser { implicit request =>
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        NotAcceptable(mart.martAddFormTpl(company_id, formWithErrors))
      },
      {case (name, address, site_url) =>
        val mmart = MMart(name=name, company_id=company_id, address=address, site_url=site_url)
        val mmartSaved = DB.withConnection { implicit c =>
          mmart.save
        }
        Redirect(routes.SysMarket.martShow(mmartSaved.id.get))
      }
    )
  }

  /** Отображение одного ТЦ. */
  def martShow(mart_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MMart.getById(mart_id) match {
        case Some(mmart) => Ok(mart.martShowTpl(mmart))
        case None => martNotFound(mart_id)
      }
    }
  }

  private def martNotFound(mart_id: Int) = NotFound("Mart not found: " + mart_id)

  /** Рендер страницы с формой редактирования торгового центра. */
  def martEditForm(mart_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MMart.getById(mart_id) match {
        case Some(mmart) =>
          val form = martFormM.fill((mmart.name, mmart.address, mmart.site_url))
          Ok(mart.martEditFormTpl(mmart, form))

        case None => martNotFound(mart_id)
      }
    }
  }

  /** Сабмит формы редактирования торгового центра. */
  def martEditFormSubmit(mart_id: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MMart.getById(mart_id) match {
        case Some(mmart) =>
          martFormM.bindFromRequest().fold(
            {formWithErrors =>
              NotAcceptable(mart.martEditFormTpl(mmart, formWithErrors))
            },
            {case (name, address, site_url) =>
              mmart.name = name
              mmart.address = address
              mmart.site_url = site_url
              mmart.saveUpdate
              // Результат saveUpdate не проверяем, т.к. withTransaction().
              Redirect(routes.SysMarket.martShow(mart_id))
            }
          )

        case None => martNotFound(mart_id)
      }
    }
  }

  /** Удалить торговый центр из системы. */
  def martDeleteSubmit(mart_id: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MMart.deleteById(mart_id) match {
        case 0 => martNotFound(mart_id)
        case 1 => Redirect(routes.SysMarket.martsList())
          .flashing("success" -> s"Mart $mart_id deleted.")
        case rc => throw new IllegalStateException(s"Too many rows deleted ($rc) for mart_id=$mart_id. Rollback...")
      }
    }
  }


  /* Магазины (арендаторы ТЦ). */

  /** Выдать страницу со списком всех магазинов в порядке их создания. */
  def shopsList = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      val shops = MShop.getAll
      Ok(shop.shopsListTpl(shops, withMart=Some(None)))
    }
  }

  /** Форма добавления/редактирования магазина. */
  val shopFormM = Form(mapping(
    "name"         -> nonEmptyText(minLength = 1, maxLength = 64),
    "mart_id"      -> number(min=1),
    "company_id"   -> number(min=1),
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


  private def getAllCompaniesAndMarts(implicit c:Connection) = {
    val _companies = MCompany.getAll
    val _marts = MMart.getAll
    (_companies, _marts)
  }

  /** Рендер страницы добавления нового магазина. */
  def shopAddForm = IsSuperuser { implicit request =>
    val (companies, marts) = DB.withConnection { implicit c =>
      getAllCompaniesAndMarts
    }
    Ok(shop.shopAddFormTpl(shopFormM, companies, marts))
  }

  /** Сабмит формы добавления нового магазина. */
  def shopAddFormSubmit = IsSuperuser { implicit request =>
    shopFormM.bindFromRequest().fold(
      {formWithErrors =>
        val (companies, marts) = DB.withConnection { implicit c =>
          getAllCompaniesAndMarts
        }
        NotAcceptable(shop.shopAddFormTpl(formWithErrors, companies, marts))
      },
      {mshop =>
        val mshopSaved = DB.withConnection { implicit c =>
          mshop.save
        }
        Redirect(routes.SysMarket.shopShow(mshopSaved.id.get))
      }
    )
  }

  /** Рендер страницы, содержащей информацию по указанному магазину. */
  def shopShow(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MShop.getById(shop_id) match {
        case Some(mshop)  => Ok(shop.shopShowTpl(mshop))
        case None         => shopNotFound(shop_id)
      }
    }
  }

  /** Рендер ошибки, если магазин не найден в базе. */
  private def shopNotFound(shop_id: Int) = NotFound("Shop not found: " + shop_id)

  /** Отрендерить страницу с формой редактирования магазина. */
  def shopEditForm(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MShop.getById(shop_id) match {
        case Some(mshop) =>
          val (companies, marts) = getAllCompaniesAndMarts
          val form = shopFormM.fill(mshop)
          Ok(shop.shopEditFormTpl(mshop, form, companies, marts))

        case None => shopNotFound(shop_id)
      }
    }
  }

  /** Сабмит формы редактирования магазина. */
  def shopEditFormSubmit(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MShop.getById(shop_id) match {
        case Some(mshop) =>
          shopFormM.bindFromRequest().fold(
            {formWithErrors =>
              val (companies, marts) = getAllCompaniesAndMarts
              NotAcceptable(shop.shopEditFormTpl(mshop, formWithErrors, companies, marts))
            },
            {newShop =>
              mshop.loadFrom(newShop)
              mshop.saveUpdate
              // Не проверяем результат saveUpdate(), т.к. withTransaction().
              Redirect(routes.SysMarket.shopShow(shop_id))
                .flashing("success" -> "Changes saved.")
            }
          )

        case None => shopNotFound(shop_id)
      }
    }
  }

  /** Админ нажал кнопку удаления магазина. Сделать это. */
  def shopDeleteSubmit(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MShop.deleteById(shop_id) match {
        case 1 => Redirect(routes.SysMarket.shopsList())
          .flashing("success" -> "Shop deleted")
        case 0 => shopNotFound(shop_id)
        case rc => throw new IllegalStateException(s"Too many shop rows deleted($rc) for shop_id=$shop_id. Rollback...")
      }
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
  def splAddForm(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      MShop.getById(shop_id) match {
        case Some(mshop) =>
          Ok(shop.pricelist.splAddFormTpl(mshop, splFormM))

        case None => shopNotFound(shop_id)
      }
    }
  }

  /** Сабмит формы добавления прайс-листа. */
  def splAddFormSubmit(shop_id: Int) = IsSuperuser { implicit request =>
    DB.withConnection { implicit c =>
      splFormM.bindFromRequest().fold(
        {formWithErrors =>
          MShop.getById(shop_id) match {
            case Some(mshop) => NotAcceptable(shop.pricelist.splAddFormTpl(mshop, formWithErrors))
            case None => shopNotFound(shop_id)
          }
        },
        {case (url, auth_info) =>
          val mspl = MShopPriceList(shop_id=shop_id, url=url, auth_info=auth_info).save
          Redirect(routes.SysMarket.shopShow(shop_id))
            .flashing("success" -> "Pricelist added.")
        }
      )
    }
  }

  /** Удалить ранее созданный прайс лист по его id. */
  def splDeleteSubmit(spl_id: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MShopPriceList.getById(spl_id) match {
        case Some(mspl) =>
          mspl.delete
          Redirect(routes.SysMarket.shopShow(mspl.shop_id))

        case None => NotFound("No such shop pricelist with id = " + spl_id)
      }
    }
  }

}

