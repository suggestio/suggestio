package io.suggest.bill.cart.c

import io.suggest.bill.cart.{MCartSubmitQs, MCartSubmitResult, MOrderContent}
import io.suggest.proto.http.client.HttpClient
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.routes.routes
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.proto.http.model._
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

import scala.scalajs.js.JSConverters._
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:41
  * Description: API for server interactions about cart & cart items.
  */
trait ILkCartApi {

  /** Ask server about order contents.
    *
    * @param orderId Order id.
    *                None means current user's Cart order.
    * @return Future with order contents.
    */
  def getOrder(orderId: Option[Gid_t]): Future[MOrderContent]

  /** Remove items from current user's Cart order.
    *
    * @param itemIds Item ids to be deleted.
    * @return Future with updated cart order contents.
    */
  def deleteItems(itemIds: Iterable[Gid_t]): Future[MOrderContent]

  /** Submit current cart into payment processing.
    * @return Future with cart submission result.
    */
  def cartSubmit( qs: MCartSubmitQs ): Future[MCartSubmitResult]

  /** Undo order hold status (interrupt order payment flow). */
  def unHoldOrder(orderId: Gid_t): Future[MOrderContent]

}


/** Bill-Cart API implementation over HTTP Fetch/XHR. */
final class LkCartApiHttpImpl extends ILkCartApi {

  override def getOrder(orderId: Option[Gid_t]): Future[MOrderContent] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkBill2.getOrder(
          orderId = orderId.mapDefined(_.toDouble)
        ),
        data = HttpReqData.justAcceptJson
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MOrderContent]
  }

  override def deleteItems(itemIds: Iterable[Gid_t]): Future[MOrderContent] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkBill2.deleteItems(
          itemIds = itemIds.iterator.map(_.toDouble).toJSArray
        ),
        data = HttpReqData.justAcceptJson
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MOrderContent]
  }

  override def cartSubmit(qs: MCartSubmitQs ): Future[MCartSubmitResult] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkBill2.cartSubmit(
          qs = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject( qs ) ),
        ),
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MCartSubmitResult]
  }

  override def unHoldOrder(orderId: Gid_t): Future[MOrderContent] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkBill2.unHoldOrder( orderId.toDouble ),
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MOrderContent]
  }

}
