package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser, MReq}
import play.api.mvc.{ActionBuilder, Result}

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

  sealed class Base
    extends isSu.Base {

    override protected def _onUnauth(req: IReqHdr): Future[Result] = {
      isSu.logBlockedAccess(req)
      errorHandler.http404Fut(req)
    }

  }

}


class IsSuOr404 @Inject() (
                            val isSuOr404Ctl  : IsSuOr404Ctl,
                            mCommonDi         : ICommonDi
                          ) {


  class ImplC
    extends isSuOr404Ctl.Base

  val Impl = new ImplC

  @inline
  def apply(): ActionBuilder[MReq] = {
    Impl
  }

}


class IsSuOrDevelOr404 @Inject() (
                                   val isSuOr404Ctl : IsSuOr404Ctl,
                                   mCommonDi        : ICommonDi
                                 ) {

  import mCommonDi._

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  class ImplC extends isSuOr404Ctl.Base {
    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || isDev
    }
  }

  val Impl = new ImplC

  @inline
  def apply() = Impl

}
