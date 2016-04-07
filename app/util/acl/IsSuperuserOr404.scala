package util.acl

import models.req.{IReqHdr, ISioUser, MReq}
import play.api.mvc.Result

import scala.concurrent.Future

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

  trait IsSuperuserOr404Base extends IsSuperuserBase with ExpireSession[MReq] {

    override def supOnUnauthResult(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
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

    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || isDev
    }

  }

}
