package util.acl

import io.suggest.bill.cart.{MCartSubmitQs, MPayableVia}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses}
import io.suggest.pay.MPaySystems
import io.suggest.playx.AppModeExt
import io.suggest.util.logs.MacroLogsImpl
import models.req.{IReq, IReqHdr, MNodeOptOrderReq, MUserInit}
import play.api.Application
import play.api.inject.Injector
import util.pay.yookassa.YooKassaUtil
import japgolly.univeq._
import play.api.http.{HttpErrorHandler, Status}
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, ActionFilter, AnyContent, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CanSubmitCart @Inject()(
                               injector       : Injector,
                             )
  extends MacroLogsImpl
{

  private lazy val canViewOrder = injector.instanceOf[CanViewOrder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val current = injector.instanceOf[Application]
  private lazy val yooKassaUtil = injector.instanceOf[YooKassaUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]


  private lazy val testPayForAll = current.configuration.getOptional[Boolean]("pay.test4all").getOrElseFalse

  /** ACL function for filtering user for allowed pay-methods. */
  def isPayableViaUser(payVia: MPayableVia)(implicit request: IReqHdr): Boolean = {
    if (payVia.isTest) {
      val isSuperUser = request.user.isSuper
      val r = isSuperUser ||
        (request.user.isAuth && (!current.mode.isProd || testPayForAll))

      LOGGER.trace(s"_isPayableViaUser(${request.user.personIdOpt.orNull}): allow test?$r, because user.isSuper?$isSuperUser and appMode.isProd?${current.mode.isProd} testPay4All?$testPayForAll")
      r
    } else {
      true
    }
  }


  /** Collect allowed pay methods. */
  def getPayableVias(orderOpt: Option[MOrder])(implicit request: IReqHdr): Seq[MPayableVia] = {
    lazy val logPrefix = s"_getPayableVias(${orderOpt.flatMap(_.id).orNull}):"
    Option.when {
      // Empty order returning as not payable.
      orderOpt.fold( false )(_.status ==* MOrderStatuses.Draft)
    } {
      (for {
        paySystem <- MPaySystems.values.iterator
        payVia <- paySystem match {
          case MPaySystems.YooKassa =>
            yooKassaUtil.payableVias
          case other =>
            LOGGER.trace(s"$logPrefix Not payable system: $other")
            Nil
        }
      } yield {
        payVia
      })
        .distinct
        .filter( isPayableViaUser )
        .toSeq
        .sorted
    }
      .getOrElse( Nil )
  }


  def canUnholdOrder( orderId: Gid_t ): ActionBuilder[MNodeOptOrderReq, AnyContent] = {
    canViewOrder( orderId, onNodeId = None )
      .andThen {
        new ActionFilter[MNodeOptOrderReq] {
          override protected def filter[A](request: MNodeOptOrderReq[A]): Future[Option[Result]] = {
            val isHold = (request.morder.status ==* MOrderStatuses.Hold)
            if (isHold) {
              // Continue processing, as expected...
              Future successful None

            } else {
              LOGGER.warn( s"canUnholdOrder($orderId): Can't do it, because order status is ${request.morder.status} since ${request.morder.dateStatus}" )
              for {
                result <- httpErrorHandler.onClientError(request, Status.PRECONDITION_FAILED, s"Order#${orderId} is not hold.")
              } yield {
                Some( result )
              }
            }
          }
          override protected def executionContext = ec
        }
      }
  }



  def apply(qs: MCartSubmitQs, inits: MUserInit*): ActionBuilder[IReq, AnyContent] = {
    qs.onNodeId
      .fold[ActionBuilder[IReq, AnyContent]] {
        isAuth()
      } { onNodeId =>
        isNodeAdmin( onNodeId, inits: _* )
      }
      // Check/validate qs.payVia:
      .andThen {
        lazy val logPrefix = s"($qs):"
        new ActionFilter[IReq] {
          override protected def filter[A](request: IReq[A]): Future[Option[Result]] = {
            if ( isPayableViaUser( qs.payVia )(request) ) {
              Future successful None
            } else {
              val msg = s"User#${request.user.personIdOpt.orNull} cannot pay with ${Json.toJson(qs.payVia).toString()}"
              LOGGER.warn(s"$logPrefix $msg")
              httpErrorHandler
                .onClientError( request, Status.FORBIDDEN, msg )
                .map( Some.apply )
            }
          }
          override protected def executionContext = ec
        }
      }
  }


}
