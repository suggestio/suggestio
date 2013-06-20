package controllers

import play.api.mvc.Controller
import play.api.data._
import util.FormUtil._
import _root_.util._
import io.suggest.util.UrlUtil
import models.{MDomainUserSettings, MDomain, MDomainQi}
import play.api.Play.current
import play.api.libs.json._
import views.html.js._
import play.api.templates.Html
import io.suggest.event.SioNotifier
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsString
import io.suggest.event.subscriber.SnActorRefSubscriber
import scala.Some
import play.api.libs.json.JsObject
import scala.concurrent.Future


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
   * @param domainStr домен. Обычно нормализованный dkey, но лучше нормализовать ещё раз.
   * @param qi_id [a-z0-9] последовательность, описывающая юзера, который устанавливал этот js.
   * @return Скрипт, который сделает всё круто.
   */
  def v2(domainStr:String, qi_id:String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    val dkey = UrlUtil.normalizeHostname(domainStr)
    // Найти домен в базе. Если его там нет, то надо запустить инсталлер и вернуть скрипт с инсталлером. Затем выполнить остальные действия из sioweb_js_controller.
    MDomain.getForDkey(dkey) match {
      // Есть домен такой в базе. Нужно выдать js-скрипт для поиска, т.е. всё как обычно.
      case Some(domain) =>
        // Отрендерить js
        val respBody = jsMainTpl(
          dkey  = dkey,
          qi_id = qi_id,
          isInstall = false,
          uSettings = MDomainUserSettings.getForDkeyAsync(dkey),
          isSiteAdmin = false // TODO определять бы по базе
        )
        replyJs(respBody)


      // Запрос скрипта для неизвестного сайта. ВОЗМОЖНО теперь там установлен скрипт.
      // Нужно проверить, относится ли запрос к юзеру, которому принадлежит qi и есть ли реально скрипт на сайте.
      case None =>
        val isQi = DomainQi.isQi(dkey, qi_id)
        val futureResult = isQi match {
          // Да, это тот юзер, который запросил код на главной. Он устанавил скрипт на сайт или просто прошел по ссылки от скрипта.
          // Теперь нужно запустить проверку домена на наличие этого скрипта и вернуть скрипт с инсталлером.
          case true =>
            // Далее запрос становится асинхронным, т.к. ensureActorFor возвращает Future[ActorRef]. Можно блокироваться через EnsureSync, можно сгенерить фьючерс. Пока делаем второе.
            // Запустить очередь приема новостей, присоединив её к sio_notifier. Когда откроется ws-соединение, очередь будет выпилена, и коннект между sn и каналом ws будет уже прямой.
            NewsQueue4Play.ensureActorFor(dkey, qi_id).map { queueActorRef =>
              // Подписаться на события qi от sio_notifier
              SioNotifier.subscribe(
                subscriber = SnActorRefSubscriber(queueActorRef),
                classifier = QiEventUtil.getClassifier(dkeyOpt = Some(dkey), qiIdOpt=Some(qi_id))
              )
              // Отправить request referer на проверку. Бывает, что юзер ставит поиск не на главной, а где-то сбоку.
              request.headers.get(REFERER).foreach { referer =>
                DomainQi.maybeCheckQiAsync(dkey=dkey, maybeUrl=referer, qi_id=qi_id)
              }
              // Сгенерить ответ.
              val respBody = jsMainTpl(
                dkey = dkey,
                qi_id = qi_id,
                isInstall = true,
                uSettings = MDomainUserSettings.empty(dkey),
                isSiteAdmin = false
              )
              replyJs(respBody)

            // Перехватывать ошибки предыдущих фьючерсов
            } recover {
              case ex: Throwable =>
                // TODO Нужно вернуть юзеру скрипт, который отобразит ему внутреннюю ошибку сервера suggest.io.
                InternalServerError("Internal server error")
            }

          // Кто-то зашел на ещё-не-установленный-сайт. Скрипт поиска выдавать смысла нет. Возвращаем уже готовый фьючерс, пригодный для дальнейшего комбинирования.
          case false =>
            Future.successful {
              ServiceUnavailable("Search not properly installed. If site owner is you, please visit https://suggest.io/ and proceed installation steps.")
                .withHeaders(RETRY_AFTER -> "5")
            }
        }
        // Добавить фоновую проверку во фьючерс результата
        val futureResult1 = futureResult andThen { case _ =>
          // отправить ссылку на корень сайта на проверку, вдруг там действительно по-тихому установлен скрипт.
          DomainQi.checkQiAsync(
            dkey = dkey,
            url  = "http://" + dkey + "/",
            qiIdOpt = if(isQi) Some(qi_id) else None
          )
        }
        // наконец вернуть ещё не готовый, но уже результат
        Async(futureResult1)
    }
  }


  /**
   * Скрипт инсталлера отправил нам содержимое window.location. Возможно, его стоит отправить в очередь на онализ.
   * @param domain домен, заявленный клиентом.
   * @param qi_id qi_id, заявленное клиентом.
   * @return Возвращает различные коды ошибок с сообщениями.
   */
  def installUrl(domain:String, qi_id:String) = {
    addDomainFormM.bindFromRequest().fold(
      {formWithErrors => NotAcceptable("Not a URL.")}
      ,
      // Действительно пришла ссылка. Нужно проверить присланные domain и qi_id и отправить ссылку на проверку.
      {url =>
        val dkey = UrlUtil.normalizeHostname(domain)
        if (DomainQi.isQi(dkey, qi_id)) {
          if (DomainQi.maybeCheckQiAsync(dkey=dkey, maybeUrl = url.toExternalForm, qi_id=qi_id))
            Ok("Ok, your URL will be checked.")
          else
            Forbidden("Unexpected 3rd-party URL.")

        } else {
          Forbidden("Installer is not running for %s. Trying to cheat me? Well, go on." format domain)
        }
      }
    )
  }


  private def replyJs(respBody:Html) = Ok(respBody).as("text/javascript")

}
