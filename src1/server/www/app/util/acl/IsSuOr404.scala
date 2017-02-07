package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser, MReq}
import play.api.mvc.Result

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:56
 * Description: Трейты-аддоны для контроллеров для IsSuperuser or 404.
 */
@Singleton
class IsSuOr404Ctl @Inject() (
                               val isSu   : IsSu,
                               mCommonDi  : ICommonDi
                             ) {

  import mCommonDi._

  trait Base extends isSu.Base with ExpireSession[MReq] {

    override def supOnUnauthResult(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }
  }

}


class IsSuOr404 @Inject() (
                            val isSuOr404Ctl  : IsSuOr404Ctl,
                            val csrf          : Csrf,
                            mCommonDi         : ICommonDi
                          ) {

  abstract class IsSuOr404Abstract
    extends isSuOr404Ctl.Base
    with ExpireSession[MReq]

  object Get extends IsSuOr404Abstract with csrf.Get[MReq]
  object Post extends IsSuOr404Abstract with csrf.Post[MReq]

}


class IsSuOrDevelOr404 @Inject() (
                                   val isSuOr404Ctl : IsSuOr404Ctl,
                                   mCommonDi        : ICommonDi
                                 ) {

  import mCommonDi._

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  object IsSuOrDevelOr404 extends isSuOr404Ctl.Base {
    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || isDev
    }
  }

  @inline
  def apply() = IsSuOrDevelOr404

}
