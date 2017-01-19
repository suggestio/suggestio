package controllers

import com.google.inject.Inject
import io.suggest.common.empty.EmptyUtil
import io.suggest.model.n2.node.MNodes
import models.adv._
import models.adv.ext.act.{ActorPathQs, OAuthVerifier}
import models.adv.ext.{MAdvRunnerTplArgs, MForAdTplArgs}
import models.adv.search.etg.ExtTargetSearchArgs
import models.jsm.init.MTargets
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.{IAdProdReq, MReqNoBody}
import org.elasticsearch.search.sort.SortOrder
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.HandlerProps
import play.api.mvc.{Result, WebSocket}
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl._
import util.adv.ext.{AdvExtFormUtil, AdvExtWsActors}
import views.html.helper.CSRF
import views.html.lk.adv.ext._
import views.html.static.popups.closingPopupTpl

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
                           override val canAdvAdUtil       : CanAdvertiseAdUtil,
                           mNodes                          : MNodes,
                           advExtWsActors                  : AdvExtWsActors,
                           override val mExtTargets        : MExtTargets,
                           override val advExtFormUtil     : AdvExtFormUtil,
                           override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAccessExtTarget
  with CanAdvertiseAd
  with CanSubmitExtTargetForNode
  with IsAdnNodeAdmin
{

  import LOGGER._
  import mCommonDi._

  /** Сколько секунд с момента генерации ссылки можно попытаться запустить процесс работы, в секундах. */
  private val WS_BEST_BEFORE_SECONDS = configuration
    .getInt("adv.ext.ws.api.best.before.seconds")
    .getOrElse(30)

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
    optional(t)
      .transform [Option[MExtTargetInfo]] (
        _.flatten,
        _.map( EmptyUtil.someF )
      )
  }


  /** Маппинг формы со списком целей. */
  private def advsFormM: ExtAdvForm = {
    Form(
      "adv" -> list(advM)
        .transform[List[MExtTargetInfo]] (_.flatten, _.map(Some.apply))
        .verifying("error.required.at.least.one.adv.service", _.nonEmpty)
    )
  }


  /**
   * Запрос страницы с инфой по размещению указанной карточки в соц.сетях.
   *
   * @param adId id рекламной карточки.
   * @return 200 Ок + страница с данными по размещениям на внешних сервисах.
   */
  def forAd(adId: String) = CanAdvertiseAdGet(adId).async { implicit request =>
    _forAdRender(adId, advsFormM, Ok)
  }

  private def _forAdRender(adId: String, form: ExtAdvForm, rs: Status)
                          (implicit request: IAdProdReq[_]): Future[Result] = {
    val targetsFut: Future[Seq[MExtTarget]] = {
      val args = ExtTargetSearchArgs(
        adnId       = request.producer.id,
        sortByDate  = Some(SortOrder.ASC),
        limit       = 100
      )
      mExtTargets.dynSearch(args)
    }

    for {
      ctxData0  <- request.user.lkCtxDataFut
      targets   <- targetsFut
    } yield {
      val args = MForAdTplArgs(
        mad         = request.mad,
        producer    = request.producer,
        targets     = targets,
        advForm     = form,
        oneTgForm   = advExtFormUtil.formForTarget
      )
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.LkAdvExtForm)
      )
      rs( forAdTpl(args) )
    }
  }

  /**
   * Сабмит формы размещения рекламных карточек на внешних сервисах. Нужно:
   * - Распарсить и проверить данные реквеста.
   * - отрендерить страницу с системой взаимодействия с JS API указанных сервисов:
   * -- Нужно подготовить websocket url, передав в него всё состояние, полученное из текущего реквеста.
   * -- Ссылка должна иметь ttl и цифровую подпись для защиты от несанкционированного доступа.
   *
   * @param adId id размещаемой рекламной карточки.
   * @return 200 Ok со страницей деятельности по размещению.
   */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    advsFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advFormSubmit($adId): failed to bind from request:\n ${formatFormErrors(formWithErrors)}")
        _forAdRender(adId, formWithErrors, NotAcceptable)
      },
      {advs =>
        val wsArgs = MExtAdvQs(
          adId          = adId,
          targets       = advs,
          bestBeforeSec = WS_BEST_BEFORE_SECONDS,
          wsId          = "" // TODO Это не нужно на данном этапе. runner() был вынесен в отдельнй экшен.
        )
        Redirect( CSRF(routes.LkAdvExt.runner(adId, Some(wsArgs))) )
      }
    )
  }

  /**
   * Рендер страницы с runner'ом.
   *
   * @param adId id размещаемой рекламной карточки.
   * @param wsArgsOpt Аргументы из сабмита.
   *                  Если не заданы, то будет редирект на форму размещения.
   * @return Страница с системой размещения.
   */
  def runner(adId: String, wsArgsOpt: Option[MExtAdvQs]) = CanAdvertiseAdPost(adId, U.Lk).async { implicit request =>
    wsArgsOpt.fold [Future[Result]] {
      // Аргументы не заданы. Такое бывает, когда юзер обратился к runner'у, но изменился ключ сервера или истекла сессия.
      Redirect(routes.LkAdvExt.forAd(adId))

    } { wsArgs =>
      for (ctxData0 <- request.user.lkCtxDataFut) yield {
        implicit val ctxData = ctxData0.copy(
          jsiTgs = Seq(MTargets.AdvExtRunner)
        )
        implicit val ctx = implicitly[Context]
        val wsArgs2 = wsArgs.copy(
          wsId = ctx.ctxIdStr,
          adId = adId
        )
        val rargs = MAdvRunnerTplArgs(
          wsCallArgs  = wsArgs2,
          mad         = request.mad,
          mnode       = request.producer
        )
        Ok( advRunnerTpl(rargs)(ctx) )
      }
    }
  }

  private sealed case class ExceptionWithResult(res: Result) extends Exception

  /**
   * Открытие websocket'а, запускающее также процессы размещения, акторы и т.д.
   *
   * @param qsArgs Подписанные параметры размещения.
   * @return 101 Upgrade.
   */
  def wsRun(qsArgs: MExtAdvQs) = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit requestHeader =>
    val resFut = for {
      // Сначала нужно синхронно проверить права доступа всякие.
      _ <- {
        if (qsArgs.bestBeforeSec <= BestBefore.nowSec) {
          Future successful None
        } else {
          val res = RequestTimeout("Request expired. Return back, refresh page and try again.")
          Future.failed( ExceptionWithResult(res) )
        }
      }

      // Запустить поиск списка целей размещения
      targetsFut: Future[ActorTargets_t] = {
        val ids = qsArgs.targets.iterator.map(_.targetId)
        val _targetsFut = mExtTargets.multiGetRev(ids)
        val targetsMap = qsArgs.targets
          .iterator
          .map { info => info.targetId -> info }
          .toMap
        _targetsFut map { targets =>
          targets.iterator
            .flatMap { target =>
              target.id
                .flatMap(targetsMap.get)
                .map { info => MExtTargetInfoFull(target, info.returnTo) }
            }
            .toList
        }
      }

      // Запустить получение текущей рекламной карточки
      madFut = mNodes.getById(qsArgs.adId)
        .map(_.get)
        .recoverWith { case ex: NoSuchElementException =>
          val ex2 = ExceptionWithResult(NotFound("Ad not found: " + qsArgs.adId))
          Future.failed(ex2)
        }

      // Собрать кое-какие синхронные данные.
      personIdOpt = sessionUtil.getPersonId(requestHeader)
      user = mSioUsers(personIdOpt)

      // Дождаться получения рекламной картоки
      mad <- madFut
      //
      adProdReq <- {
        val req1 = MReqNoBody(requestHeader, user) // RequestHeaderAsRequest(requestHeader)
        canAdvAdUtil.maybeAllowed(mad, req1)
          .map(_.get)
          .recoverWith { case _: NoSuchElementException =>
            val ex2 = ExceptionWithResult(Forbidden("Login session expired. Return back and press F5."))
            Future.failed(ex2)
          }
      }

    } yield {

      implicit val req1 = adProdReq

      // Всё ок, запускаем актора, который будет вести переговоры с этим websocket'ом.
      val eaArgs = MExtWsActorArgs(qsArgs, req1, targetsFut)
      val hp: HandlerProps = advExtWsActors.props(_, eaArgs)
      Right(hp)
    }

    resFut.recover {
      case ExceptionWithResult(res) =>
        Left(res)
      case ex: Exception =>
        Left(InternalServerError("500 Internal server error. Please try again later."))
    }
  }


  /**
   * Запрос формы создания/редактирования цели для внешнего размещения рекламы.
   *
   * @param adnId id узла.
   * @return 200 Ok с отрендеренной формой.
   */
  def writeTarget(adnId: String) = IsAdnNodeAdminGet(adnId) { implicit request =>
    val ctx = implicitly[Context]
    val form0 = advExtFormUtil.oneRawTargetFullFormM(adnId)
      .fill( ("", Some(ctx.messages("New.target")), None) )
    Ok( _createTargetTpl(adnId, form0)(ctx) )
  }


  /**
   * Сабмит формы создания/обновления цели внешнего размещения рекламной карточки.
   *
   * @param adnId id узла, к которому привязывается цель.
   * @return 200 Ok если цель создана.
   *         406 NotAcceptable если форма заполнена с ошибками. body содержит рендер формы с ошибками.
   */
  def writeTargetSubmit(adnId: String) = CanSubmitExtTargetForNodePost(adnId).async { implicit request =>
    request.newTgForm.fold(
      {formWithErrors =>
        debug(s"createTargetSubmit($adnId): Unable to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_targetFormTpl(adnId, formWithErrors, request.tgExisting))
      },
      {case (tg, ret) =>
        for (tgId <- mExtTargets.save(tg)) yield {
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
   *
   * @param tgId id цели.
   * @return 204 No content, если удаление удалось.
   *         404 если не найдена запрошенная цель.
   *         403 при проблемах с доступом.
   *         Редирект, если сессия истекла.
   */
  def deleteTargetSubmit(tgId: String) = CanAccessExtTarget(tgId).async { implicit request =>
    for {
      isDeleted <- mExtTargets.deleteById( tgId )
    } yield {
      if (isDeleted) {
        NoContent
      } else {
        warn(s"deleteTargetSubmit($tgId): Target not exists, already deleted in parallel request?")
        NotFound
      }
    }
  }


  /**
   * Возвращение юзера после oauth1-авторизации, запрошенной указанным актором.
   * Всё действо происходит внутри попапа.
   *
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
    actorSystem.actorSelection(actorInfoQs.path) ! msg
    // Закрыть текущее окно
    Ok( closingPopupTpl() )
  }
  def oauth1PopupReturnGet(adnId: String, actorInfoQs: ActorPathQs) = oauth1PopupReturn(adnId, actorInfoQs)
  def oauth1PopupReturnPost(adnId: String, actorInfoQs: ActorPathQs) = oauth1PopupReturn(adnId, actorInfoQs)

}
