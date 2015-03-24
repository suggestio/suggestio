package controllers

import models.Context
import models.usr.MPerson
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.Result
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl.{AbstractRequestWithPwOpt, MaybeAuthGet, MaybeAuthPost}
import views.html.lk.lang._
import play.api.Play.current
import util.FormUtil.uiLangM
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 18:17
 * Description: Контроллер управления языками системы.
 * Относится к ЛК, т.к. форма переключения языков сверстана именно там.
 */
object LkLang extends SioController with PlayMacroLogsImpl {

  import LOGGER._


  private def chooseLangFormM(implicit currLang: Lang): Form[Lang] = {
    Form(
      "lang" -> uiLangM(Some(currLang))
    )
  }


  /** Рендер страницы выбора языка. */
  def showLangSwitcher(r: Option[String]) = MaybeAuthGet { implicit request =>
    val ctx = implicitly[Context]
    val langForm = chooseLangFormM(ctx.lang).fill(ctx.lang)
    Ok( _showLangSwitcher(langForm, r)(ctx) )
  }


  private def _showLangSwitcher(langForm: Form[Lang], r: Option[String])(implicit ctx: Context): Html = {
    val enCode = "en"
    val english = Lang(enCode)
    val langs = Lang.availables
      .sortBy(_.code)
    val nodeOpt = None    // TODO Нужно собственную ноду получать из параметра и проверять админские права.
    langChooserTpl(
      english = english,
      lf      = langForm,
      isNowEnglish = ctx.langStr == enCode,
      langs   = langs,
      nodeOpt = nodeOpt,
      rr      = r
    )(ctx)
  }

  /** Сабмит формы выбора текущего языка. Нужно выставить язык в куку и текущему юзеру в MPerson. */
  def selectLangSubmit(r: Option[String]) = MaybeAuthPost.async { implicit request =>
    chooseLangFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("selectLangSubmit(): Failed to bind lang form: \n" + formatFormErrors(formWithErrors))
        NotAcceptable( _showLangSwitcher(formWithErrors, r) )
      },
      {newLang =>
        val saveUserLangFut: Future[_] = request.pwOpt match {
          case Some(pw) =>
            val newLangCode = newLang.code
            pw.personOptFut
              .map {
                case Some(mperson0) =>
                  mperson0.copy(lang = newLangCode)
                case None =>
                  warn("User logged in, but not found in MPerson. Creating...")
                  MPerson(lang = newLangCode, id = Some(pw.personId))
              }
              .flatMap { _.save }
          case None =>
            Future successful None
        }
        saveUserLangFut map { _ =>
          RdrBackOr(r)(routes.Ident.rdrUserSomewhere())
            .withLang(newLang)
        }
      }
    )
  }

}
