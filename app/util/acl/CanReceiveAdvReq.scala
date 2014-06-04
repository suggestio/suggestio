package util.acl

import play.api.mvc.{Results, Result, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.Play.current
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 16:23
 * Description: Может ли текущий юзер обрабатывать заявки на размещение рекламы?
 * Да, если юзер является админом соответствующего rcvr-узла.
 */
case class CanReceiveAdvReq(advReqId: Int) extends ActionBuilder[RequestWithAdvReq] {
  override def invokeBlock[A](request: Request[A], block: (RequestWithAdvReq[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val advReqOpt = DB.withConnection { implicit c =>
          MAdvReq.getById(advReqId)
        }
        advReqOpt match {
          case Some(advReq) =>
            val srmFut = SioReqMd.fromPwOptAdn(pwOpt, advReq.rcvrAdnId)
            IsAdnNodeAdmin.isAdnNodeAdmin(advReq.rcvrAdnId, pwOpt) flatMap {
              case Some(adnNode) =>
                srmFut flatMap { srm =>
                  val req1 = RequestWithAdvReq(request, advReq, adnNode, pwOpt, srm)
                  block(req1)
                }

              // Юзер не является админом.
              case None =>
                IsAdnNodeAdmin.onUnauth(request)
            }

          case None => Future successful Results.NotFound
        }

      case None =>
        IsAuth.onUnauth(request)
    }
  }
}

case class RequestWithAdvReq[A](request: Request[A], advReq: MAdvReq, rcvrNode: MAdnNode, pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)
