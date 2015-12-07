package util.acl

import models.MNode
import models.event.MEvent
import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 16:26
 * Description: Контроль доступа к событиям.
 */

trait HasNodeEventAccess
  extends OnUnauthNodeCtl
  with IsAdnNodeAdminUtilCtl
{

  import mCommonDi._

  /** Проверка доступа к событию, которое относится к узлу. */
  trait HasNodeEventAccessBase
    extends ActionBuilder[NodeEventRequest]
    with OnUnauthNode
    with IsAdnNodeAdminUtil
  {
    def eventId: String

    /** Нужен ли доступ к кошельку узла и другие функции sioReqMd? */
    def srmFull: Boolean

    /** Разрешить это только для закрывабельных событий? */
    def onlyCloseable: Boolean

    override def invokeBlock[A](request: Request[A], block: (NodeEventRequest[A]) => Future[Result]): Future[Result] = {
      val eventOptFut = MEvent.getById(eventId)
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (pwOpt.isEmpty) {
        forbidden(request, pwOpt)
      } else {
        eventOptFut flatMap {
          // Нет такого события в модели.
          case None =>
            eventNotFound(request, pwOpt)

          // Есть событие и оно подходит под пожелания контроллера.
          case Some(mevent) if !onlyCloseable || mevent.isCloseable =>
            // Для наличия прав на событие нужны права на узел.
            val srmFut = if (srmFull) {
              SioReqMd.fromPwOptAdn(pwOpt, mevent.ownerId)
            } else {
              SioReqMd.fromPwOpt(pwOpt)
            }
            isAdnNodeAdmin(mevent.ownerId, pwOpt) flatMap {
              case Some(adnNode) =>
                // Юзер имеет доступ к узлу. Значит, и к событию узла тоже.
                srmFut flatMap { srm =>
                  val req1 = NodeEventRequest(adnNode, mevent, request, pwOpt, srm)
                  block(req1)
                }

              case None =>
                forbidden(request, pwOpt)
            }

          // Контроллер требует, чтобы флаг isCloseable был выставлен, а клиент хочет обойти это ограничение.
          case Some(mevent) =>
            forbidden(request, pwOpt)
        }
      }
    }

    def forbidden(request: Request[_], pwOpt: PwOpt_t): Future[Result] = {
      onUnauthNode(request, pwOpt)
    }

    def eventNotFound(request: Request[_], pwOpt: PwOpt_t): Future[Result] = {
      val res = NotFound("Event not found: " + eventId)
      Future successful res
    }
  }

  case class HasNodeEventAccess(eventId: String,  srmFull: Boolean = true,  onlyCloseable: Boolean = false)
    extends HasNodeEventAccessBase
    with ExpireSession[NodeEventRequest]

}


abstract class AbstractEventRequest[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mevent: MEvent
}

/** Экземпляр реквеста к экшену управления событием. */
case class NodeEventRequest[A](
  adnNode   : MNode,
  mevent    : MEvent,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
) extends AbstractEventRequest(request)
