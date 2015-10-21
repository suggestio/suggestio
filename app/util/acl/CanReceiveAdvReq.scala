package util.acl

import controllers.SioController
import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import util.async.AsyncUtil
import util.di.IDb
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
  with IDb
  with OnUnauthNodeCtl
  with IsAdnNodeAdminUtilCtl
  with OnUnauthUtilCtl
{

  /** Базовая реализация action-builder'ов проверки права на обработку реквестов размещения. */
  trait CanReceiveAdvReqBase
    extends ActionBuilder[RequestWithAdvReq]
    with OnUnauthNode
    with IsAdnNodeAdminUtil
    with OnUnauthUtil
  {

    /** id запрашиваемого adv-запроса. */
    def advReqId: Int

    def _advReqNotFound(request: Request[_]): Future[Result] = {
      Future successful NotFound("adv request not found: " + advReqId)
    }

    override def invokeBlock[A](request: Request[A], block: (RequestWithAdvReq[A]) => Future[Result]): Future[Result] = {
      PersonWrapper.getFromRequest(request) match {
        case pwOpt @ Some(pw) =>
          val advReqOptFut = Future {
            db.withConnection { implicit c =>
              MAdvReq.getById(advReqId)
            }
          }(AsyncUtil.jdbcExecutionContext)
          advReqOptFut flatMap {
            case Some(advReq) =>
              val srmFut = SioReqMd.fromPwOptAdn(pwOpt, advReq.rcvrAdnId)
              isAdnNodeAdmin(advReq.rcvrAdnId, pwOpt) flatMap {
                case Some(adnNode) =>
                  srmFut flatMap { srm =>
                    val req1 = RequestWithAdvReq(request, advReq, adnNode, pwOpt, srm)
                    block(req1)
                  }

                // Юзер не является админом.
                case None =>
                  onUnauthNode(request, pwOpt)
              }

            case None =>
              _advReqNotFound(request)
          }

        case None =>
          onUnauth(request)
      }
    }
  }


  sealed abstract class CanReceiveAdvReqBase2
    extends CanReceiveAdvReqBase
    with ExpireSession[RequestWithAdvReq]

  /** GET Запрос окна обработки запроса размещения. */
  case class CanReceiveAdvReqGet(override val advReqId: Int)
    extends CanReceiveAdvReqBase2
    with CsrfGet[RequestWithAdvReq]

  case class CanReceiveAdvReqPost(override val advReqId: Int)
    extends CanReceiveAdvReqBase2
    with CsrfPost[RequestWithAdvReq]

}


/** Экземпляр одобренного запроса на обработку реквеста размещения рекламной карточки. */
case class RequestWithAdvReq[A](
  request   : Request[A],
  advReq    : MAdvReq,
  rcvrNode  : MNode,
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)


