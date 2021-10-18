package util.pay.yookassa

import io.suggest.bill.MPrice
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNode
import io.suggest.pay.yookassa.{MYkAmount, MYkPayment, MYkPaymentConfirmation, MYkPaymentConfirmationTypes, MYkPaymentCreate, MYkPaymentStatuses, YooKassaConst}
import io.suggest.proto.http.HttpConst
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import play.api.Configuration
import play.api.inject.Injector
import play.api.libs.ws.{WSAuthScheme, WSClient}
import util.TplDataFormatUtil
import japgolly.univeq._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsObject, Json}

import java.security.MessageDigest
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/** Utilities for YooKassa API interaction. */
final class YooKassaUtil @Inject() (
                                     injector: Injector,
                                   )
  extends MacroLogsImpl
{

  private lazy val wsClient = injector.instanceOf[WSClient]
  private lazy val configuration = injector.instanceOf[Configuration]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Force time-based idempotence key rotation.
    * This is needed to prevent duplication for continious payment requests. */
  private def IDEMPOTENCE_KEY_ROTATE_EVERY_SECONDS = 20


  /** Read profiles from configuration. */
  private lazy val yooProfiles: Seq[YooKassaProfile] = {
    (for {
      profilesConf <- configuration
        .getOptional[Seq[Configuration]]( "pay.yookassa.profiles" )
        .iterator
      profileConf <- profilesConf
    } yield {
      YooKassaProfile(
        shopId    = profileConf.get[String]("shop_id"),
        secretKey = profileConf.get[String]("secret"),
        isTest    = profileConf.get[Boolean]("test"),
      )
    })
      .to( LazyList )
  }

  def firstProfile(isTest: Boolean): Option[YooKassaProfile] =
    yooProfiles.find(_.isTest ==* isTest)
  def firstProfile: Option[YooKassaProfile] =
    yooProfiles.headOption


  /** Zero-step of payment procedure: seed remote PaySystem with payment data.
    *
    * @param profile Payment system profile.
    * @param orderItem Cart order contents.
    * @param payPrice Pricing for pay.
    * @param personOpt Payer (person node).
    * @param ctx Rendering context.
    * @return Future with parsed pay-system response.
    */
  def preparePayment(profile: YooKassaProfile, orderItem: MOrderWithItems, payPrice: MPrice, personOpt: Option[MNode])
                    (implicit ctx: Context): Future[YooKassaPaymentPrepareResult] = {
    val orderId = orderItem.order.id.get
    lazy val logPrefix = s"startPayment(order#$orderId/+${orderItem.items.length}, U#${ctx.user.personIdOpt.orNull}):"
    LOGGER.trace(s"$logPrefix Starting, $profile, price=$payPrice")

    // Assemble request body for new payment start:
    val payCreate = MYkPaymentCreate(
      amount = MYkAmount(
        value     = TplDataFormatUtil.formatPriceAmountPlain( payPrice ),
        currency  = payPrice.currency,
      ),
      description = Some( ctx.messages( MsgCodes.`Payment.for.order.N`, orderId ) ),
      capture = Some(true),

      // merchantCustomerId: Use readable phone number here or unreadable nodeId.
      merchantCustomerId = (for {
        mnode <- personOpt.iterator
        phoneEdge <- mnode.edges.withPredicateIter( MPredicates.Ident.Phone )
        if phoneEdge.info.flag getOrElse true
        phoneNumber <- phoneEdge.nodeIds
      } yield {
        phoneNumber
      })
        .nextOption()
        // TODO Don't using email here, because we need to debug email validation.
        .orElse {
          // Return person node ID, because no phone number found.
          ctx.user.personIdOpt
        },
      // Embedded: returns embedded web-widget for sio-payment page.
      confirmation = Some {
        MYkPaymentConfirmation(
          // TODO This should be defined outside here (in http-controller)
          pcType = MYkPaymentConfirmationTypes.Embedded,
        )
      },
    )

    // Generate idempotence key via hashing some data from. Use base64 to minify length (instead of HEX-encoding):
    // LineLen=0, SEP=[], because do not add CR-LF at the end here.
    val hashStr = new Base64(0, Array.empty, true)
      .encodeToString {
        // idempotence key to mark duplicate requests.
        val sha1 = MessageDigest.getInstance("SHA-1")
        // Append current cart-order metadata:
        sha1.update( s"$orderId ${System.currentTimeMillis() / IDEMPOTENCE_KEY_ROTATE_EVERY_SECONDS}\n".getBytes() )
        for {
          itm <- orderItem.items
            .sortBy( _.id getOrElse -1L )
        } {
          val itmStr = s"${itm.id.getOrElse("")} ${itm.price} ${itm.geoShape.getOrElse("")} ${itm.nodeId} ${itm.rcvrIdOpt.getOrElse("")} ${itm.tagFaceOpt.getOrElse("")}\n"
          sha1.update( itmStr.getBytes )
        }
        sha1.update( payPrice.toString.getBytes )

        // Append person node id to original hash:
        personOpt
          .flatMap( _.id )
          .orElse( ctx.user.personIdOpt )
          .foreach { personId =>
            sha1.update( personId.getBytes )
          }

        sha1.digest()
      }
    // Append order id to original hash (outside the hash):
    val idemptKey = hashStr + "/" + orderId
    val requestBodyJson = Json.toJsObject(payCreate)

    LOGGER.trace(s"$logPrefix Preparing HTTP request to yookassa/payments:\n Profile = $profile\n IdempotenceKey = ${idemptKey}\n-----------------------\n${Json.prettyPrint(requestBodyJson)}\n----------------------")

    for {
      // Start HTTP-request:
      resp <- wsClient
        .url( "https://api.yookassa.ru/v3/payments" )
        .withAuth(
          username = profile.shopId,
          password = profile.secretKey,
          scheme   = WSAuthScheme.BASIC,
        )
        .addHttpHeaders(
          HttpConst.Headers.IDEMPOTENCE_KEY -> idemptKey
        )
        .post( requestBodyJson )

      // Process HTTP response from pay-system:
      _ = {
        LOGGER.trace(s"$logPrefix Response from pay system:\n status = ${resp.status} ${resp.statusText}\n--------------------------\n${Json.prettyPrint(resp.json)}\n-------------------------")
        val r = resp.status ==* 200
        if (!r) throw new RuntimeException(s"Unexpected payments HTTP response: ${resp.status} ${resp.statusText}:\n ${resp.body}")
      }
      ykPayment = resp.json.as[MYkPayment]

      _ = {
        val pendingStatus = MYkPaymentStatuses.Pending
        val r = ykPayment.status ==* pendingStatus
        if (!r) throw new IllegalArgumentException(s"Unexpected yk-payment status: ${ykPayment.status}, but expected $pendingStatus")
      }
    } yield {
      YooKassaPaymentPrepareResult(
        metadata = Json.obj(
          YooKassaConst.Metadata.WEB_WIDGET_ARGS -> Json.obj(
            // https://yookassa.ru/developers/payment-forms/widget/quick-start#process-initialize-widget
            "confirmation_token" -> ykPayment.confirmation.get.confirmationToken.get,
          ),
        ),
        ykPayment,
      )
    }
  }

}


protected final case class YooKassaProfile(
                                            shopId    : String,
                                            private[yookassa] val secretKey : String,
                                            isTest    : Boolean,
                                          ) {

  override def toString: String =
    s"${productPrefix}($shopId,test?$isTest)"

}


/** Prepare payment result for [[YooKassaUtil.preparePayment()]]. */
protected case class YooKassaPaymentPrepareResult(
                                                   metadata  : JsObject,
                                                   payment   : MYkPayment,
                                                 )
