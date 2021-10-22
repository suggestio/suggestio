package util.pay.yookassa

import inet.ipaddr.IPAddressString
import io.suggest.bill.MPrice
import io.suggest.err.HttpResultingException
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.mbill2.m.txn.MTxnTypes
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNode
import io.suggest.pay.MPaySystems
import io.suggest.pay.yookassa.{MYkAmount, MYkEventTypes, MYkObject, MYkObjectTypes, MYkPayment, MYkPaymentConfirmation, MYkPaymentConfirmationTypes, MYkPaymentCreate, MYkPaymentStatuses, YooKassaConst}
import io.suggest.proto.http.HttpConst
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import play.api.Configuration
import play.api.libs.ws.{WSAuthScheme, WSClient}
import util.TplDataFormatUtil
import japgolly.univeq._
import models.req.IReq
import org.apache.commons.codec.binary.Base64
import play.api.http.{HeaderNames, HttpErrorHandler, MimeTypes}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result, Results}
import util.acl.SioControllerApi
import util.billing.{Bill2Util, BillDebugUtil}

import java.security.MessageDigest
import javax.inject.Inject
import scala.concurrent.Future


/** Utilities for YooKassa API interaction. */
final class YooKassaUtil @Inject() (
                                     sioControllerApi: SioControllerApi,
                                   )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import slickHolder.slick

  private lazy val wsClient = injector.instanceOf[WSClient]
  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]

  private lazy val bill2Util = injector.instanceOf[Bill2Util]
  private lazy val billDebugUtil = injector.instanceOf[BillDebugUtil]

  /** Force time-based idempotence key rotation.
    * This is needed to prevent duplication for continious payment requests. */
  private def IDEMPOTENCE_KEY_ROTATE_EVERY_SECONDS = 20

  // Validate remote client address as in documentation https://yookassa.ru/developers/using-api/webhooks#ip
  /** IPv4 YooKassa addrs, according to documentation. */
  def YK_NOTIFY_FROM_IPV4_ADDRS = {
    "185.71.76.0/27" ::
    "185.71.77.0/27" ::
    "77.75.153.0/25" ::
    "77.75.154.128/25" ::
    Nil
  }

  /** IPv6 YooKassa addrs, according to documentation. */
  def YK_NOTIFY_FROM_IPV6_ADDRS = {
    "2a02:5180:0:1509::/64" ::
    "2a02:5180:0:2655::/64" ::
    "2a02:5180:0:1533::/64" ::
    "2a02:5180:0:2669::/64" ::
    Nil
  }


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


  /** Handle, validate, verify, process and reply to incoming HTTP-notification from external service (payment system).
    *
    * @param method Request method verb.
    * @param restPath URL sub-path.
    * @param request HTTP request instance.
    * @return Future with result.
    */
  def handleIncomingHttpRequest(method: String, restPath: String)
                               (implicit request: IReq[AnyContent]): Future[Result] = {
    restPath match {
      case "notify" =>
        handleHttpNotify()
      case other =>
        httpErrorHandler.onClientError(request, NOT_FOUND, s"- $other - invalid REST endpoint for paySystem[${MPaySystems.YooKassa}]")
    }
  }


  /** Handle incoming HTTP-notifications from yookassa.ru. */
  def handleHttpNotify()(implicit request: IReq[AnyContent]): Future[Result] = {
    lazy val logPrefix = s"handleHttpNotify()#${System.currentTimeMillis}:"

    (for {
      // Parse remote client IP-address:
      remoteIpAddr <- {
        val ipTry = request.remoteClientIpTry
        ipTry.fold(
          {ex =>
            LOGGER.error(s"$logPrefix No valid ip-address found in request. Nginx configuration bug? remoteClientAddr=${request.remoteClientAddress} remoteAddr=${request.remoteAddress}: (${ex.getClass.getSimpleName}: ${ex.getMessage})")
            None
          },
          {ipAddr =>
            LOGGER.trace(s"$logPrefix Remote ip-address parsed: ${ipAddr} v4?${ipAddr.isIPv4} local?${ipAddr.isLocal}")
            Some( ipAddr )
          }
        )
      }

      // Validate remote IP according to YooKassa-documentation allowed subnets.
      if {
        val ipSubnetsAllowed = {
          if (remoteIpAddr.isIPv4) {
            YK_NOTIFY_FROM_IPV4_ADDRS
          } else {
            YK_NOTIFY_FROM_IPV6_ADDRS
          }
        }

        val r = ipSubnetsAllowed
          .iterator
          .map { ipAddrStr =>
            new IPAddressString( ipAddrStr )
              .toAddress
              .toPrefixBlock
          }
          .exists { ipSubnet =>
            val isMatch = ipSubnet contains remoteIpAddr
            if (isMatch) LOGGER.trace(s"$logPrefix Found subnet match: ip[${remoteIpAddr}] matches allowed yookassa subnet[${ipSubnet}]")
            isMatch
          }

        if (!r) LOGGER.error(s"$logPrefix Not found mathing yookassa-subnet for ip[${remoteIpAddr}]\n Allowed subnets are: ${ipSubnetsAllowed.mkString(" ")}")

        r
      }

      if {
        val r = request.hasBody
        if (!r) LOGGER.error(s"$logPrefix Unexpected body-less request. Ignoring everything else.\n ${remoteIpAddr} => ${request.method} ${request.uri}")
        r
      }

      contentTypeHeaderOpt = {
        val ctHeader = request.headers.get( HeaderNames.CONTENT_TYPE )
        ctHeader.fold {
          LOGGER.warn(s"$logPrefix Missing Content-Type header in request.")
        } { contentType =>
          LOGGER.trace(s"$logPrefix Found request Content-Type header: $contentType")
        }
        ctHeader
      }

      if {
        contentTypeHeaderOpt.fold {
          LOGGER.debug(s"$logPrefix Optimistically guessing missing Content-type header as valid.")
          true
        } { contentType =>
          val jsonCt = MimeTypes.JSON
          val r = (contentType ==* jsonCt)
          if (!r) LOGGER.warn(s"$logPrefix Unexpected request body content-type[$contentType], but [$jsonCt] expected")
          r
        }
      }

      // Validate body length
      if {
        val contentLenHdrName = HeaderNames.CONTENT_LENGTH
        request
          .headers
          .get( contentLenHdrName )
          .fold {
            LOGGER.error(s"$logPrefix Missing ${contentLenHdrName} header in request.")
            false

          } { contentLenValue =>
            val pattern = "^([0-9]{1,7})".r
            contentLenValue match {
              case pattern( bytesCountStr ) =>
                val bytesCount = bytesCountStr.toInt
                LOGGER.trace(s"$logPrefix Request.body size is $bytesCount bytes.")
                val maxBodySizeBytes = 1024*1024
                val r = (bytesCount < maxBodySizeBytes) && (bytesCount > 0)
                if (!r) LOGGER.warn(s"$logPrefix request.body size has not-allowed length: $bytesCount, max is $maxBodySizeBytes")
                r
              case _ =>
                LOGGER.error(s"$logPrefix Cannot parse value of $contentLenHdrName: $contentLenValue")
                false
            }
          }
      }

      // Parse request.body as JSON
      bodyJson <- {
        LOGGER.trace(s"$logPrefix Request.body is\n-----------------------${request.body}\n-----------------------")
        val r = request.body.asJson
        if (r.isEmpty) LOGGER.error(s"$logPrefix Cannot parse request.body as JSON")
        r
      }

      // Parse JSON body as MYkObject:
      ykObj <- {
        bodyJson
          .validate[MYkObject]
          .fold(
            {jsonErrors =>
              LOGGER.error(s"$logPrefix Failed to parse request.body as yk-object\n - ${jsonErrors.mkString("\n - ")}")
              None
            },
            {obj =>
              LOGGER.trace(s"$logPrefix Request.body parsed as yk-object: $obj")
              Some( obj )
            }
          )
      }

      if {
        val notificationType = MYkObjectTypes.Notification
        val r = (ykObj.typ ==* notificationType)
        if (!r) LOGGER.warn(s"$logPrefix Expected $notificationType, but ${ykObj.typ} received")
        r
      }

      // Parse ykObject.obj into MYkPayment.
      payment <- {
        ykObj.obj
          .validate[MYkPayment]
          .fold(
            {jsonErrors =>
              LOGGER.error(s"$logPrefix Failed to parse yk-object.obj as payment.\n - ${jsonErrors.mkString("\n - ")}")
              None
            },
            {ykPayment =>
              LOGGER.trace(s"$logPrefix Payment#${ykPayment.id} for ${ykPayment.amount} - notification parsed ok:\n $ykPayment")
              Some( ykPayment )
            }
          )
      }

    } yield {
      import slick.profile.api._
      val dbAction = (for {
        // Read current payment transaction info from own database:
        txnOpt <- bill2Util.mTxns
          .query
          .filter { t =>
            t.psTxnUidOpt === payment.id
          }
          .take( 1 )
          .result
          .headOption

        txn = txnOpt getOrElse {
          LOGGER.warn(s"$logPrefix Not found txn for payment#${payment.id}, returning 404, so payment-system will retry request later.")
          throw new HttpResultingException(
            httpResFut = httpErrorHandler.onClientError( request, NOT_FOUND, s"Payment not found: ${payment.id}" )
          )
        }

        txnOrderId = txn.orderIdOpt getOrElse {
          val msg = s"$logPrefix Order-less txn#${txn.id.orNull}? psTxnUid=${payment.id}"
          LOGGER.error(msg)
          throw new HttpResultingException(
            httpResFut = httpErrorHandler.onServerError( request, new IllegalStateException(msg) )
          )
        }

        // Make changes in database:
        _ <- ykObj.event.get match {
          // Completed payment processing.
          case MYkEventTypes.PaymentSucceeded =>
            txn.datePaid.fold {
              LOGGER.debug(s"$logPrefix txn#${txn.id.orNull} pending => success, because payment#${payment.id} is completed.")
              for {
                contractIdOpt <- bill2Util.mOrders.getContractId( txnOrderId )
                contractId = contractIdOpt getOrElse {
                  val msg = s"$logPrefix Not found order#${txnOrderId} for txn#${txn.id.orNull} paymentUid#${payment.id}. Should never happen."
                  LOGGER.error(msg)
                  throw new HttpResultingException(
                    httpResFut = httpErrorHandler.onServerError( request, new IllegalStateException(msg) )
                  )
                }
                priceAmount = payment.amount.toSioPrice
                txn2 <- bill2Util.incrUserBalanceFromPaySys(
                  txn         = txnOpt,
                  contractId  = contractId,
                  mprice      = priceAmount,
                  psTxnUid    = payment.id,
                  orderIdOpt  = txn.orderIdOpt,
                )
              } yield {
                LOGGER.info(s"$logPrefix Completed transaction #${txn2.id.orNull} psUid#${payment.id} with $priceAmount")
                txn2
              }

            } { datePaid =>
              LOGGER.warn(s"$logPrefix txn#${txn.id} already completed $datePaid. Ignoring success => success switch")
              throw new HttpResultingException(
                httpResFut = Future.successful( Ok("Payment already processed") )
              )
            }

          // Pending => Waiting for money capture.
          case MYkEventTypes.PaymentWaitForCapture =>
            txn.datePaid.fold {
              LOGGER.debug(s"$logPrefix Opened transaction#${txn.id.orNull} psUid#${payment.id} now waiting for money capture.")
            } { datePaid =>
              LOGGER.warn(s"$logPrefix Closed paid transaction#${txn.id.orNull} psUid#${payment.id} is waiting for money capture, but was paid in $datePaid. This is normal? What to do here?")
            }
            throw new HttpResultingException(
              httpResFut = Future.successful( Ok("Nothing to do, waiting for payment completeness.") )
            )

          // User is refused to pay. Need to unhold order, drop incompleted transaction.
          case MYkEventTypes.PaymentCancelled | MYkEventTypes.RefundSucceeded =>
            txn.datePaid.fold {
              LOGGER.trace(s"$logPrefix Cancelling of pending transaction#${txn.id.orNull} psUid#${payment.id}.")
              for {
                _ <- bill2Util.unholdOrder( txnOrderId )
                // TODO Deleting transaction? Maybe just to reset transaction comment?
                countUpdated <- bill2Util.mTxns.cancelTxn(
                  txnId = txn.id.get,
                  comment = payment.cancellationDetails.map(_.toString),
                )
                if countUpdated ==* 1
              } yield {
                LOGGER.info(s"$logPrefix Successfully cancelled & removed txn#${txn.id.orNull} psUid#${payment.id}, and unholded order#$txnOrderId")
                txn
              }

            } { datePaid =>
              LOGGER.warn(s"$logPrefix Need to cancel already closed & paid (since $datePaid) transaction#${txn.id.orNull} psUid#${payment.id}.")

              for {
                _ <- billDebugUtil.interruptOrder( txnOrderId )
                // Decrease user balance with transaction amount
                contractIdOpt <- {
                  LOGGER.trace(s"$logPrefix Order#$txnOrderId marked as closed.")
                  bill2Util.mOrders.getContractId( txnOrderId )
                }
                txnOpt2 <- DBIO.sequenceOption(
                  for (contractId <- contractIdOpt) yield {
                    val reducePriceBy = payment.amount.toSioPrice
                    LOGGER.trace(s"$logPrefix Reducing user's balance $reducePriceBy, contract#$contractId")
                    bill2Util.incrUserBalanceFromPaySys(
                      contractId = contractId,
                      mprice     = MPrice.amount.modify( -_ )( reducePriceBy ),
                      psTxnUid   = billDebugUtil.ROLLBACKED_PAYMENT_UID_PREFIX + payment.id,
                      orderIdOpt = txn.orderIdOpt,
                      comment    = Some( s"${payment.cancellationDetails.fold(MPaySystems.YooKassa.toString)(_.party)} cancelled payment${payment.cancellationDetails.fold("")(cr => " (" + cr.reason + ")")}. Rollback transaction#${txn.id.orNull}." ),
                      txType     = MTxnTypes.Rollback,
                    )
                  }
                )
              } yield {
                LOGGER.trace(s"$logPrefix Done paid transaction rollback.")
                txnOpt2 getOrElse txn
              }
            }
        }

      } yield {
        LOGGER.trace(s"$logPrefix Finished DB altering")
      })
        .transactionally

      for {
        _ <- slick.db.run( dbAction )
        // AdvBuilder procedures will be done via billing timers in background.
      } yield {
        Results.Ok("Saved OK.")
      }
    })
      .getOrElse {
        LOGGER.debug(s"$logPrefix Some checks has been failed.")
        httpErrorHandler.onClientError( request, FORBIDDEN, "Request cannot be processed: some check failed." )
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
