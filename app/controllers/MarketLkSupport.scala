package controllers

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import util.PlayLazyMacroLogsImpl
import util.acl.{IsAuth, IsAdnNodeAdmin}
import views.html.market.lk.support._
import com.typesafe.plugin.{use, MailerPlugin}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.14 14:39
 * Description: Контроллер для обратной связи с техподдержкой s.io в личном кабинете узла.
 */
object MarketLkSupport extends SioController with PlayLazyMacroLogsImpl {

  import LOGGER._


  /** Маппинг для формы обращения в саппорт. */
  private val supportFormM = {
    import play.api.data._, Forms._
    import util.FormUtil._
    Form(tuple(
      "phone" -> phoneOptM,
      "email" -> optional(email),
      "msg"   -> text2048M
    ))
  }


  /** Отрендерить форму с запросом помощи.
    * @return 200 Ок и страница с формой.
    */
  def supportForm(adnIdOpt: Option[String]) = IsAuth.async { implicit request =>
    // Взять дефолтовое значение email'а по сессии
    MPersonIdent.findAllEmails(request.pwOpt.get.personId) map {emailsDflt =>
      val emailDflt = emailsDflt.headOption
      val form = supportFormM.fill( (None, emailDflt, "") )
      Ok(supportFormTpl(adnIdOpt, form))
    }
  }


  /**
   * Сабмит формы обращения за помощью.
   */
  def supportFormSubmit(adnIdOpt: Option[String]) = IsAuth.async { implicit request =>
    ???
  }

}
