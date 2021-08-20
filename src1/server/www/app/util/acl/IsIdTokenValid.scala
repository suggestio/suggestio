package util.acl

import io.suggest.mbill2.m.ott.MOneTimeTokens
import io.suggest.model.SlickHolder
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl

import javax.inject.Inject
import models.req.{IReqHdr, MIdTokenReq}
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import util.ident.IdTokenUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.19 11:49
  * Description: ACL с поддержкой поверхностной проверкой id-токена.
  */
final class IsIdTokenValid @Inject()(
                                      injector: Injector,
                                    )
  extends MacroLogsImpl
{

  private lazy val idTokenUtil = injector.instanceOf[IdTokenUtil]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val mOneTimeTokens = injector.instanceOf[MOneTimeTokens]
  private lazy val slickHolder = injector.instanceOf[SlickHolder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import slickHolder.slick

  private def _invalidToken(req: IReqHdr): Future[Result] = {
    httpErrorHandler.onClientError(req, Status.BAD_REQUEST, "error.invalid")
  }


  /** Убедится, что токен свежий. Т.е. без idMsg и без ott.
    *
    * @param mreq Собранные данные реквеста.
    * @return true, если токен разрешается.
    */
  def isFreshToken(mreq: MIdTokenReq[_]): Boolean = {
    lazy val logPrefix = s"isFreshToken(${mreq.idToken.ottId}):"

    val r0 = {
      val r = mreq.ottOpt.isEmpty
      if (!r) LOGGER.warn(s"$logPrefix OTT already exist, unexpectedly for fresh-token.\n ${mreq.ottOpt.orNull}")
      r
    }

    r0 && {
      val r = mreq.idToken.idMsgs.isEmpty
      if (!r) LOGGER.warn(s"$logPrefix Token contains idMsgs, not fresh?\n ${mreq.idToken.idMsgs.mkString(",\n ")}")
      r
    }
  }


  /** ActionBuilder, выверяющий id-токен из аргументов метода-конструктора.
    *
    * @param token Зашифрованный id-токен.
    * @return ActionBuilder.
    */
  def apply(token: String)(validating: MIdTokenReq[_] => Boolean): ActionBuilder[MIdTokenReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MIdTokenReq] {

      override def invokeBlock[A](request: Request[A], block: MIdTokenReq[A] => Future[Result]): Future[Result] = {
        val req0 = aclUtil.reqFromRequest(request)
        lazy val logPrefix = s"(${token.hashCode}${req0.user.personIdOpt.fold("")(" u#" + _)})#${System.currentTimeMillis()}:"

        idTokenUtil
          .decrypt( token )
          .transformWith {
            case Success(idToken) =>
              if ( idTokenUtil.isConstaintsMeetRequest( idToken )(req0) ) {

                LOGGER.trace(s"$logPrefix idToken from ip#${req0.remoteClientAddress}:\n $idToken")
                (for {
                  mottOpt <- slick.db.run {
                    mOneTimeTokens.getById( idToken.ottId )
                  }
                  mreq2 = MIdTokenReq(
                    idToken = idToken,
                    ottOpt  = mottOpt,
                    user    = req0.user,
                    request = request,
                  )
                  if validating( mreq2 )
                } yield {
                  mreq2
                }).transformWith {
                  case Success(mreq2) =>
                    block(mreq2)
                  case Failure(ex) =>
                    if (!ex.isInstanceOf[NoSuchElementException])
                      LOGGER.warn(s"$logPrefix Failed to validate token", ex)
                    _invalidToken( req0 )
                }

              } else {
                LOGGER.warn(s"$logPrefix idToken out of date.")
                _invalidToken(req0)
              }

            case Failure(ex) =>
              LOGGER.warn(s"$logPrefix idToken not parsed, ip#${req0.remoteClientAddress}", ex)
              _invalidToken(req0)
          }
      }

    }
  }

}
