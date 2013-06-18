package controllers

import play.api.mvc.Controller
import play.api.data._
import util.FormUtil._
import util.{ContextT, AclT}
import io.suggest.util.UrlUtil
import models.{MDomain, MDomainQi}
import play.api.Play.current
import play.api.mvc.{Result, AnyContent, Request}

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

  val addDomainFormM = Form("url" -> urlStrMapper)
  """https?://([^/]*\.)?([.\w_-]+)(:\d+)?/activate/(\d+)/(\w+)""".r

  /**
   * Юзер хочет добавить домен в систему. Нужно вернуть ему js-код.
   * В новой редакции это Stateless-pure-функция, все данные по qi лежат у юзера в сессии в кукисах.
   */
  def addDomain = maybeAuthenticated { implicit pw_opt => implicit request =>
    addDomainFormM.bindFromRequest().fold(
      // Не осилил сабмит
      {formWithErrors => NotAcceptable("Invalid URL")}
      ,
      // Есть ссылка на хост. Нужно взять из неё хостнейм (который, возможно, IDN'утый), нормализовать и загнать в сессию.
      {urlStr =>
        // TODO
        Ok
      }
    )
  }

  /**
   * Запрос скрипта в технологии v2. Этот метод подразумевает запуск инсталлятора.
   * @param domain домен. Обычно нормализованный dkey, но лучше нормализовать ещё раз.
   * @param domainQiId [a-z0-9] последовательность, описывающая юзера, который устанавливал этот js.
   * @return Скрипт, который сделает всё круто.
   */
  def v2(domain:String, domainQiId:String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    val dkey = UrlUtil.normalizeHostname(domain)
    // TODO Найти домен в базе. Если его там нет, то надо запустить инсталлер. Затем выполнить остальные действия из sioweb_js_controller.
    MDomain.getForDkey(dkey) match {
      // Есть домен такой в базе. Нужно выдать js-скрипт для поиска, т.е. как обычно.
      case Some(domain) =>

        serveStaticJs

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
