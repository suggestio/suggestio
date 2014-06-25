package util.acl

import play.api.mvc.{Results, Result, ActionBuilder, Request}
import util.PlayMacroLogsImpl
import util.acl.PersonWrapper.PwOpt_t
import models._
import scala.concurrent.Future
import play.api.Play.current
import play.api.db.DB
import IsAuth.onUnauth
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 17:26
 * Description: Есть ли у юзера admin-доступ к узлам, на которые отправлена на публикацию указанная карточка?
 * На этом этапе неизвестно, с какой стороны на узел смотрит текущий юзер.
 */
object IsRcvrNodeAdm {
  
  def notFoundFut = Future successful Results.NotFound

}

import IsRcvrNodeAdm._


/** Просмотр чьей-то карточки с точки зрения другого узла. Узел может иметь отношение к карточке или нет. */
trait ThirdPartyAdAccessBase extends ActionBuilder[AdRcvrRequest] {
  def adId: String
  def fromAdnId: String
  override protected def invokeBlock[A](request: Request[A], block: (AdRcvrRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val madOptFut = MAd.getById(adId)
        MAdnNodeCache.getById(fromAdnId).flatMap {
          case Some(maybeRcvrNode) =>
            madOptFut flatMap {
              case Some(mad) =>
                val (srmFut, rcvrOpt) = if (IsAdnNodeAdmin.isAdnNodeAdminCheck(maybeRcvrNode, pwOpt)) {
                  // Это узел, которым владеет юзер.
                  val _srmFut = SioReqMd.fromPwOptAdn(pwOpt, fromAdnId)
                  (_srmFut, Some(maybeRcvrNode))
                } else {
                  // Это левый узел, упрощаем всё до предела.
                  val _srmFut = SioReqMd.fromPwOpt(pwOpt)
                  (_srmFut, None)
                }
                srmFut flatMap { srm =>
                  val req1 = AdRcvrRequest(mad, maybeRcvrNode, rcvrOpt, pwOpt, request, srm)
                  block(req1)
                }

              // Эта рекламная карточка не найден.
              case None => notFoundFut
            }

          // from-узел не найден.
          case None => notFoundFut
        }
        
      // Юзер не залогинен вообще.
      case None => onUnauth(request)
    }
  }
}

/**
 * Реализация [[ThirdPartyAdAccessBase]] с поддержкой таймаута сессии.
 * @param adId id рекламной карточки.
 * @param fromAdnId id узла, с точки зрения которого происходит просмотр. Возможно, это подставной узел.
 */
case class ThirdPartyAdAccess(adId: String, fromAdnId: String)
  extends ThirdPartyAdAccessBase
  with ExpireSession[AdRcvrRequest]



case class AdRcvrRequest[A](
  mad: MAd,
  fromAdnNode: MAdnNode,
  rcvrOpt: Option[MAdnNode],
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request) {
  def isRcvrAccess = rcvrOpt.isDefined
}


/** Статические функции логгирования для ACL AdvWndAccess. */
object AdvWndAccess extends PlayMacroLogsImpl

/**
 * Запрос окна с информацией о размещении карточки. Такое окно может запрашивать как создатель карточки,
 * так и узел-ресивер, который модерирует или уже отмодерировал карточку. Такое же окно может появлятся у узла,
 * которому делегировали обязанности модераторации запросов рекламных карточек.
 */
trait AdvWndAccessBase extends ActionBuilder[AdvWndRequest] {
  def adId: String
  def povAdnId: Option[String]
  def needMBB: Boolean

  import AdvWndAccess.LOGGER._

  override def invokeBlock[A](request: Request[A], block: (AdvWndRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        MAd.getById(adId).flatMap {
          case Some(mad) =>
            val producerOptFut = MAdnNodeCache.getById(mad.producerId)
            // Вычислить id ресивера исходя того, что передано в fromAdnId. Если во fromAdnId узел-модератор, то
            val rcvrIdOpt = povAdnId.filter { rcvrId =>
              rcvrId != mad.producerId  &&  DB.withConnection { implicit c =>
                MAdvOk.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)  ||  MAdvReq.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)
              }
            }
            val rcvrOptFut = rcvrIdOpt
              .fold [Future[Option[MAdnNode]]] { Future successful None } { MAdnNodeCache.getById }
            producerOptFut flatMap {
              case Some(producer) =>
                val isProducerAdmin = IsAdnNodeAdmin.isAdnNodeAdminCheckStrict(producer, pwOpt)
                // Пока ресивер ещё не готов, проверяем, относится ли текущая рекламная карточка к указанному ресиверу или продьюсеру.
                if (isProducerAdmin || rcvrIdOpt.isDefined) {
                  // Юзер может смотреть рекламную карточку.
                  rcvrOptFut flatMap { rcvrOpt =>
                    val isRcvrAdmin = rcvrOpt
                      .exists { rcvr => IsAdnNodeAdmin.isAdnNodeAdminCheckStrict(rcvr, pwOpt) }
                    trace(s"[adId=$adId povAdnId=$povAdnId] isProdAdm -> $isProducerAdmin rcvrId -> $rcvrIdOpt isRcvrAdm -> $isRcvrAdmin")
                    // Чтобы получить какой-либо доступ к окошку карточки, нужно быть или админом узла-продьюсера карточки, или же админом ресивера, переданного через fromAdnId.
                    if (isProducerAdmin || isRcvrAdmin) {
                      val srmFut = if (needMBB) {
                        // Узнаём для какого узла отображать кошелёк.
                        val myAdnId: String = if (isProducerAdmin) mad.producerId else if (isRcvrAdmin) rcvrIdOpt.get else ???
                        SioReqMd.fromPwOptAdn(pwOpt, myAdnId)
                      } else {
                        // Контроллер считает, что кошелёк не нужен.
                        SioReqMd.fromPwOpt(pwOpt)
                      }
                      srmFut flatMap { srm =>
                        val req1 = AdvWndRequest(mad, producer, rcvrOpt = rcvrOpt, isProducerAdmin, isRcvrAdmin = isRcvrAdmin, pwOpt, request, srm)
                        block(req1)
                      }
                    } else {
                      warn(s"[adId=$adId] User ${pw.personId} is nor admin of producer (${mad.producerId}), and neither of rcvr ($rcvrIdOpt)")
                      IsAdnNodeAdmin.onUnauth(request)
                    }
                  }
                } else {
                  warn(s"[adId=$adId] User ${pw.personId} is not admin of producer (${mad.producerId}), and rcvr not exists requested")
                  IsAdnNodeAdmin.onUnauth(request)
                }

              // should never occur
              case None =>
                val msg = "Ad producer not found, but it should!"
                error(s"ISE: adId=$adId producerId=${mad.producerId} :: $msg")
                Future successful Results.InternalServerError(msg)
            }

          case None =>
            debug(s"ad not found: adId = " + adId)
            notFoundFut
        }

      case None =>
        trace(s"User is NOT logged in: " + request.remoteAddress)
        IsAuth.onUnauth(request)
    }
  }
}
/**
 * Реализация [[AdvWndAccessBase]] с поддержкой таймаута сессии.
 * @param adId id рекламной карточки.
 * @param povAdnId Опциональная точка зрения на карточку.
 */
case class AdvWndAccess(adId: String, povAdnId: Option[String], needMBB: Boolean)
  extends AdvWndAccessBase
  with ExpireSession[AdvWndRequest]


case class AdvWndRequest[A](
  mad: MAd,
  producer: MAdnNode,
  rcvrOpt: Option[MAdnNode],
  isProducerAdmin: Boolean,
  isRcvrAdmin: Boolean,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request) {
  def isRcvrAccess = rcvrOpt.isDefined
}
