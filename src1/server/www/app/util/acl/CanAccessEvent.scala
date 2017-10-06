package util.acl

import javax.inject.Inject

import io.suggest.util.logs.MacroLogsImpl
import models.event.MEvents
import models.mproj.ICommonDi
import models.req.{IReq, MNodeEventReq, MReq, MUserInit}
import play.api.mvc._
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.req.ReqUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 16:26
 * Description: Контроль доступа к событиям.
 */

class CanAccessEvent @Inject() (
                                 aclUtil                : AclUtil,
                                 isNodeAdmin            : IsNodeAdmin,
                                 mEvents                : MEvents,
                                 reqUtil                : ReqUtil,
                                 mCommonDi              : ICommonDi
                               )
  extends MacroLogsImpl
{ outer =>

  import mCommonDi._


  /** Собрать ActionBuilder.
    *
    * @param eventId id обрабатываемого события.
    * @param onlyCloseable Разрешить это только для закрывабельных событий?
    * @return
    */
  def apply(eventId          : String,
            onlyCloseable    : Boolean,
            userInits        : MUserInit*): ActionBuilder[MNodeEventReq, AnyContent] = {

    val userInits1 = userInits
    new reqUtil.SioActionBuilderImpl[MNodeEventReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MNodeEventReq[A]) => Future[Result]): Future[Result] = {
        val eventOptFut = mEvents.getById(eventId)

        val user = aclUtil.userFromRequest(request)

        maybeInitUser(user)

        val reqErr = MReq(request, user)

        user.personIdOpt.fold ( forbidden(reqErr) ) { _ =>
          eventOptFut.flatMap {
            // Нет такого события в модели.
            case None =>
              eventNotFound(reqErr)

            // Есть событие и оно подходит под пожелания контроллера.
            case Some(mevent) if !onlyCloseable || mevent.isCloseable =>
              // Для наличия прав на событие нужны права на узел.
              maybeInitUser(user)

              isNodeAdmin.isAdnNodeAdmin(mevent.ownerId, user).flatMap {
                case Some(mnode) =>
                  // Юзер имеет доступ к узлу. Значит, и к событию узла тоже.
                  val req1 = MNodeEventReq(mevent, mnode, request, user)
                  block(req1)

                case None =>
                  LOGGER.warn(s"$toString Not a event owner node admin: ${mevent.ownerId}")
                  forbidden(reqErr)
              }

            // Контроллер требует, чтобы флаг isCloseable был выставлен, а клиент хочет обойти это ограничение.
            case _: Some[_] =>
              LOGGER.warn(s"$toString event isCloseable conflicted, 403")
              forbidden(reqErr)
          }
        }
      }

      def forbidden(req: IReq[_]): Future[Result] = {
        isNodeAdmin.onUnauthNode(req)
      }

      def eventNotFound(req: IReq[_]): Future[Result] = {
        val res = Results.NotFound("Event not found: " + eventId)
        res
      }

    }

  }

}

