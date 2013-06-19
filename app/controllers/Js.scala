package controllers

import play.api.mvc.Controller
import play.api.data._
import util.FormUtil._
import util.{DomainQi, ContextT, AclT}
import io.suggest.util.UrlUtil
import models.{MDomainUserSettings, MDomain, MDomainQi}
import play.api.Play.current
import play.api.mvc.{Result, AnyContent, Request}
import play.api.libs.json._
import views.html.js._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:09
 * Description: js-контроллер. Отвечает за раздачу suggest.io юзерам и за установку новых сайтов через qi.
 * Инсталлятор сайта запускается в несколько шагов.
 * 1. Проверяется, является ли текущий юзер устанавливальщиком сайта. Через сессию. В ответ отправляется js.
 * 2. JS-инсталлер считывает window.location и POSTит назад на сервер.
 *    На основе полученной ссылки, контроллер формирует список ссылок для верификации и запускает фоновый domain_qi.
 * 3. Одновременно, инсталлер запускает comet и мониторит состояние qi.
 */

object Js extends Controller with AclT with ContextT {

  val SIO_JS_STATIC_FILENAME  = current.configuration.getString("sio_js.filename") getOrElse "sio.search.v7.js"
  val PULL_INSTALLER_CALLBACK = current.configuration.getString("sio_js.installer.callback") getOrElse "sio.qi_events"

  val addDomainFormM = Form("url" -> urlAllowedMapper)

  /**
   * Юзер хочет добавить домен в систему. Нужно вернуть ему js-код.
   * В новой редакции это Stateless-pure-функция, все данные по qi лежат у юзера в сессии в кукисах и нигде более.
   * Иными словами, сабмит нового сайта не меняет состояние системы.
   */
  def addDomain = maybeAuthenticated { implicit pw_opt => implicit request =>
    addDomainFormM.bindFromRequest().fold(
      // Не осилил засабмиченную ссылку. Нужно вернуть json с ошибками.
      {formWithErrors =>
        // Для генерации локализованных сообщений об ошибках, нужно импортировать контекст.
        val ctx = getContext
        import ctx.lang
        // Сгенерить json-ответ
        val respProps = Map[String, JsValue](
          "status" -> JsString("error"),
          "errors" -> formWithErrors.errorsAsJson
        )
        val respJson = JsObject(respProps.toList)
        NotAcceptable(respJson)
      }
      ,
      // Есть ссылка на хост. Нужно взять из неё хостнейм (который, возможно, IDN'утый), нормализовать и загнать в сессию.
      {url =>
        val dkey = UrlUtil.normalizeHostname(url.getHost)
        //val startUrlStr = UrlUtil.normalize(url.toExternalForm)
        val (qi_id, session1opt) = DomainQi.addDomainQiIntoSession(dkey)
        // Сгенерить json ответа
        val resultProps = Map(
          "url"    -> url.toExternalForm,
          "status" -> "ok",
          "domain" -> dkey,
          "js_url" -> routes.Js.v2(dkey, qi_id).url
        ).mapValues(JsString(_))
        val respJson = JsObject(resultProps.toList)
        val resp0 = Ok(respJson)
        // Если сессия не изменилась, то можно её и не возвращать.
        session1opt match {
          case Some(session1) => resp0.withSession(session1)
          case None           => resp0
        }
      }
    )
  }


  /**
   * Запрос скрипта в технологии v2. Этот метод подразумевает запуск инсталлятора.
   * @param domain домен. Обычно нормализованный dkey, но лучше нормализовать ещё раз.
   * @param qi_id [a-z0-9] последовательность, описывающая юзера, который устанавливал этот js.
   * @return Скрипт, который сделает всё круто.
   */
  def v2(domain:String, qi_id:String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    val dkey = UrlUtil.normalizeHostname(domain)
    // TODO Найти домен в базе. Если его там нет, то надо запустить инсталлер. Затем выполнить остальные действия из sioweb_js_controller.
    val respBody = MDomain.getForDkey(dkey) match {
      // Есть домен такой в базе. Нужно выдать js-скрипт для поиска, т.е. как обычно.
      case Some(domain) =>
        // Отрендерить js
        jsMainTpl(
          dkey  = dkey,
          qi_id = qi_id,
          isInstall = false,
          uSettings = MDomainUserSettings.getForDkey(dkey),
          isSiteAdmin = false
        )

      // Запрос скрипта для ещё не установленного сайта. ВОЗМОЖНО теперь там установлен скрипт.
      // Нужно проверить, относится ли запрос к юзеру, которому принадлежит qi и есть ли реально скрипт на сайте.
      case None =>
        serveStaticJs
    }
    ???
  }


  /**
   * Выдать юзеру js статически. Может вызываться в конце экшена.
   * @param request реквест из экшона.
   * @return
   */
  private def serveStaticJs(implicit request:Request[AnyContent]) : Result = {
    controllers.Assets.at("javascripts", SIO_JS_STATIC_FILENAME)(request)
  }

}
