package util.acl

import play.api.mvc.{RequestHeader, Result}
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future
import play.api.Play.isDev

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:56
 * Description: Трейты-аддоны для контроллеров для IsSuperuser or 404.
 */
trait IsSuperuserOr404Ctl
  extends IsSuperuser
{
  import mCommonDi._

  trait IsSuperuserOr404Base extends IsSuperuserBase with ExpireSession[AbstractRequestWithPwOpt] {
    override def supOnUnauthResult(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
      errorHandler.http404Fut(request)
    }
  }

}


trait IsSuperuserOr404 extends IsSuperuserOr404Ctl {

  object IsSuperuserOr404 extends IsSuperuserOr404Base

}


trait IsSuperuserOrDevelOr404 extends IsSuperuserOr404Ctl {

  import mCommonDi._

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  object IsSuperuserOrDevelOr404 extends IsSuperuserOr404Base {
    override protected def isAllowed(pwOpt: PwOpt_t): Boolean = {
      super.isAllowed(pwOpt) || isDev
    }
  }

}
