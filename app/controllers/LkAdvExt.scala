package controllers

import java.net.URL

import models._
import models.adv._
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.HandlerProps
import play.api.mvc.{Result, WebSocket}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.adv.ExtAdvWsActor
import util.event.SiowebNotifier.Implicts.sn
import play.api.data._, Forms._
import util.FormUtil._
import views.html.lk.adv.ext._
import play.api.Play.{current, configuration}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 14:45
 * Description: Этот контроллер руководит взаимодейтсвием пользователя с системой размещения карточек в соц.сетях и
 * иных сервисах, занимающихся PR-деятельстью.
 * Логический родственник [[MarketAdv]], который занимается размещениями карточек на узлах.
 */
object LkAdvExt extends SioControllerImpl with PlayMacroLogsImpl {

  import LOGGER._

  /** Сколько секунд с момента генерации ссылки можно попытаться запустить процесс работы, в секундах. */
  val WS_BEST_BEFORE_SECONDS = configuration.getInt("adv.ext.ws.api.best.before.seconds") getOrElse 30

  /** Маппинг одной выбранной цели. */
  private def advM: Mapping[Option[String]] = {
    val t = tuple(
      "enabled"  -> optional(boolean),
      "targetId" -> esIdM
    )
    .transform[Option[String]] (
      { case (Some(true), targetId) => Some(targetId)
        case _ => None },
      { case Some(targetId) => (Some(true), targetId)
        case _ => (None, "") }
    )
    optional(t).transform[Option[String]] (
      _.flatten,
      {v => if (v.isDefined) Some(v) else None }
    )
  }

  private type Form_t = Form[List[String]]

  /** Маппинг формы со списком целей. */
  private def advsFormM: Form_t = {
    Form(
      "adv" -> list(advM)
        .transform[List[String]] (_.flatten, _.map(Some.apply))
        .verifying("error.required.at.least.one.adv.service", _.nonEmpty)
    )
  }


  /**
   * Запрос страницы с инфой по размещению указанной карточки в соц.сетях.
   * @param adId id рекламной карточки.
   * @return 200 Ок + страница с данными по размещениям на внешних сервисах.
   */
  def forAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    _forAdRender(adId, advsFormM)
      .map { Ok(_) }
  }

  private def _forAdRender(adId: String, form: Form_t)(implicit request: RequestWithAdAndProducer[_]): Future[Html] = {
    val targetsFut = MExtTarget.findByAdnId(request.producerId)
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
  def advFormSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
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
          targetIds     = advs,
          bestBeforeSec = WS_BEST_BEFORE_SECONDS,
          wsId          = ctx.ctxIdStr
        )
        Ok( advRunnerTpl(wsCallArgs, request.mad, request.producer)(ctx) )
      }
    )
  }

  private sealed case class ExceptionWithResult(res: Result) extends Exception
  def wsRun(args: MExtAdvQs) = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit requestHeader =>
    // Сначала нужно проверить права доступа всякие.
    val fut0 = if (args.bestBeforeSec <= BestBefore.nowSec) {
      Future successful None
    } else {
      val res = RequestTimeout("Request expired. Return back, refresh page and try again.")
      Future failed ExceptionWithResult(res)
    }
    fut0.flatMap { _ =>
      val madFut = MAd.getById(args.adId)
        .map(_.get)
        .recoverWith { case ex: NoSuchElementException =>
          Future failed ExceptionWithResult(NotFound("Ad not found: " + args.adId))
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
    }.map { req1 =>
      val hp: HandlerProps = ExtAdvWsActor.props(_, args)
      Right(hp)
    }.recover {
      case ExceptionWithResult(res) =>
        Left(res)
      case ex: Exception =>
        Left(InternalServerError("500 Internal server error. Please try again later."))
    }
  }


  /** Маппинг формы для ввода ссылки на цель. */
  def targetFormM: Form[MExtTarget] = {
    Form(
      // Проверять по доступным сервисам, подходит ли эта ссылка для хотя бы одного из них. И возвращать этот сервис.
      "url" -> urlM
        .transform[(URL, Option[MExtService])] (
          {url =>
            //val url1 = srv.normalizeTargetUrl(url)
            url -> MExtServices.findForHost(url.getHost)
          },
          { _._1 }
        )
        .verifying("error.service.unknown", _._2.isDefined)
        .transform [MExtTarget] (
          {case (url, srvOpt) =>
            val srv = srvOpt.get
            val url1 = srv.normalizeTargetUrl(url)
            MExtTarget(url = url1, service = srv, adnId = null)
          },
          { metgt => (new URL(metgt.url), Some(metgt.service)) }
        )
    )
  }

  /**
   * Запрос формы создания новой цели для размещения рекламы.
   * @param adnId id узла.
   * @return 200 Ok с отрендеренной формой.
   */
  def createTarget(adnId: String) = IsAdnNodeAdmin(adnId) { implicit request =>
    val form = targetFormM
    Ok(_createTargetTpl(request.adnNode, form))
  }

  /**
   * Сабмит формы создания новой цели для размещения рекламной карточки.
   * @param adnId id узла, к которому привязывается цель.
   * @return 200 Ok если цель создана.
   *         406 NotAcceptable если форма заполнена с ошибками. body содержит рендер формы с ошибками.
   */
  def createTargetSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    targetFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createTargetSubmit($adnId): Unable to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_createTargetTpl(request.adnNode, formWithErrors))
      },
      {tgt0 =>
        // TODO Обращаться по ссылке, получать title страницы, вставлять в поле name
        val tgt1 = tgt0.copy(
          adnId = adnId
        )
        tgt1.save.map { tgtId =>
          Ok("TODO")
        }
      }
    )
  }

}
