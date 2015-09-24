package util.acl

import io.suggest.model.n2.node.MNodeTypes
import models.MNode
import models.req.SioReqMd
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Results, Result, ActionBuilder, Request}
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 22:33
 * Description: Гибрид IsSuperuser и чтения произвольного юзера из хранилища по id.
 */

trait IsSuperuserPersonBase extends ActionBuilder[MPersonRequest] {
  /** id юзера. */
  def personId: String

  override def invokeBlock[A](request: Request[A], block: (MPersonRequest[A]) => Future[Result]): Future[Result] = {
    val mpersonOptFut = MNode.getByIdType(personId, MNodeTypes.Person)
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper isSuperuser pwOpt) {
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      mpersonOptFut flatMap {
        case Some(mperson) =>
          srmFut flatMap { srm =>
            val req1 = MPersonRequest(mperson, pwOpt, request, srm)
            block(req1)
          }
        case None =>
          personNotFound(request)
      }

    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }

  /** Юзер не найден. */
  def personNotFound(request: Request[_]): Future[Result] = {
    val res = Results.NotFound("person not exists: " + personId)
    Future successful res
  }
}


/** Дефолтовая реализация [[IsSuperuserPersonBase]]. */
case class IsSuperuserPerson(personId: String)
  extends IsSuperuserPersonBase
  with ExpireSession[MPersonRequest]


/** Экземпляр реквеста. */
case class MPersonRequest[A](
  mperson   : MNode,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
) extends AbstractRequestWithPwOpt[A](request)
