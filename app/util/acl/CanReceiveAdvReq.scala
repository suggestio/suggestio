package util.acl

import controllers.SioController
import models.adv.MAdvReq
import models.req.{MNodeAdvReqReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 16:23
 * Description: Может ли текущий юзер обрабатывать заявки на размещение рекламы?
 * Да, если юзер является админом соответствующего rcvr-узла.
 */
trait CanReceiveAdvReq
  extends SioController
  with OnUnauthNodeCtl
  with IsAdnNodeAdminUtilCtl
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Базовая реализация action-builder'ов проверки права на обработку реквестов размещения. */
  trait CanReceiveAdvReqBase
    extends ActionBuilder[MNodeAdvReqReq]
    with OnUnauthNode
    with IsAdnNodeAdminUtil
    with OnUnauthUtil
  {

    /** id запрашиваемого adv-запроса. */
    def advReqId: Int

    def _advReqNotFound(request: Request[_]): Future[Result] = {
      NotFound("adv request not found: " + advReqId)
    }

    override def invokeBlock[A](request: Request[A], block: (MNodeAdvReqReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold {
        onUnauth(request)
      } { personId =>
        val advReqOptFut = Future {
          db.withConnection { implicit c =>
            MAdvReq.getById(advReqId)
          }
        }(AsyncUtil.jdbcExecutionContext)

        val user = mSioUsers(personIdOpt)

        advReqOptFut flatMap {
          case Some(advReq) =>
            isAdnNodeAdmin(advReq.rcvrAdnId, user) flatMap {
              case Some(mnode) =>
                val req1 = MNodeAdvReqReq(advReq, mnode, request, user)
                block(req1)

              // Юзер не является админом.
              case None =>
                val req1 = MReq(request, user)
                onUnauthNode(req1)
            }

          case None =>
            _advReqNotFound(request)
        }
      }
    }
  }


  sealed abstract class CanReceiveAdvReqBase2
    extends CanReceiveAdvReqBase
    with ExpireSession[MNodeAdvReqReq]

  /** GET Запрос окна обработки запроса размещения. */
  case class CanReceiveAdvReqGet(override val advReqId: Int)
    extends CanReceiveAdvReqBase2
    with CsrfGet[MNodeAdvReqReq]

  case class CanReceiveAdvReqPost(override val advReqId: Int)
    extends CanReceiveAdvReqBase2
    with CsrfPost[MNodeAdvReqReq]

}
