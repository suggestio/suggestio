package controllers

import javax.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.i18n.I18nConst
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.Context
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.Result
import util.FormUtil.uiLangM
import util.acl.{IsSu, MaybeAuth, SioControllerApi}
import util.i18n.JsMessagesUtil
import views.html.lk.lang._
import japgolly.univeq._
import models.req.IReq
import play.api.http.HttpErrorHandler
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 18:17
 * Description: Контроллер управления языками системы.
 * Относится к ЛК, т.к. форма переключения языков сверстана именно там.
 */
final class LkLang @Inject() (
                               sioControllerApi                : SioControllerApi,
                               injector                        : Injector,
                               implicit private val ec         : ExecutionContext,
                             )
  extends MacroLogsImplLazy
{

  // Контроллер НЕ сингтон, то описываем DI для необязательных кусков:
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val jsMessagesUtil = injector.instanceOf[JsMessagesUtil]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val isSu = injector.instanceOf[IsSu]

  import sioControllerApi._

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

    val langCode2msgs = mCommonDi.langs
      .availables
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
          LOGGER.debug("selectLangSubmit(): Failed to bind lang form: \n" + formatFormErrors(formWithErrors))
          _showLangSwitcher(formWithErrors, r, NotAcceptable)
        },
        {newLang =>
          import esModel.api._
          val saveUserLangFut = FutureUtil.optFut2futOpt( request.user.personIdOpt ) { _ =>
            val newLangCode = newLang.code
            for {
              personNodeOpt <- request.user.personNodeOptFut
              personNode = personNodeOpt.get

              res <- mNodes.tryUpdate( personNode ) {
                MNode.meta
                  .composeLens( MMeta.basic )
                  .composeLens( MBasicMeta.langs )
                  .set( newLangCode :: Nil )
              }

            } yield res.id
          }

          // Залоггировать ошибки.
          for (ex <- saveUserLangFut.failed)
            LOGGER.error("Failed to save lang for mperson", ex)

          // Сразу возвращаем результат ничего не дожидаясь. Сохранение может занять время, а необходимости ждать его нет.
          RdrBackOr(r)(routes.Ident.rdrUserSomewhere())
            .withLang(newLang)
        }
      )
    }
  }


  /** Сколько секунд кэшировать на клиенте js'ник с локализацией. */
  private def LK_MESSAGES_CACHE_MAX_AGE_SECONDS = if (mCommonDi.isProd) 864000 else 5


  /** Сборка ответа messages.js */
  private def _messagesResp(msgsInfo: jsMessagesUtil.JsMsgsInfo, langCode: String, hash: Int)
                           (implicit request: IReq[_]): Future[Result] = {
    // Проверить хеш
    if (hash ==* msgsInfo.hash) {
      val messages = implicitly[Messages]

      // Проверить langCode
      if (messages.lang.code equalsIgnoreCase langCode) {
        val js = msgsInfo.jsMessages( Some(I18nConst.WINDOW_JSMESSAGES_NAME) )(messages)
        Ok(js)
          .withHeaders(
            CACHE_CONTROL -> s"public, max-age=$LK_MESSAGES_CACHE_MAX_AGE_SECONDS"
          )

      } else {
        LOGGER.trace(s"${request.path} lang=$langCode must be ${messages.lang.code}")
        errorHandler.onClientError(request, NOT_FOUND, s"Lang: $langCode")
      }

    } else {
      LOGGER.trace(s"${request.path} hash=$hash must be ${msgsInfo.hash}")
      errorHandler.onClientError(request, NOT_FOUND, s"hash: $hash")
    }
  }


  /** 2016.dec.6: Из-за опытов с react.js возникла необходимость использования client-side messages.
    * Тут экшен, раздающий messages для личного кабинета.
    *
    * @param hash Ключ кэширования.
    * @param langCode Изначальное не проверяется, но для решения проблем с кешированием вбит в адрес ссылки.
    * @return js asset с локализованными мессагами внутрях.
    */
  def lkMessagesJs(langCode: String, hash: Int) = maybeAuth().async { implicit request =>
    _messagesResp( jsMessagesUtil.lk, langCode, hash )(request)
  }

  /** Раздача messages для системной админки по аналогии с lkMessagesJs.
    *
    * @param langCode Язык.
    * @param hash Ключ кэшировния.
    * @return messages.js для сисадминки.
    */
  def sysMessagesJs(langCode: String, hash: Int) = isSu().async { implicit request =>
    _messagesResp( jsMessagesUtil.sys, langCode, hash )(request)
  }

}
