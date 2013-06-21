package controllers

import util.event._
import play.api.mvc.{WebSocket, Controller}
import play.api.data._
import util.FormUtil._
import _root_.util._
import io.suggest.util.UrlUtil
import models.{MDomainUserSettings, MDomain}
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
import java.util.UUID
import play.api.libs.iteratee.{Iteratee, Concurrent}
import io.suggest.util.event.subscriber.{SioEventTJSable, SnWebsocketSubscriber}


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

object Js extends Controller with AclT with ContextT with Logs {

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
            val timestamp0 = NewsQueue4Play.getTimestampMs
            logger.debug("Requesting NewsQueue for (%s %s) user=%s..." format(dkey, qi_id, pw_opt))
            NewsQueue4Play.ensureActorFor(dkey, qi_id).map { queueActorRef =>
              logger.debug("NewsQueue %s for (%s %s) ready." format(queueActorRef, dkey, qi_id))
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
                isSiteAdmin = false,
                wsTimestamp = Some(timestamp0)
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
   * TODO Следует перенести этот микро-функционал в вебсокет в in-канал.
   * @param domain домен, заявленный клиентом.
   * @param qi_id qi_id, заявленное клиентом.
   * @return Возвращает различные коды ошибок с сообщениями.
   */
  def installUrl(domain:String, qi_id:String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    addDomainFormM.bindFromRequest().fold(
      {formWithErrors => NotAcceptable("Not a URL.")}
      ,
      // Действительно пришла ссылка. Нужно проверить присланные domain и qi_id и отправить ссылку на проверку.
      {url =>
        val dkey = UrlUtil.normalizeHostname(domain)
        val result = if (DomainQi.isQi(dkey, qi_id)) {
          if (DomainQi.maybeCheckQiAsync(dkey=dkey, maybeUrl = url.toExternalForm, qi_id=qi_id))
            Ok("Ok, your URL will be checked.")
          else
            Forbidden("Unexpected 3rd-party URL.")

        } else {
          Forbidden("Installer is not running for %s. Trying to cheat me? Well, go on." format domain)
        }
        logger.debug("installUrl(%s %s): User %s want us to check also URL=%s. Decision: %s" format(dkey, qi_id, pw_opt, url, result))
        result
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
    implicit val pw_opt = person(request)
    val (in0, out0) = EventUtil.globalUserEventIO
    DomainQi.isQi(dkey, qi_id) match {
      case true =>
        // uuid для трубы, добавляемой в SioNotifier.
        val uuid = UUID.randomUUID()
        logger.debug(logPrefix + "Starting ws connection for user %s. Subscriber=%s" format(pw_opt, uuid.toString))
        val classifier  = QiEventUtil.getClassifier(dkeyOpt = Some(dkey), qiIdOpt = Some(qi_id))
        // Канал выдачи данных клиенту. Подписаться на события SioNotifier, затем залить в канал пропущенные новости, которые пришли между реквестами и во время подписки.
        // Возможные дубликаты новостей безопасны, опасность представляют потерянные уведомления.
        val out1 = out0 >- Concurrent.unicast(onStart = {channel: Concurrent.Channel[JsValue] =>
          NewsQueue4Play.getActorFor(dkey, qi_id).foreach { nqActorRefOpt =>
            val subscriberWs = new SnWebsocketSubscriber(uuid=uuid, channel=channel)
            val snActionFuture: Future[Boolean] = nqActorRefOpt match {
              // Как и ожидалось, у супервизора уже есть очередь с новостями. Нужно заменить её в SioNotifier на прямой канал SN -> WS.
              case Some(nqActorRef) =>
                logger.debug(logPrefix + "SN atomic replace: NewsQueue %s -> %s; user=%s" format(nqActorRef, subscriberWs, pw_opt))
                SioNotifier.replaceSubscriberSync(
                  subscriberOld = SnActorRefSubscriber(nqActorRef),
                  classifier    = classifier,
                  subscriberNew = subscriberWs
                ) andThen { // Затем нужно перекачать накопленные новости в открытый канал.
                  case _ =>
                    NewsQueue4Play.shortPull(nqActorRef, timestampMs).foreach { newsReply =>
                      val news = newsReply.news
                      val newsFailed = news.foldLeft(List[NewsQueue4Play.NewsEventT]()) { (accWrong, n) =>
                        n match {
                          case n:SioEventTJSable =>
                            channel.push(n.toJson)
                            accWrong

                          case other => other :: accWrong
                        }
                      }
                      // Если были недопустимые новости, то нужен варнинг в логах.
                      if(!newsFailed.isEmpty)
                        logger.warn(logPrefix + "forwarded only %s news of total %s. Failed to forward: %s" format(news.size - newsFailed.size, news.size, newsFailed))
                      else
                        logger.debug("installWs(%s %s %s): " format(dkey, qi_id, timestampMs))
                    }
                    logger.debug(logPrefix + "Async.stopping NewsQueue %s ..." format(nqActorRef))
                    NewsQueue4Play.stop(nqActorRef)
                }

              // Внезапно очереди нет. Это плохо, и это скорее всего приведет к ошибке, если валидация сайта уже прошла до текущего момента,
              // и значит уведомление об успехе было отправлено в /dev/null. Нужно предложить юзеру обновить страницу.
              // TODO Может нужно обновить страницу сразу? Или отобразить кнопку релоада юзеру?
              case None =>
                logger.error(logPrefix + "NewsQueue doesn't exist, but it should. Possible incorrect behaviour for user %s." format(pw_opt))
                channel.push(MaybeErrorEvent("It looks like, something went wrong during installation procedure. If errors or problems occurs, please reload the page.").toJson)
                SioNotifier.subscribeSync(
                  subscriber = subscriberWs,
                  classifier = classifier
                )
            }
            // перехват возможных внутренних ошибок
            snActionFuture onFailure { case ex:Throwable =>
              logger.error(logPrefix + "Internal error during NQ -> WS swithing.", ex)
              channel.push(InternalServerErrorEvent("Suggest.io has detected internal error.").toJson)
            }
          }
        })
        // Канал приема данных от клиента. При EOF нужно отписать от событий out-канал.
        // TODO добавить сюда получатель window.location (см. installUrl() выше). Этот итератор in0 как-то не особо стремится комбинироваться с другими.
        /*val in2 = Iteratee
          .foreach[JsValue] {
            case JsObject(Seq(("install_url", JsString(maybeUrl)))) =>
              DomainQi.maybeCheckQiAsync(dkey=dkey, qi_id=qi_id, maybeUrl=maybeUrl)

            case other =>
          }*/
        val in1 = in0.mapDone { _ =>
          SioNotifier.unsubscribe(
            subscriber = new SnWebsocketSubscriber(uuid=uuid, channel = null),
            classifier = classifier
          )
        }
        (in1, out1)


      // Кто-то долбится на веб-сокет в обход сессии.
      case false =>
        logger.error(logPrefix + "Requested dkey/qi_id not in session %s. Returning error to user via websocket." format(session))
        val out1 = out0 >- Concurrent.unicast(onStart = {channel: Concurrent.Channel[JsValue] =>
          // TODO дергать нормальное событие в виде json, а не эту строку.
          channel.push(AccessErrorEvent("Illegial access, install flow is not running").toJson)
        })
        (in0, out1)
    }
  }


  private def replyJs(respBody:Html) = Ok(respBody).as("text/javascript")

}
