package util.pay.yookassa

import cats.data.OptionT
import inet.ipaddr.IPAddressString
import io.suggest.bill.MPrice
import io.suggest.bill.cart.{MCartPayInfo, MPayableVia}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.mbill2.m.txn.{MMoneyReceiverInfo, MTxnMetaData, MTxnTypes}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pay.MPaySystems
import io.suggest.pay.yookassa._
import io.suggest.proto.http.HttpConst
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import play.api.Configuration
import play.api.libs.ws.{WSAuthScheme, WSClient}
import util.TplDataFormatUtil
import japgolly.univeq._
import models.mpay.MCartNeedMoneyArgs
import models.req.IReq
import org.apache.commons.codec.binary.Base64
import play.api.http.{HeaderNames, HttpErrorHandler, MimeTypes}
import play.api.libs.json.{JsObject, Json, OWrites, Reads}
import play.api.mvc.{AnyContent, Result, Results}
import util.acl.SioControllerApi
import util.billing.{Bill2Util, BillDebugUtil}
import util.mdr.MdrUtil

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
  private lazy val mdrUtil = injector.instanceOf[MdrUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val esModel = injector.instanceOf[EsModel]

  /** Current paysystem is YooKassa. */
  def PAY_SYSTEM = MPaySystems.YooKassa

  /** Force time-based idempotence key rotation.
    * This is needed to prevent duplication for continious payment requests. */
  private def IDEMPOTENCE_KEY_ROTATE_EVERY_SECONDS = 20

  // Validate remote client address as in documentation https://yookassa.ru/developers/using-api/webhooks#ip
  /** IPv4 YooKassa addrs, according to documentation. */
  def YK_NOTIFY_FROM_IPV4_ADDRS = {
    "185.71.76.0/27" ::
    "185.71.77.0/27" ::
    "77.75.153.0/25" ::
    "77.75.156.11/32" ::
    "77.75.156.35/32" ::
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
      new YooKassaProfile {
        override lazy val shopId =
          profileConf.get[String]("shop_id")
        override private[yookassa] lazy val secretKey =
          profileConf.get[String]("secret")
        override lazy val isTest =
          profileConf.get[Boolean]("test")
        override lazy val isDealSupported =
          profileConf.getOptional[Boolean]("deal").getOrElseFalse
      }
    })
      .to( LazyList )
  }

  def firstProfile(isTest: Boolean): Option[YooKassaProfile] =
    yooProfiles.find(_.isTest ==* isTest)
  def firstProfile: Option[YooKassaProfile] =
    yooProfiles.headOption

  def findProfile(mpv: MPayableVia): Option[YooKassaProfile] =
    yooProfiles.find( _.isTest ==* mpv.isTest )


  /** @return true, if safe deals are supported by current paysystem profile. */
  def isSafeDealsSupported(profile: YooKassaProfile): Boolean =
    profile.isDealSupported


  def payableVias: Seq[MPayableVia] = {
    yooProfiles
      .map { ykProfile =>
        MPayableVia(
          isTest = ykProfile.isTest,
        )
      }
  }


  def mkIdemptKeyHash(f: MessageDigest => Unit): String = {
    new Base64(0, Array.empty, true)
      .encodeToString {
        // idempotence key to mark duplicate requests.
        val sha1 = MessageDigest.getInstance("SHA-1")
        f( sha1 )
        sha1.digest()
      }
  }

  def REST_API_ENDPOINT = "https://api.yookassa.ru/v3/"


  def prefmtFooter(profile: YooKassaProfile): Option[String] = {
    Option.when( profile.isTest ) {
      // Bank-card testing credentials: https://yookassa.ru/developers/payment-forms/widget/quick-start#process-enter-test-data-test-store
      """Номер карты — 5555 5555 5555 4477
        |Срок действия — 01/30 (или другая дата, больше текущей)
        |CVC — 123 (или три любые цифры)
        |Код 3-D Secure — 123 (или три любые цифры)
        |""".stripMargin
    }
  }

  def priceToYkAmount(price: MPrice): MYkAmount = {
    MYkAmount(
      value     = TplDataFormatUtil.formatPriceAmountPlain( price ),
      currency  = price.currency,
    )
  }


  /** Zero-step of payment procedure: seed remote PaySystem with payment data.
    *
    * @param profile Payment system profile.
    * @param orderItem Cart order contents.
    * @param payPrice Pricing for pay.
    * @param personOpt Payer (person node).
    * @param ctx Rendering context.
    * @return Future with parsed pay-system response.
    */
  def preparePayment(profile: YooKassaProfile, orderItem: MOrderWithItems, payPrice: MPrice, personOpt: Option[MNode], deal: Option[MYkPaymentDeal])
                    (implicit ctx: Context): Future[YooKassaPaymentPrepareResult] = {
    val orderId = orderItem.order.id.get

    lazy val logPrefix = s"startPayment(order#$orderId/+${orderItem.items.length}, U#${ctx.request.user.personIdOpt.orNull}):"
    LOGGER.trace(s"$logPrefix Starting, $profile, price=$payPrice")

    val ykAmountTotal = priceToYkAmount( payPrice )

    def _getFirstEdgeNodeId( pred: MPredicate ): Option[String] = {
      (for {
        mnode <- personOpt.iterator
        phoneEdge <- mnode.edges.withPredicateIter( pred )
        if phoneEdge.info.flag getOrElse true
        phoneNumber <- phoneEdge.nodeIds
      } yield {
        phoneNumber
      })
        .nextOption()
    }

    val personPhoneNumberOpt = _getFirstEdgeNodeId( MPredicates.Ident.Phone )

    // Assemble request body for new payment start:
    val payCreate = MYkPaymentCreate(
      amount = ykAmountTotal,
      description = Some( ctx.messages( MsgCodes.`Payment.for.order.N`, orderId ) ),
      capture = OptionUtil.SomeBool.someTrue,

      // merchantCustomerId: Use readable phone number here or unreadable nodeId.
      merchantCustomerId = personPhoneNumberOpt
        // TODO Don't using email here, because we need to debug email validation.
        .orElse {
          // Return person node ID, because no phone number found.
          ctx.request.user.personIdOpt
        },

      // Embedded: returns embedded web-widget for sio-payment page.
      confirmation = Some {
        MYkPaymentConfirmation(
          // TODO This should be defined outside here (in http-controller)
          pcType = MYkPaymentConfirmationTypes.Embedded,
        )
      },

      receipt = Some( MYkReceipt(
        items = MYkItem(
          description     = ctx.messages( MsgCodes.`Advertising.services` ),
          quantity        = "1",
          amount          = ykAmountTotal,
          vatCode         = MYkVatCodes.NoVat,
          paymentSubject  = Some( MYkPaymentSubjects.Service ),
        ) :: Nil,
        customer = {
          val personEmailOpt = _getFirstEdgeNodeId( MPredicates.Ident.Email )
          Option.when( personPhoneNumberOpt.nonEmpty || personEmailOpt.nonEmpty ) {
            MYkCustomer(
              email = personEmailOpt,
              phone = personPhoneNumberOpt,
            )
          }
        },
      ))
    )

    // Generate idempotence key via hashing some data from. Use base64 to minify length (instead of HEX-encoding):
    // LineLen=0, SEP=[], because do not add CR-LF at the end here.
    val hashStr = mkIdemptKeyHash { sha1 =>
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
        .orElse( ctx.request.user.personIdOpt )
        .foreach { personId =>
          sha1.update( personId.getBytes )
        }
    }

    // Append order id to original hash (outside the hash):
    val idemptKey = hashStr + "/" + orderId
    val requestBodyJson = Json.toJsObject(payCreate)

    LOGGER.trace(s"$logPrefix Preparing HTTP request to yookassa/payments:\n Profile = $profile\n IdempotenceKey = ${idemptKey}\n-----------------------\n${Json.prettyPrint(requestBodyJson)}\n----------------------")

    for {
      // Start HTTP-request:
      resp <- wsClient
        .url( s"$REST_API_ENDPOINT/payments" )
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
        httpErrorHandler.onClientError(request, NOT_FOUND, s"- $other - invalid REST endpoint for paySystem[$PAY_SYSTEM]")
    }
  }


  /** Handle incoming HTTP-notifications from yookassa.ru. */
  def handleHttpNotify()(implicit request: IReq[AnyContent], ctx: Context): Future[Result] = {
    lazy val logPrefix = s"handleHttpNotify()#${System.currentTimeMillis}:"

    (for {
      // Parse remote client IP-address:
      remoteIpAddr <- {
        request.remoteClientIpTry.fold(
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
            (t.psTxnUidOpt === payment.id) &&
            (t.paySystemStr === PAY_SYSTEM.value)
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
        actionRes: YkNotifyDbActionRes <- ykObj.event.get match {
          // Completed payment processing.
          case MYkEventTypes.PaymentSucceeded =>
            txn.datePaid.fold {
              LOGGER.debug(s"$logPrefix txn#${txn.id.orNull} pending => success, because payment#${payment.id} is completed.")
              for {
                // Maybe, prepare to notify moderators about moderation tasks:
                mdrNotifyCtx1 <- mdrUtil.mdrNotifyPrepareCtx
                // Grab contractId for next changes:
                contractIdOpt <- bill2Util.mOrders.getContractId( txnOrderId )
                contractId = contractIdOpt getOrElse {
                  val msg = s"$logPrefix Not found order#${txnOrderId} for txn#${txn.id.orNull} paymentUid#${payment.id}. Should never happen."
                  LOGGER.error(msg)
                  throw new HttpResultingException(
                    httpResFut = httpErrorHandler.onServerError( request, new IllegalStateException(msg) )
                  )
                }
                priceAmount = payment.amount.toSioPrice

                // Ensure order: is hold or draft.
                payResult <- bill2Util.handlePaymentReceived(
                  orderId    = txnOrderId,
                  contractId = contractId,
                  payPrice   = priceAmount,
                  paySysUid  = payment.id,
                  paySystem  = PAY_SYSTEM,
                  txn        = txnOpt,
                )
              } yield {
                LOGGER.info(s"$logPrefix Completed transaction#${payResult.balanceTxn.id.orNull} psUid#${payment.id} with $priceAmount, order#$txnOrderId with ${payResult.draftOrder.items.length} opened items => okItemsCount=>${payResult.result.okItemsCount}")
                // After db-transaction and outside it, do related activities: send moderation email, etc.
                val afterF = { () =>
                  import esModel.api._

                  // Notify moderators about current user transaction and related events:
                  val personNodeOptFut = mNodes.dynSearchOne(
                    new MNodeSearch {
                      override val contractIds = contractId :: Nil
                      override def limit = 1
                      override val nodeTypes = MNodeTypes.Person :: Nil
                    }
                  )
                  val mdrNotifyFut = mdrUtil.maybeSendMdrNotify(
                    mdrNotifyCtx  = mdrNotifyCtx1,
                    orderId       = txn.orderIdOpt,
                    personNodeFut = personNodeOptFut,
                    paidTotal     = Some( priceAmount ),
                  )(ctx)

                  // May be, notify s.io staff about possible problems with order closing:
                  val cartSkipsNotifyFut = mdrUtil.maybeNotifyCartSkippedItems( PAY_SYSTEM, payResult )

                  // Notify payer about changes in money balance.
                  val userPaymentNotifyFut = (for {
                    txnMeta       <- txn.metadata
                    cartSubmitQs  <- txnMeta.cartQs
                    onNodeId      <- cartSubmitQs.onNodeId
                  } yield {
                    LOGGER.trace(s"$logPrefix Found onNodeId=$onNodeId for order#$txnOrderId in txn#${txn.id.orNull} metadata")
                    mdrUtil.paymentNotifyPayer(
                      personNodeOptFut,
                      orderId = txnOrderId,
                      onNodeId = Some( onNodeId ),
                    )(ctx)
                  })
                    .getOrElse {
                      // TODO In future, need to return into showcase-cart URL.
                      LOGGER.debug(s"$logPrefix Detecting back-address into user's personal cabinet, because nodeId undefined in txn.metadata.")
                      mdrUtil.paymentNotifyPayerDetect(
                        personNodeOptFut,
                        orderId = txnOrderId,
                      )(ctx)
                    }

                  Future.sequence(
                    mdrNotifyFut ::
                    cartSkipsNotifyFut ::
                    userPaymentNotifyFut ::
                    Nil
                  )
                }

                YkNotifyDbActionRes( Some(afterF) )
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
          // TODO Refund status here. Need to test this in future when refunds will be implemented. Because refunds are not implemented a.t.m. then this status was coded here.
          case MYkEventTypes.PaymentCancelled | MYkEventTypes.RefundSucceeded =>
            txn.datePaid.fold {
              LOGGER.trace(s"$logPrefix Cancelling of pending transaction#${txn.id.orNull} psUid#${payment.id}.")
              for {
                _ <- bill2Util.unholdOrder(
                  orderId = txnOrderId,
                  comment = payment.cancellationDetails.map(_.toString),
                )
              } yield {
                LOGGER.info(s"$logPrefix Successfully cancelled & removed txn#${txn.id.orNull} psUid#${payment.id}, and unholded order#$txnOrderId")
                YkNotifyDbActionRes()
              }

            } { datePaid =>
              LOGGER.debug(s"$logPrefix Need to cancel already closed & paid (since $datePaid) transaction#${txn.id.orNull} psUid#${payment.id}.")

              for {
                _ <- billDebugUtil.interruptOrder( txnOrderId )
                // Decrease user balance with transaction amount
                contractIdOpt <- {
                  LOGGER.trace(s"$logPrefix Order#$txnOrderId marked as closed.")
                  bill2Util.mOrders.getContractId( txnOrderId )
                }
                _ <- DBIO.sequenceOption(
                  for (contractId <- contractIdOpt) yield {
                    val reducePriceBy = payment.amount.toSioPrice
                    LOGGER.trace(s"$logPrefix Reducing user's balance $reducePriceBy, contract#$contractId")
                    bill2Util.incrUserBalanceFromPaySys(
                      contractId = contractId,
                      mprice     = MPrice.amount.modify { amount0 =>
                        -Math.abs( amount0 )
                      }( reducePriceBy ),
                      psTxnUid   = billDebugUtil.ROLLBACKED_PAYMENT_UID_PREFIX + payment.id,
                      paySystem  = PAY_SYSTEM,
                      orderIdOpt = txn.orderIdOpt,
                      comment    = Some( s"${payment.cancellationDetails.fold(PAY_SYSTEM.toString)(_.party)} cancelled payment${payment.cancellationDetails.fold("")(cr => " (" + cr.reason + ")")}. Rollback transaction#${txn.id.orNull}." ),
                      txType     = MTxnTypes.ReturnToBalance,
                    )
                  }
                )
              } yield {
                LOGGER.info(s"$logPrefix Cancel/refunded/rollbacked transaction#${txn.id.orNull} ($datePaid) psUid#${payment.id}")
                // TODO Email notifications about refund/unpay.
                YkNotifyDbActionRes()
              }
            }
        }

      } yield {
        LOGGER.trace(s"$logPrefix Finished billing DB updates")
        actionRes
      })
        .transactionally

      for {
        dbActionRes <- slick.db.run( dbAction )
        // AdvBuilder procedures will be done via billing timers in background.
        _ <- dbActionRes
          .afterAction
          .fold [Future[_]]
            { Future.successful(()) }
            { afterAction =>
              LOGGER.trace(s"$logPrefix Launching after-database actions...")
              afterAction()
            }
      } yield {
        Results.Ok("Saved OK.")
      }
    })
      .getOrElse {
        LOGGER.debug(s"$logPrefix Some checks has been failed.")
        httpErrorHandler.onClientError( request, FORBIDDEN, "Request cannot be processed: some check failed." )
      }
  }


  private def _mkPost[Body: OWrites, Resp: Reads](logPrefix: => String, profile: YooKassaProfile, url: String, body: Body)
                                                 (idemptUpdate: MessageDigest => Unit = {md => md update body.toString.getBytes()}): Future[Resp] = {
    val idemptKey = mkIdemptKeyHash( idemptUpdate )
    LOGGER.trace(s"$logPrefix Requesting $PAY_SYSTEM#$profile Idempotence-Key=$idemptKey ...")

    wsClient
      .url( url )
      .addHttpHeaders(
        HttpConst.Headers.IDEMPOTENCE_KEY -> idemptKey,
      )
      .withAuth(
        username = profile.shopId,
        password = profile.secretKey,
        scheme   = WSAuthScheme.BASIC,
      )
      .post( Json.toJsObject( body ) )
      .map { resp =>
        if (resp.status ==* 200) {
          val respJson = resp.json
          LOGGER.trace(s"$logPrefix Successfully request $PAY_SYSTEM(${resp.status} ${resp.statusText}):\n${Json.prettyPrint(respJson)}")
          respJson.as[Resp]
        } else {
          val msg = s"API request failed: ${resp.status} ${resp.statusText}\n${resp.body}"
          LOGGER.info(s"$logPrefix $msg")
          throw new RuntimeException( msg )
        }
      }
  }


  /** Non-captured money transaction cancelling. Do not usable for already-success transactions.
    *
    * @param profile YooKassa profile.
    * @param paymentId YooKassa payment uuid.
    * @return Updated payment data, if cancelled.
    *         Failed future, if cancellation not possible.
    */
  def earlyCancelPayment(profile: YooKassaProfile, paymentId: String): Future[MYkPayment] = {
    lazy val logPrefix = s"earlyCancelPayment($paymentId):"
    _mkPost[JsObject, MYkPayment](
      logPrefix, profile,
      url = s"$REST_API_ENDPOINT/payments/$paymentId/cancel",
      body = JsObject.empty
    )( _.update(paymentId.getBytes()) )
  }


  /** Deal-starting API.
    *
    * @param profile YooKassa profile credentials.
    * @param dealInfo Deal-create instance.
    * @return Future with newly created deal info.
    */
  def startDeal(profile: YooKassaProfile, dealInfo: MYkDealCreate): Future[MYkDeal] = {
    lazy val logPrefix = s"startDeal(${profile.shopId}):"

    _mkPost[MYkDealCreate, MYkDeal](
      logPrefix, profile,
      url = s"$REST_API_ENDPOINT/deals",
      body = dealInfo,
    )()
  }


  /** Create outgoing money payout.
    *
    * @param profile YooKassa profile credentials.
    * @param payoutInfo Information about payout.
    * @return Future with processed payout.
    */
  def payOut(profile: YooKassaProfile, payoutInfo: MYkPayoutCreate): Future[MYkPayout] = {
    lazy val logPrefix = s"payOut(${payoutInfo.amount})#${System.currentTimeMillis()}:"

    _mkPost[MYkPayoutCreate, MYkPayout](
      logPrefix, profile,
      url = s"$REST_API_ENDPOINT/payouts",
      body = payoutInfo,
    )()
  }


  /** Billing thinks, that user need to pay some money via YooKassa payment system.
    *
    * @param args Arguments container.
    * @param ctx Current user's HTTP request context.
    * @return Payment information for client-side js.
    */
  def handleCartNeedsMoneyPay(args: MCartNeedMoneyArgs)(implicit ctx: Context): Future[MCartPayInfo] = {
    lazy val logPrefix = s"handleCartNeedsMoneyPay(o#${args.cartOrder.order.id.orNull})#${System.currentTimeMillis()}:"

    val payPrices = args.cartResolution.needMoney.get
    val paySystem = PAY_SYSTEM
    require(payPrices.lengthIs == 1, s"Only one currency allowed for $paySystem")
    val Seq(payPrice) = payPrices
    val ykProfile = findProfile( args.cartQs.payVia ).get

    for {
      // Collect all money receivers before payment/deal, because
      moneyRcvrs <- bill2Util.prepareAllMoneyReceivers( args.cartOrder.items )

      // If deals feature is supported by paysystem, we need to find money-receivers on current paysystem.
      // From all money-receivers, check for money-receivers with payouts configured for current paysystem.
      // If any dealable-receiver, let's open new deal on paysystem, save deal-transaction for order.
      ykDealInfoOpt <- (for {
        dealData <- OptionT.fromOption[Future] {
          bill2Util.prepareDealData(
            paySystem       = paySystem,
            isDealSupportedPaySys = isSafeDealsSupported( ykProfile ),
            moneyRcvrs      = moneyRcvrs,
          )
        }
        // Lets open paySystem deal on payment-system side:
        ykDeal <- OptionT.liftF {
          startDeal(
            ykProfile,
            MYkDealCreate(
              dealType = MYkDealTypes.SafeDeal,
              feeMoment = MYkDealFeeMoments.DealClosed,
              description = Some {
                ctx.messages( MsgCodes.`Deal.for.0`,
                  ctx.messages( MsgCodes.`Order.N`, args.cartOrder.order.id.get ),
                )
              },
            )
          )
        }
      } yield {
        // New deal created on paySystem-side. Let's bypass needed data to next phase:
        LOGGER.debug(s"$logPrefix Created new deal#${ykDeal.id} on $paySystem till ${ykDeal.expiresAt}")
        (ykDeal, dealData)
      })
        .value

      paymentStarted <- preparePayment(
        // TODO payProfile: Detect test/prod using URL qs args, filtering by request.user.isSuper
        profile   = ykProfile,
        orderItem = args.cartOrder,
        payPrice  = payPrice,
        personOpt = Some( args.personNode ),
        deal      = for {
          (ykDeal, dealData) <- ykDealInfoOpt
        } yield {
          MYkPaymentDeal(
            id = ykDeal.id,
            settlements = MYkPaymentDealSettlement(
              typ = MYkPaymentDealSettlementTypes.Payout,
              amount = priceToYkAmount( dealData.payoutPrice ),
            ) :: Nil,
          )
        },
      )(ctx)

      // Store started not-yet-completed transaction:
      _ <- slick.db.run {
        import slickHolder.slick.profile.api._

        val contractId = args.personContract.id.get
        (for {
          userBalance0 <- bill2Util.ensureBalanceFor( contractId, payPrice.currency )
          txn <- bill2Util.openPaySystemTxn(
            balanceId   = userBalance0.id.get,
            payAmount   = payPrice.amount,
            orderIdOpt  = args.cartOrder.order.id,
            psTxnId     = paymentStarted.payment.id,
            paySystem   = paySystem,
            psDealIdOpt = ykDealInfoOpt.map(_._1.id),
            txnMetadata = Some( MTxnMetaData(
              cartQs = Option.when( args.cartQs.nonEmpty )(args.cartQs),
              // TODO Remove this receivers: they are converted into transactions.
              moneyRcvrs = moneyRcvrs
                .iterator
                .map { mri =>
                  MMoneyReceiverInfo(
                    contractId      = mri.enc.contract.id.get,
                    price           = mri.price,
                    isSioComission  = mri.isSioComissionPrice,
                  )
                }
                .toSeq,
              psPayload = ykDealInfoOpt.map { case (ykDeal, _) =>
                Json toJson MTxnMetaYkPayload(
                  deal = Some( ykDeal ),
                )
              },
            ))
              .filter(_.nonEmpty),
          )

          // Save draft payout transactions, if any:
          payoutTxnsOpt <- DBIO.sequenceOption {
            for {
              (ykDeal, dealData) <- ykDealInfoOpt
            } yield {
              bill2Util.openDealPayoutTxns(
                orderIdOpt  = args.cartOrder.order.id,
                paySystem   = paySystem,
                psDealId    = ykDeal.id,
                moneyRcvrs  = dealData.decision.dealWith,
              )
            }
          }

          // Mark current cart order as Hold.
          orderHold <- {
            if (payoutTxnsOpt.exists(_.nonEmpty))
              LOGGER.trace(s"$logPrefix Created ${payoutTxnsOpt.iterator.flatten.size} payout draft transactions")
            bill2Util.holdOrder( args.cartOrder.order )
          }
        } yield {
          LOGGER.trace(s"$logPrefix Hold order#${orderHold.id.orNull}, hold pending txn#${txn.id}, will await payment confirmation.")
          None
        })
          .transactionally
      }
    } yield {
      MCartPayInfo(
        paySystem,
        metadata = Some( paymentStarted.metadata ),
        prefmtFooter = OptionUtil.maybeOpt( ctx.request.user.isSuper ) {
          prefmtFooter( ykProfile )
        },
      )
    }
  }

}


protected trait YooKassaProfile {

  def shopId    : String
  private[yookassa] def secretKey : String
  def isTest    : Boolean
  def isDealSupported: Boolean

  override final def toString: String = {
    s"${getClass.getSimpleName}($shopId,test?$isTest)"
  }

}


/** Prepare payment result for [[YooKassaUtil.preparePayment()]]. */
protected case class YooKassaPaymentPrepareResult(
                                                   metadata  : JsObject,
                                                   payment   : MYkPayment,
                                                 )

/** DB-action results container after completed DB-processing of yookassa notification. */
protected case class YkNotifyDbActionRes(
                                          afterAction     : Option[() => Future[_]]       = None,
                                        )
