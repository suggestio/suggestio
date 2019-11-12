package controllers

import javax.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.i18n.I18nConst
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.Result
import util.FormUtil.uiLangM
import util.acl.MaybeAuth
import util.i18n.JsMessagesUtil
import views.html.lk.lang._
import japgolly.univeq._
import play.api.http.HttpErrorHandler

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 18:17
 * Description: Контроллер управления языками системы.
 * Относится к ЛК, т.к. форма переключения языков сверстана именно там.
 */
@Singleton
class LkLang @Inject() (
                         esModel                         : EsModel,
                         mNodes                          : MNodes,
                         maybeAuth                       : MaybeAuth,
                         jsMessagesUtil                  : JsMessagesUtil,
                         sioControllerApi                : SioControllerApi,
                         errorHandler                    : HttpErrorHandler,
                         csrf                            : Csrf,
                         implicit private val ec         : ExecutionContext,
                       )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import LOGGER._
  import esModel.api._

  private def chooseLangFormM(currLang: Lang): Form[Lang] = {
    Form(
      "lang" -> uiLangM(Some(currLang))
    )
  }


  /** Рендер страницы выбора языка. */
  def showLangSwitcher(r: Option[String]) = csrf.AddToken {
    maybeAuth(U.Lk).async { implicit request =>
      val ctx = implicitly[Context]
      val l0 = ctx.messages.lang
      val langForm = chooseLangFormM(l0).fill(l0)
      _showLangSwitcher(langForm, r, Ok)(ctx)
    }
  }

  private def _showLangSwitcher(langForm: Form[Lang], r: Option[String], rs: Status)(implicit ctx: Context): Future[Result] = {

    val langCodes = mCommonDi.langs
      .availables

    val langCode2msgs = mCommonDi.langs.availables
      .iterator
      .map { lang =>
        val msgs = messagesApi.preferred( lang :: Nil )
        lang.code -> msgs
      }
      .toMap

    val englishLang = langCodes
      .filter(_.language ==* "en")
      .sortBy(_.country ==* "US")
      .headOption
      .getOrElse { Lang.defaultLang }

    val english = messagesApi.preferred( englishLang :: Nil )
    val nodeOpt = None    // TODO Нужно собственную ноду получать из параметра и проверять админские права.

    val html = langChooserTpl(
      english = english,
      lf      = langForm,
      isNowEnglish = ctx.messages.lang.language ==* "en",
      langs   = langCodes.sortBy(_.code),
      nodeOpt = nodeOpt,
      rr      = r,
      langCode2msgs = langCode2msgs
    )(ctx)

    rs(html)
  }


  /** Сабмит формы выбора текущего языка. Нужно выставить язык в куку и текущему юзеру в MPerson. */
  def selectLangSubmit(r: Option[String]) = csrf.Check {
    maybeAuth().async { implicit request =>
      chooseLangFormM( request.messages.lang ).bindFromRequest().fold(
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
          for (ex <- saveUserLangFut.failed)
            error("Failed to save lang for mperson", ex)

          // Сразу возвращаем результат ничего не дожидаясь. Сохранение может занять время, а необходимости ждать его нет.
          RdrBackOr(r)(routes.Ident.rdrUserSomewhere())
            .withLang(newLang)
        }
      )
    }
  }


  /** Сколько секунд кэшировать на клиенте js'ник с локализацией. */
  private val LK_MESSAGES_CACHE_MAX_AGE_SECONDS = if (mCommonDi.isProd) 864000 else 5

  /** 2016.dec.6: Из-за опытов с react.js возникла необходимость использования client-side messages.
    * Тут экшен, раздающий messages для личного кабинета.
    *
    * @param hash PROJECT LAST_MODIFIED hash code.
    * @param langCode Изначальное не проверяется, но для решения проблем с кешированием вбит в адрес ссылки.
    * @return js asset с локализованными мессагами внутрях.
    */
  def lkMessagesJs(langCode: String, hash: Int) = maybeAuth().async { implicit request =>

    // Проверить хеш
    if (hash ==* jsMessagesUtil.hash) {
      val messages = implicitly[Messages]

      // Проверить langCode
      if (messages.lang.code equalsIgnoreCase langCode) {
        val js = jsMessagesUtil.lkJsMsgsFactory( Some(I18nConst.WINDOW_JSMESSAGES_NAME) )(messages)
        Ok(js)
          .withHeaders(CACHE_CONTROL -> ("public, max-age=" + LK_MESSAGES_CACHE_MAX_AGE_SECONDS))

      } else {
        errorHandler.onClientError(request, NOT_FOUND, s"Lang: $langCode")
      }

    } else {
      LOGGER.trace(s"${request.path} hash=$hash must be ${jsMessagesUtil.hash}")
      errorHandler.onClientError(request, NOT_FOUND, s"hash: $hash")
    }
  }

}
