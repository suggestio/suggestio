package controllers

import play.api.data._
import play.api.data.Forms._
import util.acl.IsSuperuser
import util.FormUtil._
import views.html.sys1._
import models.MDomain
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:14
 * Description: Административный доступ к системной панели suggest.io. Не для обычных юзеров, а только внутреннее использование.
 * В прошлой версии sioweb был /db/ контроллер для этого. Для экшенов этого контроллера всегда используется isSuperuser.
 */

object Sys extends SioController {

  /** Маппинг формы добавления сайта. */
  val addSiteFormM = Form("domain" -> domain2dkeyMapper)


  /** index.html для системной панели. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }


  /** Список ключей */
  def dkeysListAll = IsSuperuser { implicit request =>
    ???
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


  /** Рендер формы добавления домена. */
  def addSiteForm = IsSuperuser { implicit request =>
    ???
  }

  /** Сабмит формы, отрендеренной в addSiteForm(). */
  def addSiteFormSubmit = IsSuperuser { implicit request =>
    ???
  }

}
