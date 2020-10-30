package util.acl

import io.suggest.sjs.SjsUtil
import javax.inject.Inject
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser, MReq}
import play.api.http.Status
import play.api.mvc.{ActionBuilder, AnyContent, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:56
 * Description: Трейты-аддоны для контроллеров для IsSuperuser or 404.
 */
final class IsSuOr404Ctl @Inject() (
                                     val isSu   : IsSu,
                                     mCommonDi  : ICommonDi
                                   ) {

  import mCommonDi._

  sealed protected[acl] class Base
    extends isSu.Base {

    override protected def _onUnauth(req: IReqHdr): Future[Result] = {
      isSu.logBlockedAccess(req)
      errorHandler.onClientError(req, Status.NOT_FOUND)
    }

  }

}


final class IsSuOr404 @Inject() (
                                  val isSuOr404Ctl  : IsSuOr404Ctl,
                                ) {


  private class ImplC
    extends isSuOr404Ctl.Base

  private val Impl: ActionBuilder[MReq, AnyContent] = {
    new ImplC
  }

  @inline
  def apply(): ActionBuilder[MReq, AnyContent] = {
    Impl
  }

}


final class IsSuOrDevelOr404 @Inject() (
                                         val isSuOr404Ctl : IsSuOr404Ctl,
                                         mCommonDi        : ICommonDi
                                       ) {

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  private class ImplC extends isSuOr404Ctl.Base {
    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || mCommonDi.isDev || SjsUtil.isDevelDetected
    }
  }

  private val Impl: ActionBuilder[MReq, AnyContent] = new ImplC

  @inline
  def apply() = Impl

}
