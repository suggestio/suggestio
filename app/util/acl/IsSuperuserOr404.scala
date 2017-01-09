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

  trait IsSuOr404Base extends IsSuBase with ExpireSession[MReq] {

    override def supOnUnauthResult(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }
  }

}


trait IsSuperuserOr404 extends IsSuperuserOr404Ctl {

  abstract class IsSuOr404Abstract extends IsSuOr404Base with ExpireSession[MReq]
  object IsSuOr404 extends IsSuOr404Abstract
  object IsSuOr404Get extends IsSuOr404Abstract with CsrfGet[MReq]
  object IsSuOr404Post extends IsSuOr404Abstract with CsrfPost[MReq]

}


trait IsSuperuserOrDevelOr404 extends IsSuperuserOr404Ctl {

  import mCommonDi._

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  object IsSuOrDevelOr404 extends IsSuOr404Base {

    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || isDev
    }
  }

}
