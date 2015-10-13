package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.node.MNode
import io.suggest.playx.ICurrentApp
import models.Context
import org.elasticsearch.client.Client
import play.api.data.Form
import play.api.i18n.{MessagesApi, Lang}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl.{MaybeAuthGet, MaybeAuthPost}
import views.html.lk.lang._
import util.FormUtil.uiLangM

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 18:17
 * Description: Контроллер управления языками системы.
 * Относится к ЛК, т.к. форма переключения языков сверстана именно там.
 */
class LkLang @Inject() (
  override val messagesApi      : MessagesApi,
  override implicit val current : play.api.Application,
  override implicit val ec      : ExecutionContext,
  implicit val esClient         : Client,
  override implicit val sn      : SioNotifierStaticClientI
)
  extends SioController
  with PlayMacroLogsImpl
  with ICurrentApp
{

  import LOGGER._

  private def chooseLangFormM(implicit currLang: Lang): Form[Lang] = {
    Form(
      "lang" -> uiLangM(Some(currLang))
    )
  }


  /** Рендер страницы выбора языка. */
  def showLangSwitcher(r: Option[String]) = MaybeAuthGet { implicit request =>
    val ctx = implicitly[Context]
    val l0 = ctx.messages.lang
    val langForm = chooseLangFormM(l0).fill(l0)
    Ok( _showLangSwitcher(langForm, r)(ctx) )
  }


  private def _showLangSwitcher(langForm: Form[Lang], r: Option[String])(implicit ctx: Context): Html = {
    val langs = Lang.availables
      .sortBy(_.code)
    val englishLang = langs
      .filter(_.language == "en")
      .sortBy(_.country == "US")
      .headOption
      .getOrElse { Lang.defaultLang }
    val english = ctx.messages.copy(lang = englishLang)
    val nodeOpt = None    // TODO Нужно собственную ноду получать из параметра и проверять админские права.
    langChooserTpl(
      english = english,
      lf      = langForm,
      isNowEnglish = ctx.messages.lang.language == "en",
      langs   = langs,
      nodeOpt = nodeOpt,
      rr      = r
    )(ctx)
  }


  /** Сабмит формы выбора текущего языка. Нужно выставить язык в куку и текущему юзеру в MPerson. */
  def selectLangSubmit(r: Option[String]) = MaybeAuthPost { implicit request =>
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
                  mperson0.copy(
                    meta = mperson0.meta.copy(
                      basic = mperson0.meta.basic.copy(
                        langs = List(newLangCode)
                      )
                    )
                  )
                case None =>
                  warn("User logged in, but not found in MPerson. Creating...")
                  MNode.applyPerson(lang = newLangCode, id = Some(pw.personId))
              }
              .flatMap { _.save }
          case None =>
            Future successful None
        }
        saveUserLangFut onFailure {
          case ex: Throwable  =>  error("Failed to save lang for mperson", ex)
        }
        // Сразу возвращаем результат ничего не дожидаясь. Сохранение может занять время, а необходимости ждать его нет.
        RdrBackOr(r)(routes.Ident.rdrUserSomewhere())
          .withLang(newLang)
      }
    )
  }

}
