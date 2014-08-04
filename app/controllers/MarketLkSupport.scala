package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import util.PlayLazyMacroLogsImpl
import util.acl.{IsAuth, IsAdnNodeAdmin}
import views.html.market.lk.support._

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
    // TODO Брать дефолтовое значение email'а по сессии
    val form = supportFormM
    Ok(supportFormTpl(adnIdOpt, form))
  }


  /**
   * Сабмит формы обращения за помощью.
   */
  def supportFormSubmit(adnIdOpt: Option[String]) = IsAuth.async { implicit request =>
    ???
  }

}
