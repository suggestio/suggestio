package controllers

import javax.inject.{Inject, Singleton}
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.EsModel
import io.suggest.ext.svc.MExtServices
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.csp.Csp
import io.suggest.util.logs.MacroLogsImpl
import models.adv._
import models.adv.ext.act.{ActorPathQs, OAuthVerifier}
import models.adv.ext.{MAdvRunnerTplArgs, MExtTargetSearchArgs, MForAdTplArgs}
import models.mctx.Context
import models.mext.MExtServicesJvm
import models.mproj.ICommonDi
import models.req.{IAdProdReq, MReqNoBody}
import org.elasticsearch.search.sort.SortOrder
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Result, WebSocket}
import util.FormUtil._
import util.acl._
import util.adv.ext.{AdvExtFormUtil, AdvExtWsActors}
import util.sec.CspUtil
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
 */
@Singleton
class LkAdvExt @Inject() (
                           esModel                         : EsModel,
                           canAdvAd                        : CanAdvAd,
                           mNodes                          : MNodes,
                           advExtWsActors                  : AdvExtWsActors,
                           isNodeAdmin                     : IsNodeAdmin,
                           mExtTargets                     : MExtTargets,
                           canSubmitExtTargetForNode       : CanSubmitExtTargetForNode,
                           advExtFormUtil                  : AdvExtFormUtil,
                           cspUtil                         : CspUtil,
                           aclUtil                         : AclUtil,
                           canAccessExtTarget              : CanAccessExtTarget,
                           sioControllerApi                : SioControllerApi,
                           mCommonDi                       : ICommonDi
)
  extends MacroLogsImpl
{

  import sioControllerApi._
  import LOGGER._
  import mCommonDi._
  import esModel.api._
  import cspUtil.Implicits._


  /** Сколько секунд с момента генерации ссылки можно попытаться запустить процесс работы, в секундах. */
  private def WS_BEST_BEFORE_SECONDS = 600


  /** CSP-заголовок, разрешающий работу системы внешнего размещения карточек. */
  private def _CSP_HDR_OPT: Option[(String, String)] = {
    cspUtil.mkCustomPolicyHdr { csp0 =>
      val allSrcsIter = for {
        mExtService <- MExtServices.values.iterator
        if mExtService.hasAdvExt
        advExt = MExtServicesJvm.forService( mExtService ).advExt
        domain      <- advExt.cspSrcDomains
        proto       <- advExt.cspSrcProtos
      } yield {
        proto + "://" + domain
      }

      val allSrcs = allSrcsIter.toList

      csp0
        .addDefaultSrc( allSrcs: _* )
        .addScriptSrc( Csp.Sources.UNSAFE_EVAL :: Csp.Sources.UNSAFE_INLINE :: allSrcs: _* )
        .addImgSrc( allSrcs: _* )
        .addConnectSrc( allSrcs: _* )
        .addStyleSrc( allSrcs: _* )
    }
  }


  private def _nowSec = System.currentTimeMillis() / 1000L

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
  def forAd(adId: String) = csrf.AddToken {
    canAdvAd(adId, U.Lk).async { implicit request =>
      _forAdRender(adId, advsFormM, Ok)
    }
  }

  private def _forAdRender(adId: String, form: ExtAdvForm, rs: Status)
                          (implicit request: IAdProdReq[_]): Future[Result] = {
    val targetsFut: Future[Seq[MExtTarget]] = {
      val args = MExtTargetSearchArgs(
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
      implicit val ctxData = ctxData0.withJsInitTargets(
        MJsInitTargets.LkAdvExtForm :: ctxData0.jsInitTargets
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
  def forAdSubmit(adId: String) = csrf.Check {
    canAdvAd(adId).async { implicit request =>
      advsFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(s"advFormSubmit($adId): failed to bind from request:\n ${formatFormErrors(formWithErrors)}")
          _forAdRender(adId, formWithErrors, NotAcceptable)
        },
        {advs =>
          val wsArgs = MExtAdvQs(
            adId          = adId,
            targets       = advs,
            bestBeforeSec = _nowSec + WS_BEST_BEFORE_SECONDS,
            wsId          = "" // TODO Это не нужно на данном этапе. runner() был вынесен в отдельнй экшен.
          )
          Redirect( CSRF(routes.LkAdvExt.runner(adId, Some(wsArgs))) )
        }
      )
    }
  }

  /**
   * Рендер страницы с runner'ом.
   *
   * @param adId id размещаемой рекламной карточки.
   * @param wsArgsOpt Аргументы из сабмита.
   *                  Если не заданы, то будет редирект на форму размещения.
   * @return Страница с системой размещения.
   */
  def runner(adId: String, wsArgsOpt: Option[MExtAdvQs]) = csrf.Check {
    canAdvAd(adId, U.Lk).async { implicit request =>
      def logPrefix = s"runner($adId):"

      wsArgsOpt.fold [Future[Result]] {
        // Аргументы не заданы. Такое бывает, когда юзер обратился к runner'у, но изменился ключ сервера или истекла сессия.
        LOGGER.debug(s"$logPrefix wsArgs are empty, rdr user#${request.user.personIdOpt.orNull} back to form.")
        Redirect(routes.LkAdvExt.forAd(adId))

      } { wsArgs =>
        val now = _nowSec
        if (wsArgs.bestBeforeSec < now) {
          LOGGER.debug(s"$logPrefix Deprecated request TTL=${wsArgs.bestBeforeSec}, now = $now, wsArgs = $wsArgs")
          Redirect( routes.LkAdvExt.forAd(adId) )
            .flashing(FLASH.ERROR -> "Please.try.again")

        } else {

          for (ctxData0 <- request.user.lkCtxDataFut) yield {
            implicit val ctxData = ctxData0.withJsInitTargets(
              MJsInitTargets.AdvExtRunner :: ctxData0.jsInitTargets
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

            // Запилить CSP-заголовок с сильно-расширенной политикой безопасности.
            Ok( advRunnerTpl(rargs)(ctx) )
              .withCspHeader( _CSP_HDR_OPT )
          }
        }
      }
    }
  }

  private sealed case class ExceptionWithResult(result: Result) extends Exception

  /**
   * Открытие websocket'а, запускающее также процессы размещения, акторы и т.д.
   *
   * @param qsArgs Подписанные параметры размещения.
   * @return 101 Upgrade.
   */
  def wsRun(qsArgs: MExtAdvQs) = WebSocket.acceptOrResult[JsValue, JsValue] { implicit requestHeader =>
    lazy val logPrefix = s"wsRun[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix $qsArgs")

    val resFut = for {
      // Сначала нужно синхронно проверить права доступа всякие.
      _ <- {
        val now = _nowSec
        if (qsArgs.bestBeforeSec >= now) {
          Future.successful(None)
        } else {
          LOGGER.info(s"$logPrefix Expired ext adv ws TLL: ${qsArgs.bestBeforeSec} <= $now")
          val res = NotAcceptable("Request expired. Return back, refresh page and try again.")
          Future.failed( ExceptionWithResult(res) )
        }
      }

      // Запустить поиск списка целей размещения
      targetsFut: Future[ActorTargets_t] = {
        val _targetsFut = mExtTargets.multiGet {
          for (t <- qsArgs.targets.iterator) yield t.targetId
        }

        val targetsMap = qsArgs.targets
          .iterator
          .map { info => info.targetId -> info }
          .toMap

        for (targets <- _targetsFut) yield {
          val iter = for {
            target <- targets.iterator
            tgId   <- target.id
            info   <- targetsMap.get(tgId)
          } yield {
            MExtTargetInfoFull(target, info.returnTo)
          }
          iter.toList
        }
      }

      // Запустить получение текущей рекламной карточки
      madFut = mNodes.getById(qsArgs.adId)
        .map(_.get)
        .recoverWith { case _: NoSuchElementException =>
          val msg = s"Node not found: ${qsArgs.adId}"
          LOGGER.error(s"$logPrefix $msg")
          errorHandler
            .onClientError(requestHeader, NOT_FOUND, msg)
            .flatMap { resp =>
              Future.failed( ExceptionWithResult(resp) )
            }
        }

      // Собрать кое-какие синхронные данные.
      user = aclUtil.userFromRequest(requestHeader)

      // Дождаться получения рекламной картоки
      mad <- madFut

      adProdReq <- {
        val req1 = MReqNoBody(requestHeader, user) // RequestHeaderAsRequest(requestHeader)
        canAdvAd.maybeAllowed(mad, req1)
          .map(_.get)
          .recoverWith { case _: NoSuchElementException =>
            val msg = s"$logPrefix User[$user] not allowed to adv ad#${mad.idOrNull}"
            LOGGER.warn(msg)
            errorHandler.onClientError(requestHeader, FORBIDDEN, msg).flatMap { resp =>
              Future.failed( ExceptionWithResult(resp) )
            }
          }
      }

    } yield {

      implicit val req1 = adProdReq

      // Всё ок, запускаем актора, который будет вести переговоры с этим websocket'ом.
      val eaArgs = MExtWsActorArgs(qsArgs, req1, targetsFut)
      val aFlow = ActorFlow.actorRef[JsValue, JsValue](
        props = advExtWsActors.props(_, eaArgs)
      )
      LOGGER.trace(s"$logPrefix Ok, actor flow => $aFlow")
      Right(aFlow)
    }

    resFut.recoverWith { case ex: Throwable =>
      val resFut = ex match {
        case ExceptionWithResult(res) =>
          LOGGER.trace(s"$logPrefix Returning HTTP ${res.header.status} (failure), see logs upper.")
          Future.successful(res)
        case ex: Throwable =>
          LOGGER.error(s"$logPrefix Failure, qs = $qsArgs", ex)
          errorHandler.onClientError(requestHeader, INTERNAL_SERVER_ERROR)
      }
      resFut
        .map( Left.apply )
    }
  }


  /**
   * Запрос формы создания/редактирования цели для внешнего размещения рекламы.
   *
   * @param adnId id узла.
   * @return 200 Ok с отрендеренной формой.
   */
  def writeTarget(adnId: String) = csrf.AddToken {
    isNodeAdmin(adnId) { implicit request =>
      val ctx = implicitly[Context]
      val form0 = advExtFormUtil.oneRawTargetFullFormM(adnId)
        .fill( ("", Some(ctx.messages("New.target")), None) )
      Ok( _createTargetTpl(adnId, form0)(ctx) )
    }
  }


  /**
   * Сабмит формы создания/обновления цели внешнего размещения рекламной карточки.
   *
   * @param adnId id узла, к которому привязывается цель.
   * @return 200 Ok если цель создана.
   *         406 NotAcceptable если форма заполнена с ошибками. body содержит рендер формы с ошибками.
   */
  def writeTargetSubmit(adnId: String) = csrf.Check {
    canSubmitExtTargetForNode(adnId).async { implicit request =>
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
  def deleteTargetSubmit(tgId: String) = canAccessExtTarget(tgId).async { implicit request =>
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
  private def oauth1PopupReturn(adnId: String, actorInfoQs: ActorPathQs) = isNodeAdmin(adnId).async { implicit request =>
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
