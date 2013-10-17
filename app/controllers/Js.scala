package controllers

import util.event._
import play.api.mvc.{SimpleResult, WebSocket, Controller}
import play.api.data._
import util.FormUtil._
import util.acl._
import _root_.util._
import io.suggest.util.UrlUtil
import models._
import play.api.Play.current
import play.api.libs.json._
import views.txt.js._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsString
import io.suggest.event.subscriber.SnActorRefSubscriber
import scala.Some
import play.api.libs.json.JsObject
import scala.concurrent.Future
import java.util.UUID
import play.api.libs.iteratee.Concurrent


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

object Js extends Controller with ContextT with Logs {

  // TODO удалить, если это более не нужно.
  val SIO_JS_STATIC_FILENAME  = current.configuration.getString("sio_js.filename") getOrElse "sio.search.v7.js"
  val PULL_INSTALLER_CALLBACK = current.configuration.getString("sio_js.installer.callback") getOrElse "sio.qi_events"

  val addDomainFormM = Form("url" -> urlAllowedMapper)

  import LOGGER._

  /**
   * Юзер хочет добавить домен в систему. Нужно вернуть ему js-код.
   * В новой редакции это Stateless-pure-функция, все данные по qi лежат у юзера в сессии в кукисах и нигде более.
   * Иными словами, сабмит нового сайта не меняет состояние системы.
   */
  def addDomain = MaybeAuth { implicit request =>
    lazy val logPrefix = "addDomain(): "
    trace(logPrefix + "starting...")
    addDomainFormM.bindFromRequest().fold(
      // Не осилил засабмиченную ссылку. Нужно вернуть json с ошибками.
      {formWithErrors =>
        debug(logPrefix + "form parse failed: " + formWithErrors.errors)
        // Для генерации локализованных сообщений об ошибках, нужно импортировать контекст.
        // Сгенерить json-ответ
        val respProps = Map[String, JsValue](
          "status" -> JsString("error"),
          "errors" -> formWithErrors.errorsAsJson
        )
        val respJson = JsObject(respProps.toList)
        trace(logPrefix + "reply: NotAcceptable / " + respJson)
        NotAcceptable(respJson)
      }
      ,
      // Есть ссылка на хост. Нужно взять из неё хостнейм (который, возможно, IDN'утый), нормализовать и загнать в сессию.
      {url =>
        trace(logPrefix + "form parsed ok. found URL = " + url.toExternalForm)
        val dkey = UrlUtil.normalizeHostname(url.getHost)
        trace(logPrefix + "dkey = " + dkey)
        //val startUrlStr = UrlUtil.normalize(url.toExternalForm)
        val (qi_id, session1Opt) = DomainQi.addDomainQiIntoSession(dkey)
        trace(logPrefix + s"qi = $qi_id ;; sessionOpt = " + session1Opt)
        // Сгенерить json ответа
        val resultProps = Map(
          "url"    -> url.toExternalForm,
          "status" -> "ok",
          "domain" -> dkey,
          "js_url" -> routes.Js.v2(dkey, qi_id).url
        ).mapValues(JsString)
        val respJson = JsObject(resultProps.toList)
        trace(logPrefix + "reply: 200 Ok / " + respJson)
        val resp0 = Ok(respJson)
        // Если сессия не изменилась, то можно её и не возвращать.
        session1Opt match {
          case Some(session1) => resp0 withSession session1
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
  def v2(domainStr:String, qi_id:String) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"v2($domainStr, $qi_id): "
    trace(logPrefix + "starting...")
    val dkey = UrlUtil.normalizeHostname(domainStr)
    // Найти домен в базе. Если его там нет, то надо запустить инсталлер и вернуть скрипт с инсталлером. Затем выполнить остальные действия из sioweb_js_controller.
    MDomain.getForDkey(dkey) flatMap {
      // Есть домен такой в базе. Нужно выдать js-скрипт для поиска, т.е. всё как обычно.
      case Some(domain) =>
        trace(logPrefix + s"dkey=$dkey found as $domain")
        // Параллельно читаем права юзера на домен и настройки домена. Через "for {} yield {}" подавляем многоэтажность фьючерсов.
        val isSiteAdminFut = IsDomainAdmin.isDkeyAdmin(dkey, request.pwOpt, request)
        for {
          dus           <- MDomainUserSettings.getForDkey(dkey)
          siteAdminOpt  <- isSiteAdminFut
        } yield {
          trace(logPrefix + s"siteAdminOpt = $siteAdminOpt ;; settings = $dus")
          // Отрендерить js
          val respBody = jsMainTpl(
            dkey        = dkey,
            qi_id       = qi_id,
            isInstall   = false,
            uSettings   = dus,
            isSiteAdmin = siteAdminOpt.isDefined
          )
          replyJs(respBody)
        }


      // Запрос скрипта для неизвестного сайта. ВОЗМОЖНО теперь там установлен скрипт.
      // Нужно проверить, относится ли запрос к юзеру, которому принадлежит qi и есть ли реально скрипт на сайте.
      case None =>
        import request.pwOpt
        lazy val firstCheckUrl = "http://" + dkey + "/"
        val isQi = DomainQi.isQi(dkey, qi_id)
        trace(logPrefix + s"dkey=$dkey not found in storage. isQi=$isQi")
        val futureResult = isQi match {
          // Да, это тот юзер, который запросил код на главной. Он устанавил скрипт на сайт или просто прошел по ссылки от скрипта.
          // Теперь нужно запустить проверку домена на наличие этого скрипта и вернуть скрипт с инсталлером.
          case true =>
            // Далее запрос становится асинхронным, т.к. ensureActorFor возвращает Future[ActorRef]. Можно блокироваться через EnsureSync, можно сгенерить фьючерс. Пока делаем второе.
            // Запустить очередь приема новостей, присоединив её к sio_notifier. Когда откроется ws-соединение, очередь будет выпилена, и коннект между sn и каналом ws будет уже прямой.
            val timestamp0 = NewsQueue4Play.getTimestampMs
            debug(logPrefix + s"Requesting NewsQueue for user=$pwOpt ;; tstamp=$timestamp0 ...")
            NewsQueue4Play.ensureActorFor(dkey, qi_id) flatMap { queueActorRef =>
              // Очередь запущена. Нужно подписать её на события SioNotifier.
              debug(logPrefix + s"NewsQueue $queueActorRef ready for user=$pwOpt. Subscribing NewsQueue for SioweNotifier events...")
              // Подписаться на события qi от sio_notifier
              SiowebNotifier.subscribeSync(
                subscriber = SnActorRefSubscriber(queueActorRef),
                classifier = QiEvent.getClassifier(dkeyOpt = Some(dkey), qiIdOpt=Some(qi_id))
              ) map { _ =>
                debug(logPrefix + "SN subscribed OK")
                // Подписывание очереди на событие выполнено.
                // Отправить request referer на проверку. Бывает, что юзер ставит поиск не на главной, а где-то сбоку.
                request.headers.get(REFERER) foreach { referer =>
                  // Если реферер совпадает с проверяемым url, то НЕ нужно его проверять второй раз.
                  if (referer != firstCheckUrl) {
                    debug(logPrefix + "found referrer: " + referer)
                    DomainQi.maybeCheckQiAsync(dkey=dkey, maybeUrl=referer, qi_id=qi_id, sendEvents=true, pwOpt=pwOpt)
                  } else {
                    debug(logPrefix + "ignoring refererer: " + referer)
                  }
                }
                // Сгенерить ответ.
                val respBody = jsMainTpl(
                  dkey = dkey,
                  qi_id = qi_id,
                  isInstall = true,
                  uSettings = MDomainUserSettings.empty(dkey),
                  isSiteAdmin = false,
                  wsTimestamp = Some(timestamp0)
                )
                replyJs(respBody)
              }

            // Перехватывать ошибки предыдущих фьючерсов
            } recover {
              case ex: Throwable =>
                // TODO Нужно вернуть юзеру скрипт, который отобразит ему внутреннюю ошибку сервера suggest.io.
                error(logPrefix + "start-qi future chain failed: .recover() -> HTTP 500", ex)
                InternalServerError("Internal server error")
            }

          // Кто-то зашел на ещё-не-установленный-сайт. Скрипт поиска выдавать смысла нет. Возвращаем уже готовый фьючерс, пригодный для дальнейшего комбинирования.
          case false =>
            warn(logPrefix + "isQi=false and domain not exist.")
            Future.successful {
              ServiceUnavailable("Search not properly installed. If site owner is you, please visit https://suggest.io/ and proceed installation steps.")
                .withHeaders(RETRY_AFTER -> "5")
            }
        }
        // Добавить фоновую проверку во фьючерс результата
        futureResult andThen { case _ =>
          trace(logPrefix + "All ok, let's check Qi in background...")
          // отправить ссылку на корень сайта на проверку, вдруг там действительно по-тихому установлен скрипт.
          DomainQi.checkQiAsync(
            dkey = dkey,
            url  = "http://" + dkey + "/",
            qiIdOpt = if (isQi) Some(qi_id) else None,
            sendEvents = true,
            pwOpt = pwOpt
          )
        }

    }
  }


  /**
   * Скрипт инсталлера отправил нам содержимое window.location. Возможно, его стоит отправить в очередь на онализ.
   * TODO Следует перенести этот микро-функционал в вебсокет в in-канал.
   * @param domain домен, заявленный клиентом.
   * @param qi_id qi_id, заявленное клиентом.
   * @return Возвращает различные коды ошибок с сообщениями.
   */
  def installUrl(domain:String, qi_id:String) = MaybeAuth { implicit request =>
    lazy val logPrefix = s"installUrl($domain, $qi_id): "
    trace(logPrefix + "starting...")

    addDomainFormM.bindFromRequest().fold(
      {formWithErrors =>
        warn(logPrefix + "POST parse failure: " + formWithErrors.errors)
        NotAcceptable("Not a URL.")}
      ,
      // Действительно пришла ссылка. Нужно проверить присланные domain и qi_id и отправить ссылку на проверку.
      {url =>
        val dkey = UrlUtil.normalizeHostname(domain)
        trace(logPrefix + s"Found URL in POST: $url ;; dkey = $dkey")
        if (DomainQi.isQi(dkey=dkey, qi_id=qi_id)) {
          
          if (DomainQi.maybeCheckQiAsync(dkey=dkey, maybeUrl = url.toExternalForm, qi_id=qi_id, sendEvents=true, request.pwOpt).isDefined) {
            debug(logPrefix + "200 OK - this url MAY be checked: " + url)
            Ok("OK.")
          } else {
            warn(logPrefix + "ignoring unwanted URL: " + url)
            Forbidden("No.")
          }

        } else {
          warn(logPrefix + s"Installer not running for $dkey -> $qi_id")
          Forbidden("Installer is not running for %s. Trying to cheat me? Well, go on." format domain)
        }
      }
    )
  }


  /**
   * Юзер устанавливает скрипт sio.js на сайт. Запустившийся инсталлер присоединяется к веб-сокету.
   * @param dkey ключ домена, заявленный клиентом.
   * @param qi_id ключ qi, заявленный клиентом.
   * @param timestampMs таймштамп, выданный на шаге рендера js.
   * @return каналы веб-сокет с данными JsValue.
   */
  def installWs(dkey:String, qi_id:String, timestampMs:Long) = WebSocket.using[JsValue] { implicit request =>
    // Чтобы префикс логгера не писать много раз, выносим его за скобки
    lazy val logPrefix = "installWs(%s %s %s): " format(dkey, qi_id, timestampMs)
    implicit val pwOpt = PersonWrapper.getFromRequest
    DomainQi.isQi(dkey, qi_id) match {
      // Настоящий юзер проходит настоящую установку.
      case true =>
        val (in0, out0) = EventUtil.globalUserEventIO
        // uuid для трубы, добавляемой в SioNotifier.
        val uuid = UUID.randomUUID()
        LOGGER.debug(logPrefix + "Starting ws connection for user %s. Subscriber=%s" format(pwOpt, uuid.toString))
        val classifier = QiEvent.getClassifier(dkeyOpt = Some(dkey), qiIdOpt = Some(qi_id))
        // Канал выдачи данных клиенту. Подписаться на события SioNotifier, затем залить в канал пропущенные новости, которые пришли между реквестами и во время подписки.
        // Возможные дубликаты новостей безопасны, опасность представляют потерянные уведомления.
        val out1 = out0 >- Concurrent.unicast(onStart = {channel: Concurrent.Channel[JsValue] =>
          EventUtil.replaceNqWithWsChannel(classifier, uuid, nqDkey=dkey, nqTyp=qi_id, channel=channel, nqIsMandatory=true, timestampMs=timestampMs, logPrefix=logPrefix)
        })
        // Канал приема данных от клиента. При EOF нужно отписать от событий out-канал.
        // TODO добавить сюда получатель window.location (см. installUrl() выше). Этот итератор in0 как-то не особо стремится комбинироваться с другими.
        /*val in2 = Iteratee
          .foreach[JsValue] {
            case JsObject(Seq(("install_url", JsString(maybeUrl)))) =>
              DomainQi.maybeCheckQiAsync(dkey=dkey, qi_id=qi_id, maybeUrl=maybeUrl)

            case other =>
          }*/
        val in1 = EventUtil.inIterateeSnUnsubscribeWsOnEOF(in0, uuid, classifier)
        (in1, out1)


      // Кто-то долбится на веб-сокет в обход сессии.
      case false =>
        LOGGER.error(logPrefix + "Requested dkey/qi_id not in session %s. Returning error to user via websocket." format session)
        EventUtil.wsAccessImpossbleIO("Illegial access, installation flow is not running.")
    }
  }


  private def replyJs(respBody:play.api.templates.Txt) = {
    trace("replyJs(): 200 Ok")
    Ok(respBody).as("text/javascript")
  }



  /*private def qiAction(dkey: String)(implicit request: RequestWithPwOpt): Future[SimpleResult] = {
    import request.pwOpt
  }*/

}
