package util.acl

import play.api.mvc.{Results, Result, ActionBuilder, Request}
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
  override def invokeBlock[A](request: Request[A], block: (AdRcvrRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val madOptFut = MAd.getById(adId)
        MAdnNodeCache.getByIdCached(fromAdnId).flatMap {
          case Some(maybeRcvrNode) =>
            madOptFut flatMap {
              case Some(mad) =>
                // TODO Может стоит как-то запользовать IsAdnNodeAdmin.checkNodeCreds?
                val (srmFut, rcvrOpt) = if (maybeRcvrNode.personIds contains pw.personId) {
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


/**
 * Запрос окна с информацией о размещении карточки. Такое окно может запрашивать как создатель карточки,
 * так и узел-ресивер, который модерирует или уже отмодерировал карточку.
 */
trait AdvWndAccessBase extends ActionBuilder[AdvWndRequest] {
  def adId: String
  def fromAdnId: Option[String]
  def needMBC: Boolean

  override def invokeBlock[A](request: Request[A], block: (AdvWndRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        MAd.getById(adId).flatMap {
          case Some(mad) =>
            val producerOptFut = MAdnNodeCache.getByIdCached(mad.producerId)
            val rcvrIdOpt0 = fromAdnId.filter(_ != mad.producerId)
            val isRcvrRelated = rcvrIdOpt0 exists { rcvrId =>
              DB.withConnection { implicit c =>
                MAdvOk.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)  ||  MAdvReq.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)
              }
            }
            val rcvrIdOpt = rcvrIdOpt0 filter { _ => isRcvrRelated }
            val rcvrOptFut = rcvrIdOpt
              .fold [Future[Option[MAdnNode]]] { Future successful None } { MAdnNodeCache.getByIdCached }
            producerOptFut flatMap {
              case Some(producer) =>
                val isProducerAdmin = IsAdnNodeAdmin.isAdnNodeAdminCheck(producer, pwOpt)
                // Пока ресивер ещё не готов, проверяем, относится ли текущая рекламная карточка к указанному ресиверу или продьюсеру.
                if (isProducerAdmin || isRcvrRelated) {
                  // Юзер может смотреть рекламную карточку.
                  rcvrOptFut flatMap { rcvrOpt =>
                    val isRcvrAdmin = rcvrOpt
                      .exists { rcvr => IsAdnNodeAdmin.isAdnNodeAdminCheck(rcvr, pwOpt) }
                    // Чтобы получить какой-либо доступ к окошку карточки, нужно быть или админом узла-продьюсера карточки, или же админом ресивера, переданного через fromAdnId.
                    if (isProducerAdmin || isRcvrAdmin) {
                      val srmFut = if (needMBC) {
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
                      IsAdnNodeAdmin.onUnauth(request)
                    }
                  }
                } else {
                  IsAdnNodeAdmin.onUnauth(request)
                }

              case None =>
                Future successful Results.InternalServerError("Ad producer not found, but it should!")
            }

          case None => notFoundFut
        }

      case None => IsAuth.onUnauth(request)
    }
  }
}
/**
 * Реализация [[AdvWndAccessBase]] с поддержкой таймаута сессии.
 * @param adId id рекламной карточки.
 * @param fromAdnId Опциональная точка зрения на карточку.
 */
case class AdvWndAccess(adId: String, fromAdnId: Option[String], needMBC: Boolean)
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
