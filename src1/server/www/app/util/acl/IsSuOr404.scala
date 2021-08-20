package util.acl

import io.suggest.playx._
import io.suggest.sjs.SjsUtil

import javax.inject.Inject
import models.req.{IReqHdr, ISioUser, MReq}
import play.api.Application
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:56
 * Description: Трейты-аддоны для контроллеров для IsSuperuser or 404.
 */
final class IsSuOr404Ctl @Inject() (
                                     injector   : Injector,
                                   ) {


  val isSu = injector.instanceOf[IsSu]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]

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
                                         injector         : Injector,
                                         isSuOr404Ctl     : IsSuOr404Ctl,
                                       ) {

  private lazy val current = injector.instanceOf[Application]

  /** Разрешить не-админам и анонимам доступ в devel-режиме. */
  private class ImplC extends isSuOr404Ctl.Base {
    override protected def isAllowed(user: ISioUser): Boolean = {
      super.isAllowed(user) || current.mode.isDev || SjsUtil.isDevelDetected
    }
  }

  private val Impl: ActionBuilder[MReq, AnyContent] = new ImplC

  @inline
  def apply() = Impl

}
