package controllers

import play.api.Play.current
import play.api.db.DB
import io.suggest.util.MacroLogsImpl
import util.acl.IsSuperuser
import models._
import views.html.sys1.market._
import play.api.data._, Forms._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Все продажно-купле-продажные дела вынесены из Sys-контроллера в этот контроллер.
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
      MCompany.allById
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
        Ok(company.companyEditFormTpl(companyId, form))

      case None => companyNotFound(companyId)
    }
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(companyId: Int) = IsSuperuser { implicit request =>
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        NotAcceptable(company.companyEditFormTpl(companyId, formWithErrors))
      },
      {name =>
        DB.withConnection { implicit c =>
          MCompany.getById(companyId) match {
            case Some(mc) =>
              mc.name = name
              mc.saveUpdate
              Redirect(routes.SysMarket.companyShow(companyId))

            case None => companyNotFound(companyId)
          }
        }
      }
    )
  }

  /** Админ приказал удалить указанную компанию. */
  def companyDeleteSubmit(companyId: Int) = IsSuperuser { implicit request =>
    DB.withTransaction { implicit c =>
      MCompany.deleteById(companyId) match {
        case 1 =>
          Redirect(routes.SysMarket.companiesList())
            .flashing("success" -> s"Company $companyId deleted.")

        case 0 => companyNotFound(companyId)

        case rc => throw new IllegalStateException(s"Too many rows deleteted ($rc). Rollback.")
      }
    }
  }


  private def companyNotFound(companyId: Int) = NotFound("Company not found: " + companyId)

}
