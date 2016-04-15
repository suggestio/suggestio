package controllers

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.node.MNodes
import models.MNode
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.Result
import util.FormUtil.uiLangM
import util.PlayMacroLogsImpl
import util.acl.MaybeAuth
import views.html.lk.lang._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 18:17
 * Description: Контроллер управления языками системы.
 * Относится к ЛК, т.к. форма переключения языков сверстана именно там.
 */
class LkLang @Inject() (
  mNodes                          : MNodes,
  override val mCommonDi          : ICommonDi
)
  extends SioController
  with PlayMacroLogsImpl
  with MaybeAuth
{

  import LOGGER._
  import mCommonDi._

  private def chooseLangFormM(implicit currLang: Lang): Form[Lang] = {
    Form(
      "lang" -> uiLangM(Some(currLang))
    )
  }


  /** Рендер страницы выбора языка. */
  def showLangSwitcher(r: Option[String]) = MaybeAuthGet(U.Lk).async { implicit request =>
    val ctx = implicitly[Context]
    val l0 = ctx.messages.lang
    val langForm = chooseLangFormM(l0).fill(l0)
    _showLangSwitcher(langForm, r, Ok)(ctx)
  }

  private def _showLangSwitcher(langForm: Form[Lang], r: Option[String], rs: Status)(implicit ctx: Context): Future[Result] = {
    val langCodes = langs.availables
      .sortBy(_.code)
    val englishLang = langCodes
      .filter(_.language == "en")
      .sortBy(_.country == "US")
      .headOption
      .getOrElse { Lang.defaultLang }
    val english = ctx.messages.copy(lang = englishLang)
    val nodeOpt = None    // TODO Нужно собственную ноду получать из параметра и проверять админские права.
    val html = langChooserTpl(
      english = english,
      lf      = langForm,
      isNowEnglish = ctx.messages.lang.language == "en",
      langs   = langCodes,
      nodeOpt = nodeOpt,
      rr      = r
    )(ctx)
    rs(html)
  }


  /** Сабмит формы выбора текущего языка. Нужно выставить язык в куку и текущему юзеру в MPerson. */
  def selectLangSubmit(r: Option[String]) = MaybeAuthPost().async { implicit request =>
    chooseLangFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("selectLangSubmit(): Failed to bind lang form: \n" + formatFormErrors(formWithErrors))
        _showLangSwitcher(formWithErrors, r, NotAcceptable)
      },
      {newLang =>
        val saveUserLangFut: Future[_] = {
          FutureUtil.optFut2futOpt( request.user.personIdOpt ) { personId =>
            val newLangCode = newLang.code
            for {
              personNodeOpt <- request.user.personNodeOptFut

              personNode = {
                personNodeOpt.fold [MNode] {
                  warn("User logged in, but not found in MPerson. Creating...")
                  mNodes.applyPerson(
                    lang = newLangCode,
                    id = Some(personId)
                  )
                } { mperson0 =>
                  mperson0.copy(
                    meta = mperson0.meta.copy(
                      basic = mperson0.meta.basic.copy(
                        langs = List(newLangCode)
                      )
                    )
                  )
                }
              }

              id <- mNodes.save(personNode)

            } yield {
              Some(id)
            }
          }
        }

        // Залоггировать ошибки.
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
