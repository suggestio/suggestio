package util.acl

import models.event.IMEvents
import models.req.{IReq, MNodeEventReq, MReq, MUserInit}
import play.api.mvc.{ActionBuilder, Request, Result, Results}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 16:26
 * Description: Контроль доступа к событиям.
 */

trait CanAccessEvent
  extends IsAdnNodeAdminUtilCtl
  with Csrf
  with IMEvents
{

  import mCommonDi._

  /** Проверка доступа к событию, которое относится к узлу. */
  trait HasNodeEventAccessBase
    extends ActionBuilder[MNodeEventReq]
    with OnUnauthNode
    with IsAdnNodeAdminUtil
    with InitUserCmds
  {
    def eventId: String

    /** Разрешить это только для закрывабельных событий? */
    def onlyCloseable: Boolean

    override def invokeBlock[A](request: Request[A], block: (MNodeEventReq[A]) => Future[Result]): Future[Result] = {
      val eventOptFut = mEvents.getById(eventId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      maybeInitUser(user)

      val reqErr = MReq(request, user)
      personIdOpt.fold ( forbidden(reqErr) ) { personId =>
        eventOptFut flatMap {
          // Нет такого события в модели.
          case None =>
            eventNotFound(reqErr)

          // Есть событие и оно подходит под пожелания контроллера.
          case Some(mevent) if !onlyCloseable || mevent.isCloseable =>
            // Для наличия прав на событие нужны права на узел.
            maybeInitUser(user)

            isAdnNodeAdmin(mevent.ownerId, user) flatMap {
              case Some(mnode) =>
                // Юзер имеет доступ к узлу. Значит, и к событию узла тоже.
                val req1 = MNodeEventReq(mevent, mnode, request, user)
                block(req1)

              case None =>
                LOGGER.warn("Not a event owner node admin: " + mevent.ownerId)
                forbidden(reqErr)
            }

          // Контроллер требует, чтобы флаг isCloseable был выставлен, а клиент хочет обойти это ограничение.
          case Some(mevent) =>
            LOGGER.warn("event isCloseable conflicted, 403")
            forbidden(reqErr)
        }
      }
    }

    def forbidden(req: IReq[_]): Future[Result] = {
      onUnauthNode(req)
    }

    def eventNotFound(req: IReq[_]): Future[Result] = {
      val res = Results.NotFound("Event not found: " + eventId)
      Future successful res
    }
  }

  abstract class HasNodeEventAccessAbstract
    extends HasNodeEventAccessBase
    with ExpireSession[MNodeEventReq]


  case class HasNodeEventAccess(
    override val eventId          : String,
    override val onlyCloseable    : Boolean,
    override val userInits        : MUserInit*
  )
    extends HasNodeEventAccessAbstract

  case class HasNodeEventAccessGet(
    override val eventId          : String,
    override val onlyCloseable    : Boolean,
    override val userInits        : MUserInit*
  )
    extends HasNodeEventAccessAbstract
    with CsrfGet[MNodeEventReq]

  case class HasNodeEventAccessPost(
    override val eventId          : String,
    override val onlyCloseable    : Boolean,
    override val userInits        : MUserInit*
  )
    extends HasNodeEventAccessAbstract
    with CsrfPost[MNodeEventReq]

}

