package controllers

import util.event.EventUtil
import play.api.mvc._
import util._
import models._
import scala.concurrent.future
import views.html.admin._
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.event.SioNotifier
import play.api.libs.json._
import java.util.UUID
import play.api.libs.iteratee.Concurrent
import io.suggest.event.subscriber.SnActorRefSubscriber
import scala.Some
import play.api.data._
import play.api.data.Forms._
import FormUtil._
import gnu.inet.encoding.IDNA
import DkeyContainer.dkeyJsProps
import io.suggest.util.UrlUtil
import domain_user_settings.DUS_Basic._
import MDomainUserSettings.{DataMap_t, DataMapKey_t}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 14:59
 * Description: Пользовательская админка сайтов. Основные функции:
 * - Рендер основной формы.
 * - Валидация доменов (не-qi).
 * - Управление доменами поиска.
 * - Сохранения настроек поиска.
 * - Другие команды.
 *
 * Исходное API сохраняется (как в прошлой версии).
 */

object Admin extends Controller with AclT with ContextT with Logs {

  private val nqTyp = "admin"

  // Маппинг для формы добавления домена в список доменов админки.
  val addDomainFormM = Form("domain" -> domain2dkeyMapper)

  // Форма базовых настроек домена.
  val domainBasicSettingsFormM = Form(tuple(
    "show_images"       -> boolean,
    "show_content_text" -> showMapper
  ))


  /**
   * Юзер заходит в админку (на главную страницу админки).
   * @return Нужно отрендерить главную форму админки со списком доменов и прочими причиндалами.
   */
  def index = isAuthenticated { implicit pw_opt => implicit request =>
    val pw = pw_opt.get
    lazy val logPrefix = "index() user=%s: " format pw.id
    // Опрос DFS может быть долгим, поэтому дальше всё делаем асинхронно.
    Async { future {
      val personDomains = pw.allDomainsAuthz
      // Т.к. в фоне будет запущена валидация доменов, надо ещё запустить очередь новостей, которая потом будет подцеплена
      // к SioNotifier, и затем заменена прямым на веб-сокетом. Процесс очереди НЕ надо запускать, если доменов нет
      // или если юзер является админом suggest.io.
      // В итоге получается, что очередь запускается на случай сбора новостей между этим экшеном и окончанием активации WebSocket'а.
      val timestampMs = NewsQueue4Play.getTimestampMs
      if (!pw.isAdmin && !personDomains.isEmpty) {
        logger.debug(logPrefix + "Maybe need revalidate %s domains. Starting news queue..." format personDomains.size)
        NewsQueue4Play.ensureActorFor(pw.id, nqTyp) onSuccess { case nqActorRef =>
          logger.debug(logPrefix + "NewsQueue started as %s" format nqActorRef)
          // Подписать очередь на события SioNotifier
          SioNotifier.subscribe(
            subscriber = new SnActorRefSubscriber(nqActorRef),
            classifier = getUserValidationClassifier(pw.id)
          )
          // Запустить валидацию доменов. Сразу подсчитываем число доменов, которые начали процедуру ревалидации.
          val rdvCount = personDomains.foldLeft(0) { (counter, pd) =>
            counter + pd.maybeRevalidate(sendEvents = true).size
          }
          // Если ни один из доменов не начал процедуру валидации, то очередь можно сразу грохнуть.
          if (rdvCount == 0) {
            NewsQueue4Play.stop(nqActorRef)
            logger.debug(logPrefix + "No revalidations needed. NQ %s stopping..." format nqActorRef)
          } else {
            logger.debug("Started %s revalidations" format rdvCount)
          }
        }
      }
      Ok(indexTpl(personDomains, timestampMs))
    }}
  }


  /**
   * Подключение к событиям админки через WebSocket.
   */
  def ws(timestampMs: Long) = WebSocket.using[JsValue] { implicit request =>
    implicit val pw_opt = person(request)
    lazy val logPrefix = "ws(%s) user=%s: " format(timestampMs, pw_opt)
    // Проверить права.
    if (pw_opt.isEmpty) {
      logger.warn(logPrefix + "Unexpected anonymous hacker: " + request.remoteAddress)
      // Анонимус долбиться на веб-сокет. Выдать ему сообщение о невозможности подобной эксплуатации энтерпрайза.
      EventUtil.wsAccessImpossbleIO("Anonymous users cannot use admin interface: not yet implemented.")

    } else {
      // Это зареганный юзер зашел в админку. Пока он входил, уже возможно запустилась очередь новостями перевалидации.
      val pw = pw_opt.get
      // Нужно найти очередь с новостями валидации (если она существует), и вытащить из неё данные,
      val classifier = getUserValidationClassifier(pw.id)
      val uuid = UUID.randomUUID()
      val (in0, out0) = EventUtil.globalUserEventIO
      // Подписаться на события валидации
      val out1 = out0 >- Concurrent.unicast(onStart = { channel: Concurrent.Channel[JsValue] =>
        EventUtil.replaceNqWithWsChannel(classifier, uuid, nqDkey=pw.id, nqTyp=nqTyp, channel=channel, nqIsMandatory=false, timestampMs=timestampMs, logPrefix=logPrefix)
      })
      // При закрытии канала отписаться от событий, подписанных выше.
      val in1 = EventUtil.inIterateeSnUnsubscribeWsOnEOF(in0, uuid, classifier)
      (in1, out1)
    }
  }



  /**
   * Запрос перевалидации одного домена. (Юзер нажимает клавишу recheck). POST-запрос, но домен передается снаружи запроса.
   * @return 204 No Content - recheck не требуется.
   *         201 Created    - запущена перевалидация
   */
  def revalidateDomain(domain: String) = isDomainAdmin(domain) { implicit pw_opt => implicit request => authz =>
    DomainValidator.maybeRevalidate(authz, sendEvents = true) match {
      case Some(_) => Created
      case None    => NoContent
    }
  }


  private val jsStatusOk       = JsString("ok")
  private val jsStatusNxDomain = JsString("no_such_domain")

  /**
   * Юзер сабмиттит форму добавления домена.
   * @return 201 Created - домен добавлен
   */
  def addDomain = isAuthenticated { implicit pw_opt => implicit request =>
    addDomainFormM.bindFromRequest().fold(
      // Пришла некорректная форма. Вернуть назад.
      formWithErrors =>
        // TODO нужно отрендернный шаблон формы возвращать, чтоб юзер мог увидеть ошибку.
        NotAcceptable("TODO")
      ,
      {dkey =>
        val jsonFields0 = dkeyJsProps(dkey)
        MDomain.getForDkey(dkey) match {
          // Есть такой домен в базе
          case Some(_) =>
            val person_id = pw_opt.get.id
            val da = MPersonDomainAuthz.newValidation(dkey=dkey, person_id=person_id).save
            // Теперь надо сгенерить json ответа
            val jsonFields1 = "status" -> jsStatusOk :: jsonFields0
            Created(JsObject(jsonFields1))

          case None =>
            val json = JsObject("status" -> jsStatusNxDomain :: jsonFields0)
            NotFound(json)
        }
      }
    )
  }


  /**
   * Юзер хочет удалить домен из списка.
   * @return 204 No content - удалено;
   *         404 Not Found  - не такого домена у юзера в списке (обычно) или какая-то другая неведомая ошибка.
   */
  def deleteDomain = isAuthenticated { implicit pw_opt => implicit request =>
    addDomainFormM.bindFromRequest().fold(
      formWithErrors => NotAcceptable
      ,
      {dkey =>
        MPersonDomainAuthz.delete(person_id=pw_opt.get.id, dkey=dkey) match {
          case true  => NoContent
          case false => NotFound
        }
      }
    )
  }


  /**
   * Юзер хочет пройти валидацию сайта, загрузив файл на сайт.
   * @param domain домен. Обычно dkey, но всё же будет повторно нормализован.
   */
  def getValidationFile(domain: String) = isAuthenticated { implicit pw_opt => implicit request =>
    val dkey = UrlUtil.normalizeHostname(domain)
    MPersonDomainAuthz.getForPersonDkey(person_id=pw_opt.get.id, dkey=dkey) match {
      // Отрендерить файлик валидации и вернуть его юзеру.
      case Some(da) =>
        Ok(getValidationFileTpl(da))
          .as("text/plain; charset=UTF-8")
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=" + da.filename))

      // Нет такого домена в списке добавленных юзером
      case None =>
        NotFound("No such domain in person's domain list.")
    }
  }


  /**
   * Юзер щелкнул по домену. Отобразить другую часть админки.
   * @param domain домен.
   * @return inline-рендер для домена.
   */
  def domainSettings(domain: String) = isDomainAdmin(domain) { implicit pw_opt => implicit request => authz =>
    val du = authz.domainUserSettingsAsync
    val form = domainBasicSettingsFormM.fill((du.showImages, du.showContentText))
    Ok(_domainSettingsTpl(du, form))
  }

  /**
   * Сабмит формы настроек из страницы админки.
   * @param domain домен
   * @return
   */
  def domainSettingsFormSubmit(domain: String) = isDomainAdmin(domain) { implicit pw_opt => implicit request => authz =>
    domainBasicSettingsFormM.bindFromRequest().fold(
      formWithErrors => NotAcceptable
      ,
      {case (showImages, showContentText) =>
        val du = authz.domainUserSettings
        val data1 = du.data + (KEY_SHOW_IMAGES -> showImages) + (KEY_SHOW_CONTENT_TEXT -> showContentText)
        du.withData(data1).save
        Ok
      }
    )
  }


  /**
   * Накатить настройки домена. Используется за пределами страницы админки.
   * @param domain Домен, для которого сохраняем.
   * @return ???
   */
  def applyDomainSettings(domain:String) = isDomainAdmin(domain) { implicit pw_opt => implicit request => authz =>
    val du = authz.domainUserSettings
    val data1 = request.body
      .asFormUrlEncoded
      .get
      .foldLeft(du.data) { case (_data, kvs) =>
        val (k, vs) = kvs
        applyDUSF((k, vs.head, _data))
    }
    val du1 = du.withData(data1).save
    Ok("Ok TODO")
  }


  // TODO Осталось ещё POST reindex?


  /**
   * Фунция накатывания различных параметров (без формы) на DomainUserSettings собирается здеся.
   * Когда будут новые группы параметров, нужно будет подключать новые функции эти сюда.
   */
  private val applyDUSF: PartialFunction[(DataMapKey_t, String, DataMap_t), DataMap_t] = {
    applyBasicSettingsF orElse {
      case (k, v, data) =>
        logger.warn("Unknown DUS key=%s => %s SKIPPED" format(k, v))
        data
    }
  }


  /**
   * Эта функция вызывается для генерации ответа, когда выполнение какого-то действия в контроллере запрещено по причине
   * незалогиненности юзера. В данном случае, должен происходить редирект на форму логина.
   * request с пометкой implicit
   * @param request Заголовки запроса.
   * @return Result, такой же как и в экшенах контроллеров.
   */
  override protected def onUnauthorized(request: RequestHeader) = Redirect( routes.Ident.persona() )

  /**
   * Генерация классификатора событий для юзера в админке.
   * @param person_id id юзера. Т.е. обычно почта.
   * @return Классификатор пригодный для SioNotifier
   */
  private def getUserValidationClassifier(person_id: String) = DVEventUtil.getClassifier(personIdOpt = Some(person_id))

}
