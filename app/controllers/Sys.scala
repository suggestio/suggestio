package controllers

import play.api.data._
import play.api.data.Forms._
import util.acl.IsSuperuser
import util.FormUtil._
import views.html.sys1._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import util.{Logs, DomainManager}
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:14
 * Description: Административный доступ к системной панели suggest.io. Не для обычных юзеров, а только внутреннее использование.
 * В прошлой версии sioweb был /db/ контроллер для этого. Для экшенов этого контроллера всегда используется isSuperuser.
 */

object Sys extends SioController with Logs {

  import LOGGER._

  /** Маппинг формы добавления сайта. */
  val addSiteFormM = Form("domain" -> domain2dkeyMapper)


  /** index.html для системной панели. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }


  /** Список всех доменов (всех ключей доменов). */
  def dkeysListAll = IsSuperuser.async { implicit request =>
    MDomain.getAll.map { list =>
      Ok(dkeysListAllTpl(list))
    }
  }


  /** Страница "обзор домена" со общей информацией. */
  def dkeyShow(dkey: String) = IsSuperuser.async { implicit request =>
    MDomain.getForDkey(dkey) flatMap {
      case Some(domain) =>
        domain.allPersonAuthz map { authzList =>
          Ok(dkeyShowTpl(domain, authzList))
        }

      case None => NotFound("No such domain: " + dkey)
    }
  }


  /**
   * Отрендерить страницу поиска по домену.
   * @param dkey Ключ домена.
   * @return Форма для имитации живого поиска на сайте.
   */
  def dkeySearch(dkey: String) = IsSuperuser.async { implicit request =>
    MDomain.getForDkey(dkey: String) map {
      case Some(domain) => Ok(dkeySearchTpl(dkey))
      case None         => NotFound("No such domain: " + dkey)
    }
  }


  /** Рендер формы добавления домена. */
  def addSiteForm = IsSuperuser { implicit request =>
    Ok(addSiteFormTpl(addSiteFormM))
  }

  /** Сабмит формы, отрендеренной в addSiteForm(). */
  def addSiteFormSubmit = IsSuperuser.async { implicit request =>
    lazy val logPrefix = "addSiteFormSubmit(): "
    addSiteFormM.bindFromRequest.fold(
      {formWithErrors =>
        trace(logPrefix + "form parse failed: " + formWithErrors.errors)
        Future successful BadRequest(formWithErrors.errorsAsJson)
      }
      ,
      {dkey =>
        trace(logPrefix + "POST parsed. dkey found = " + dkey)
        val addedBy = request.pwOpt.get.id + " (без проверки)"
        DomainManager.installDkey(dkey=dkey, addedBy=addedBy) map { result =>
          val msg = result match {
            case Some(crawlerRef) => "Domain already in crawler: " + crawlerRef
            case None             => "Crawler successfully notified about new domain."
          }
          info(logPrefix + msg)
          Ok(addSiteSuccessTpl(dkey, result.map(_.toString())))
        }
      }
    )
  }


  /** Удаление домена из системы. */
  def dkeyDelete(dkey: String) = IsSuperuser { implicit request =>
    ???
  }
}
