package controllers

import com.google.inject.Inject
import models._
import models.adv._
import models.adv.ext.act.{OAuthVerifier, ActorPathQs}
import models.adv.search.etg.ExtTargetSearchArgs
import org.elasticsearch.search.sort.SortOrder
import play.api.i18n.{MessagesApi, Messages}
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.HandlerProps
import play.api.mvc.{Result, WebSocket}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.adv.{ExtUtil, ExtAdvWsActor}
import play.api.data._, Forms._
import util.FormUtil._
import views.html.lk.adv.ext._
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Akka.system

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 14:45
 * Description: Этот контроллер руководит взаимодейтсвием пользователя с системой размещения карточек в соц.сетях и
 * иных сервисах, занимающихся PR-деятельстью.
 * Логический родственник [[MarketAdv]], который занимается размещениями карточек на узлах.
 */
class LkAdvExt @Inject() (
  override val messagesApi: MessagesApi
)
  extends SioControllerImpl with PlayMacroLogsImpl
{

  import LOGGER._

  /** Сколько секунд с момента генерации ссылки можно попытаться запустить процесс работы, в секундах. */
  val WS_BEST_BEFORE_SECONDS = configuration.getInt("adv.ext.ws.api.best.before.seconds") getOrElse 30

  /** Маппинг одной выбранной цели. */
  private def advM: Mapping[Option[MExtTargetInfo]] = {
    // Заворачиваем id таргета в подмаппинг, чтобы матчить tgId. TODO: может можно просто "tg.id" в имени запилить?
    val t = tuple(
      "tg_id"    -> esIdM,
      "return"   -> optional(MExtReturns.mapping)
    )
    .transform[Option[MExtTargetInfo]] (
      { case (targetId, Some(ret)) => Some(MExtTargetInfo(targetId, ret))
        case _ => None },
      { case Some(info) => (info.targetId, Some(info.returnTo))
        case _ => ("", None) }
    )
    optional(t).transform[Option[MExtTargetInfo]] (
      _.flatten,
      {v => if (v.isDefined) Some(v) else None }
    )
  }

  private type Form_t = Form[List[MExtTargetInfo]]

  /** Маппинг формы со списком целей. */
  private def advsFormM: Form_t = {
    Form(
      "adv" -> list(advM)
        .transform[List[MExtTargetInfo]] (_.flatten, _.map(Some.apply))
        .verifying("error.required.at.least.one.adv.service", _.nonEmpty)
    )
  }


  /**
   * Запрос страницы с инфой по размещению указанной карточки в соц.сетях.
   * @param adId id рекламной карточки.
   * @return 200 Ок + страница с данными по размещениям на внешних сервисах.
   */
  def forAd(adId: String) = CanAdvertiseAdGet(adId).async { implicit request =>
    _forAdRender(adId, advsFormM)
      .map { Ok(_) }
  }

  private def _forAdRender(adId: String, form: Form_t)(implicit request: RequestWithAdAndProducer[_]): Future[Html] = {
    val targetsFut: Future[Seq[MExtTarget]] = {
      val args = ExtTargetSearchArgs(
        adnId       = request.producer.id,
        sortByDate  = Some(SortOrder.ASC),
        maxResults  = 100
      )
      MExtTarget.dynSearch(args)
    }
    val currentAdvsFut = MExtAdv.findForAd(adId)
      .map { advs =>
        advs.iterator
          .map { a =>  a.extTargetId -> a }
          .toMap
      }
    for {
      targets       <- targetsFut
      currentAdvs   <- currentAdvsFut
    } yield {
      forAdTpl(request.mad, request.producer, targets, currentAdvs, form)
    }
  }

  /**
   * Сабмит формы размещения рекламных карточек на внешних сервисах. Нужно:
   * - Распарсить и проверить данные реквеста.
   * - отрендерить страницу с системой взаимодействия с JS API указанных сервисов:
   * -- Нужно подготовить websocket url, передав в него всё состояние, полученное из текущего реквеста.
   * -- Ссылка должна иметь ttl и цифровую подпись для защиты от несанкционированного доступа.
   * @param adId id размещаемой рекламной карточки.
   * @return 200 Ok со страницей деятельности по размещению.
   */
  def advFormSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    advsFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advFormSubmit($adId): failed to bind from request:\n ${formatFormErrors(formWithErrors)}")
        _forAdRender(adId, formWithErrors)
          .map { NotAcceptable(_) }
      },
      {advs =>
        implicit val ctx = implicitly[Context]
        val wsCallArgs = MExtAdvQs(
          adId          = adId,
          targets       = advs,
          bestBeforeSec = WS_BEST_BEFORE_SECONDS,
          wsId          = ctx.ctxIdStr
        )
        Ok( advRunnerTpl(wsCallArgs, request.mad, request.producer)(ctx) )
      }
    )
  }

  private sealed case class ExceptionWithResult(res: Result) extends Exception

  /**
   * Открытие websocket'а, запускающее также процессы размещения, акторы и т.д.
   * @param qsArgs Подписанные параметры размещения.
   * @return 101 Upgrade.
   */
  def wsRun(qsArgs: MExtAdvQs) = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit requestHeader =>
    // Сначала нужно проверить права доступа всякие.
    val fut0 = if (qsArgs.bestBeforeSec <= BestBefore.nowSec) {
      Future successful None
    } else {
      val res = RequestTimeout("Request expired. Return back, refresh page and try again.")
      Future failed ExceptionWithResult(res)
    }
    // Если купон валиден, то сразу запускаем в фоне чтение данных по целям размещения...
    val targetsFut: Future[ActorTargets_t] = fut0.flatMap { _ =>
      val ids = qsArgs.targets.iterator.map(_.targetId)
      val targetsFut = MExtTarget.multiGet(ids)
      val targetsMap = qsArgs.targets
        .iterator
        .map { info => info.targetId -> info }
        .toMap
      targetsFut map { targets =>
        targets.iterator
          .flatMap { target =>
          target.id
            .flatMap(targetsMap.get)
            .map { info => MExtTargetInfoFull(target, info.returnTo) }
        }
          .toList
      }
    }
    // Одновременно запускаем сбор инфы по карточке и проверку прав на неё.
    fut0.flatMap { _ =>
      val madFut = MAd.getById(qsArgs.adId)
        .map(_.get)
        .recoverWith { case ex: NoSuchElementException =>
          Future failed ExceptionWithResult(NotFound("Ad not found: " + qsArgs.adId))
        }
      val pwOpt = PersonWrapper.getFromRequest
      madFut
        .flatMap { mad =>
          val req1 = RequestHeaderAsRequest(requestHeader)
          CanAdvertiseAd.maybeAllowed(pwOpt, mad, req1)
        }
        .map(_.get)
        .recoverWith { case ex: NoSuchElementException =>
           Future failed ExceptionWithResult(Forbidden("Login session expired. Return back and press F5."))
        }
    }.map { implicit req1 =>
      // Всё ок, запускаем актора, который будет вести переговоры с этим websocket'ом.
      val eaArgs = MExtWsActorArgs(qsArgs, req1, targetsFut)
      val hp: HandlerProps = ExtAdvWsActor.props(_, eaArgs)
      Right(hp)
    }.recover {
      case ExceptionWithResult(res) =>
        Left(res)
      case ex: Exception =>
        Left(InternalServerError("500 Internal server error. Please try again later."))
    }
  }


  /**
   * Запрос формы создания/редактирования цели для внешнего размещения рекламы.
   * @param adnId id узла.
   * @return 200 Ok с отрендеренной формой.
   */
  def writeTarget(adnId: String) = IsAdnNodeAdminGet(adnId) { implicit request =>
    val ctx = implicitly[Context]
    val form0 = ExtUtil.oneRawTargetFullFormM(adnId)
      .fill( ("", Some(Messages("New.target")(ctx.messages)), None) )
    Ok( _createTargetTpl(adnId, form0)(ctx) )
  }


  /**
   * Сабмит формы создания/обновления цели внешнего размещения рекламной карточки.
   * @param adnId id узла, к которому привязывается цель.
   * @return 200 Ok если цель создана.
   *         406 NotAcceptable если форма заполнена с ошибками. body содержит рендер формы с ошибками.
   */
  def writeTargetSubmit(adnId: String) = CanSubmitExtTargetForNode(adnId).async { implicit request =>
    request.newTgForm.fold(
      {formWithErrors =>
        debug(s"createTargetSubmit($adnId): Unable to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_targetFormTpl(adnId, formWithErrors, request.tgExisting))
      },
      {case (tg, ret) =>
        tg.save.map { tgId =>
          // Вернуть форму с выставленным id.
          val tg2 = tg.copy(id = Some(tgId))
          val form = request.newTgForm fill (tg2, ret)
          Ok(_targetFormTpl(adnId, form, Some(tg2)))
        }
      }
    )
  }


  /**
   * Сабмит удаления цели.
   * @param tgId id цели.
   * @return 204 No content, если удаление удалось.
   *         404 если не найдена запрошенная цель.
   *         403 при проблемах с доступом.
   *         Редирект, если сессия истекла.
   */
  def deleteTargetSubmit(tgId: String) = CanAccessExtTarget(tgId).async { implicit request =>
    request.extTarget.delete.map {
      case true =>
        NoContent
      case false =>
        warn(s"deleteTargetSubmit($tgId): Target not exists, already deleted in parallel request?")
        NotFound
    }
  }


  /**
   * Возвращение юзера после oauth1-авторизации, запрошенной указанным актором.
   * Всё действо происходит внутри попапа.
   * @param adnId id узла. Используется для ACL.
   * @param actorInfoQs Инфа по актору для связи с ним.
   * @return Что актор пожелает.
   *         В норме -- закрытие попапа с выставление шифрованного access-token'а в куку.
   */
  private def oauth1PopupReturn(adnId: String, actorInfoQs: ActorPathQs) = IsAdnNodeAdmin(adnId).async { implicit request =>
    trace(s"${request.method} oauth1return($adnId, $actorInfoQs): " + request.uri)
    val msg = OAuthVerifier(
      request.getQueryString("oauth_verifier")
    )
    system.actorSelection(actorInfoQs.path) ! msg
    // TODO Закрыть текущее окно
    // TODO Выставить в сессию access_token, полученный от актора.
    Ok("TODO")
  }
  def oauth1PopupReturnGet(adnId: String, actorInfoQs: ActorPathQs) = oauth1PopupReturn(adnId, actorInfoQs)
  def oauth1PopupReturnPost(adnId: String, actorInfoQs: ActorPathQs) = oauth1PopupReturn(adnId, actorInfoQs)

}
