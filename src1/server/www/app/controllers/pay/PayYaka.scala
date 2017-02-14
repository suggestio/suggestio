package controllers.pay

import com.google.inject.Inject
import controllers.SioControllerImpl
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.order.MOrders
import io.suggest.model.n2.node.MNode
import io.suggest.stat.m.{MAction, MActionTypes}
import io.suggest.util.Lists
import io.suggest.util.logs.MacroLogsImpl
import models.mbill.MCartIdeas
import models.mctx.Context
import models.mpay.yaka.{MYakaAction, MYakaActions, MYakaFormData, MYakaReq}
import models.mproj.ICommonDi
import models.req.{INodeOrderReq, IReq, IReqHdr}
import models.usr.MPersonIdents
import play.api.mvc.Result
import play.twirl.api.Xml
import util.acl.{CanPayOrder, MaybeAuth}
import util.billing.Bill2Util
import util.mail.IMailerWrapper
import util.pay.yaka.YakaUtil
import util.stat.StatUtil
import views.html.lk.billing.pay._
import views.html.lk.billing.pay.yaka._
import views.xml.lk.billing.pay.yaka._yakaRespTpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 15:02
  * Description: Контроллер для ПС Яндекс.Касса, которая к сбербанку ближе.
  *
  * @see Прямая ссылка: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9D%D0%B0%D1%87%D0%B0%D0%BB%D0%BE-%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D0%B8]]
  *      Короткая ссылка: [[https://goo.gl/Zfwt15]]
  */
class PayYaka @Inject() (
               maybeAuth                : MaybeAuth,
               canPayOrder              : CanPayOrder,
               statUtil                 : StatUtil,
               mItems                   : MItems,
               yakaUtil                 : YakaUtil,
               mBalances                : MBalances,
               mOrders                  : MOrders,
               bill2Util                : Bill2Util,
               mailerWrapper            : IMailerWrapper,
               mPersonIdents            : MPersonIdents,
               override val mCommonDi   : ICommonDi
             )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi.{ec, mNodesCache, slick}


  /**
    * Реакция контроллера на невозможность оплатить ордер, т.к. ордер уже оплачен или не требует оплаты.
    * @param request Реквест c ордером.
    * @return Редирект.
    */
  private def _alreadyPaid(request: INodeOrderReq[_]): Future[Result] = {
    NotImplemented("Order already paid. TODO")    // TODO Редиректить? На страницу подтверждения оплаты или куда?
  }

  /** Узнать цену для оплаты.
    *
    * @param orderId id ордера.
    * @param request Текущий реквест.
    * @return MPrice или exception.
    */
  // TODO Удалить, вынеся логику на уровень биллинга в виде транзакции.
  private def _getPayPrice(orderId: Gid_t)(implicit request: IReq[_]): Future[MPrice] = {
    // Посчитать, сколько юзеру надо доплатить за вычетом остатков по балансам.
    val payPricesFut = bill2Util.getPayPrices(orderId, request.user.mBalancesFut)

    // Собираем данные для рендера формы отправки в платёжку.
    for {
      payPrices     <- payPricesFut
    } yield {
      yakaUtil.assertPricesForPay(payPrices)
    }

  }


  /** Подготовиться к оплате через яндекс-кассу.
    * Юзеру рендерится неизменяемая форма с данными для оплаты.
    * Экшен можно вызывать несколько раз, он не влияет на БД.
    *
    * @param orderId id заказа-корзины.
    * @param onNodeId На каком узле сейчас находимся?
    * @return Страница с формой оплаты, отправляющей юзера в яндекс.кассу.
    */
  def payForm(orderId: Gid_t, onNodeId: MEsUuId) = canPayOrder.Get(orderId, onNodeId, _alreadyPaid, U.Balance).async { implicit request =>
    val personId = request.user.personIdOpt.get

    val payPriceFut = _getPayPrice(orderId)

    // Попытаться определить email клиента.
    val userEmailOptFut = for {
      // TODO Надо бы искать максимум 1 элемент.
      epws <- mPersonIdents.findAllEmails(personId)
    } yield {
      epws.headOption
    }

    // Собираем данные для рендера формы отправки в платёжку.
    for {
      payPrice      <- payPriceFut
      userEmailOpt  <- userEmailOptFut
    } yield {
      implicit val ctx = implicitly[Context]

      // Цена в одной единственной валюте, которая поддерживается яндекс-кассой.
      // Собрать аргументы для рендера, отрендерить страницу с формой.
      val formData = MYakaFormData(
        shopId  = yakaUtil.SHOP_ID,
        scId    = yakaUtil.SC_ID,
        sumRub  = payPrice.amount,
        customerNumber  = request.user.personIdOpt.get,
        orderNumber     = Some(orderId),
        clientEmail     = userEmailOpt
      )
      // Отрендерить шаблон страницы.
      val html = PayFormTpl(request.mnode) {
        // Отрендерить саму форму в HTML. Форма может меняться от платежки к платёжке, поэтому вставляется в общую страницу в виде HTML.
        _YakaFormTpl(formData)(ctx)
      }(ctx)
      Ok(html)
    }
  }


  /**
    * Логгировать, если неуклюжие ксакепы ковыряются в API сайта.
    * @param request Инстанс HTTP-реквеста.
    * @return Some(stat action) если есть странность с данными для MStat/kibana.
    *         None если всё ок.
    */
  private def _logIfHasCookies(implicit request: IReqHdr): Option[MAction] = {
    OptionUtil.maybe( mCommonDi.isProd && request.cookies.nonEmpty ) {
      LOGGER.error(s"_logIfHasCookies[${System.currentTimeMillis()}]: Unexpected cookies ${request.cookies} from client ${request.remoteAddress}, personId = ${request.user.personIdOpt}")
      MAction(
        actions = MActionTypes.UnexpectedCookies :: Nil,
        textNi  = {
          val hdrStr = request.headers
            .headers
            .iterator
            .map { case (k,v) => k + ": " + v }
            .mkString("\n")
          request.cookies.toString() :: hdrStr :: Nil
        }
      )
    }
  }

  private def formatReqBody(body: Map[String, Traversable[Any]]): String = {
    val d = "\n "
    body
      .mapValues(_.mkString(", "))
      .mkString(d, d, "")
  }

  /** id узла платежной системы. Узел не существует, просто нужен был идентифицируемый id для статистики. */
  private def PAY_SYS_NODE_ID = "Yandex.Kassa"

  def YREQ_BP = parse.urlFormEncoded(60000)

  /**
    * Экшен проверки платежа яндекс-кассой.
    *
    * POST body содержит вот такие данные:
    *
    * {{{
    *   orderNumber -> 617
    *   orderSumAmount -> 2379.07
    *   cdd_exp_date -> 1117
    *   shopArticleId -> 391660
    *   paymentPayerCode -> 4100322062290
    *   cdd_rrn -> 217175135289
    *   external_id -> deposit
    *   paymentType -> AC
    *   requestDatetime -> 2017-02-14T10:38:38.971+03:00
    *   depositNumber -> 97QmIfZ5P9JSF-mqD0uEbYeRXY0Z.001f.201702
    *   nst_eplPayment -> true
    *   cps_user_country_code -> PL
    *   cdd_response_code -> 00
    *   orderCreatedDatetime -> 2017-02-14T10:38:38.780+03:00
    *   sk -> yde0255436f276b59f1642648b119b0d0
    *   action -> checkOrder
    *   shopId -> 84780
    *   scid -> 548806
    *   shopSumBankPaycash -> 1003
    *   shopSumCurrencyPaycash -> 10643
    *   rebillingOn -> false
    *   orderSumBankPaycash -> 1003
    *   cps_region_id -> 2
    *   orderSumCurrencyPaycash -> 10643
    *   merchant_order_id -> 617_140217103753_00000_84780
    *   unilabel -> 2034c791-0009-5000-8000-00001cc939aa
    *   cdd_pan_mask -> 444444|4448
    *   customerNumber -> rosOKrUOT4Wu0Bj139F1WA
    *   yandexPaymentId -> 2570071018240
    *   invoiceId -> 2000001037346
    *   shopSumAmount -> 2295.80
    *   md5 -> AD3E905D6F489F1AD7F2F302D2982B1B
    * }}}
    *
    * @see Пример с CURL: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9F%D1%80%D0%B8%D0%BC%D0%B5%D1%80-curl]]
    * @return 200 OK + XML, когда всё нормально.
    */
  def check = maybeAuth().async(YREQ_BP) { implicit request =>
    lazy val logPrefix = s"check[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteAddress} body: ${formatReqBody(request.body)}")

    val yakaAction = MYakaActions.Check
    val shopId = yakaUtil.SHOP_ID

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind yaka request: ${formatFormErrors(formWithErrors)}")
        _badRequest(yakaAction)
      },

      // Всё ок забиндилось. Выверяем все данные.
      {yReq =>
        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val resDataFut: Future[(List[MAction], Xml)] = {
          val expMd5 = yakaUtil.getMd5(yReq)
          // Проверяем shopId и md5 одновременно, чтобы по-лучше идентифицировать реквест.
          if (
            yReq.shopId == shopId &&
            yReq.action == yakaAction &&
            expMd5.equalsIgnoreCase( yReq.md5 )
          ) {

            // Проверяем связь юзера и ордера: Узнать id контракта юзера.
            val usrNodeOptFut = mNodesCache.getById(yReq.personId)

            // Начальные stat-экшены.
            val statMas0Fut = _statActions0(yReq, usrNodeOptFut)

            val resFut = for {
              // Дождаться получения ноды обрабатываемого юзера.
              usrNodeOpt <- usrNodeOptFut

              // Проверить ордер, цену, захолдить ордер если требуется.
              _ <- slick.db.run {
                import slick.profile.api._
                val a = for {
                  mOrder0 <- bill2Util.checkOrder(
                    orderId             = yReq.orderId,
                    validContractId     = usrNodeOpt.get.billing.contractId.get,
                    claimedOrderPrices  = MCurrencies.hardMapByCurrency( yReq :: Nil )
                  )

                  // Стоимость заказа успешно выверена. Захолдить ордер.
                  mOrderHold <- bill2Util.holdOrder( mOrder0 )
                } yield {
                  mOrderHold
                }
                a.transactionally
              }

              statMas0 <- statMas0Fut

            } yield {
              // Всё ок. Вернуть 200 + XML без ошибок.
              val xml = _successXml(yakaAction, yReq.invoiceId)
              val checkMa = MAction(
                actions = MActionTypes.Success :: Nil
              )
              (checkMa :: statMas0, xml)
            }

            resFut.recoverWith { case ex: Throwable =>
              LOGGER.error(s"$logPrefix billing failed to check current order.", ex)
              // Вернуть ошибку 100, понятную для яндекс-кассы:
              for (statMas0 <- statMas0Fut) yield {
                val xml = _yakaRespTpl(
                  yakaAction  = yakaAction,
                  errCode     = yakaUtil.ErrorCodes.ORDER_ERROR,
                  invoiceId   = Some( yReq.invoiceId ),
                  shopId      = shopId,
                  errMsg      = Some( ex.getMessage )
                )
                val statMas2 = MAction(
                  actions = MActionTypes.PayBadOrder :: Nil,
                  textNi  = ex.toString :: Nil
                ) :: statMas0
                (statMas2, xml)
              }
            }

          } else {
            _badMd5(yakaAction, yReq, expMd5)
          }
        }

        // Запустить подготовку статистики. Тут Future[], но в данном случае этот код синхронный O(1).
        val userSaOptFut = statUtil.userSaOptFutFromRequest()

        for {
          (statsMas, res) <- resDataFut
          userSaOpt1      <- userSaOptFut
        } yield {
          // В фоне завершаем работу со статистикой.
          // Future{} -- чтобы не задерживаться, надежно изолировав работу платежки от работы статистики, не всегда безглючной.
          Future {
            _saveStats(yReq, statsMas, userSaOpt1)
          }

          // Вернуть XML-ответ яндекс-кассе. Тут всегда 200, судя по докам.
          Ok(res)
        }
      }
    )
  }


  /** Ответ платежке при невозможности понять запрос. */
  private def _badRequest(yakaAction: MYakaAction): Result = {
    val xml = _yakaRespTpl(
      yakaAction = yakaAction,
      errCode    = yakaUtil.ErrorCodes.BAD_REQUEST,
      shopId     = yakaUtil.SHOP_ID,
      invoiceId  = None
    )
    Ok(xml)
  }

  /** Инициализация начальных данных для статистики.
    * Код используется в обоих экшенах: check и payment.
    */
  private def _statActions0(yReq: MYakaReq, personNodeOptFut: Future[Option[MNode]]): Future[List[MAction]] = {
    // Собрать stat-экшен для записи текущего юзера.
    for (usrNodeOpt <- personNodeOptFut) yield {
      val ma0 = statUtil.personNodeStat(yReq.personId, usrNodeOpt)
      ma0 :: Nil
    }
  }


  /** Уведомление о прошедшем платеже.
    *
    * @return 200 + XML.
    */
  def payment = maybeAuth().async(YREQ_BP) { implicit request =>
    lazy val logPrefix = s"payment[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteAddress} body:\n ${formatReqBody(request.body)}")

    val yakaAction = MYakaActions.Payment
    val shopId = yakaUtil.SHOP_ID

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix unable to bind form: ${formatFormErrors(formWithErrors)}")
        _badRequest(yakaAction)
      },

      // Удалось забиндить данные оплаты в форму. Надо проверить данные реквеста, в т.ч. md5.
      {yReq =>
        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val expMd5 = yakaUtil.getMd5(yReq)
        val resDataFut: Future[(List[MAction], Xml)] = {
          // Проверяем shopId и md5 одновременно, чтобы по-лучше идентифицировать реквест.
          if ( expMd5.equalsIgnoreCase(yReq.md5) &&
            yReq.shopId == shopId &&
            yReq.action == yakaAction
          ) {
            // Базовые поля реквеста и md5 совпадают с ожидаемыми. Зачислить на баланс юзера оплаченные бабки, попытаться исполнить ордер.
            // Узнать узел юзера, для его contract_id и человеческого названия.
            val usrNodeOptFut = mNodesCache.getById( yReq.personId )

            // Собрать начальные stat-экшены.
            val statMas0Fut = _statActions0(yReq, usrNodeOptFut)

            val payFut: Future[(List[MAction], Xml)] = for {
              // Дождаться данных по узлу юзера.
              usrNodeOpt <- usrNodeOptFut
              usrNode = usrNodeOpt.get
              contractId = usrNode.billing.contractId.get

              // Выполнить действия в биллинге, связанные с проведением платежа.
              cartIdea <- slick.db.run {
                bill2Util.handlePaySysPayment(
                  orderId     = yReq.orderId,
                  contractId  = contractId,
                  mprice      = MPrice(yReq)
                )
              }

              // Узнать начальные данные для статистики.
              statMas0 <- statMas0Fut

            } yield {
              val maPaym = cartIdea match {
                case oc: MCartIdeas.OrderClosed =>
                  LOGGER.info(s"$logPrefix Order ${yReq.orderId} closed successfully. Invoice ${yReq.invoiceId}")
                  MAction(
                    actions = MActionTypes.Success :: Nil
                  )
                case _ =>
                  val cartIdeaStr = cartIdea.toString
                  LOGGER.error(s"$logPrefix Unable to close order ${yReq.orderId}. something gone wrong: $cartIdeaStr")
                  Future {
                    mailerWrapper.instance
                      .setSubject(s"[YaKa] Проблемы с завершением платежа order#${yReq.orderId} инвойс#{${yReq.invoiceId}")
                      .setText(cartIdeaStr + "\n\n\n" + request)
                      .setRecipients(mailerWrapper.EMAILS_PROGRAMMERS: _*)
                      .send()
                  }
                  MAction(
                    actions = MActionTypes.PayBadIdea :: Nil,
                    textNi  = cartIdea.toString :: Nil
                  )
              }

              // Рендер XML-ответа яндекс-кассе.
              val xml = _successXml(yakaAction, yReq.invoiceId)

              // Собрать результат.
              (maPaym :: statMas0, xml)
            }

            payFut.recoverWith { case ex: Throwable =>
              val yReqStr = yReq.toString
              LOGGER.error(s"$logPrefix Unable to process payment $yReqStr", ex)
              for (statMas0 <- statMas0Fut) yield {
                val maErr = MAction(
                  actions = MActionTypes.PayBadOrder :: Nil,
                  textNi  = ex.toString :: yReqStr :: Nil
                )
                val xml = _yakaRespTpl(
                  yakaAction  = yakaAction,
                  errCode     = yakaUtil.ErrorCodes.ORDER_ERROR,
                  shopId      = shopId,
                  invoiceId   = Some( yReq.invoiceId ),
                  errMsg      = Some( ex.getMessage )
                )
                (maErr :: statMas0, xml)
              }
            }

          } else {
            _badMd5(yakaAction, yReq, expMd5)
          }
        }

        // Завершить экшен сохранением статистики и отсылкой ответа яндекс-кассе.
        val userSaOptFut = statUtil.userSaOptFutFromRequest()
        for {
          (statMas3, respXml) <- resDataFut
          userSaOpt1          <- userSaOptFut
        } yield {
          // В фоне заканчиваем сбор и сохранение статистики.
          Future {
            _saveStats(yReq, statMas3, userSaOpt1)
          }
          Ok(respXml)
        }
      }
    )
  }


  private def _badMd5(yakaAction: MYakaAction, yReq: MYakaReq, expMd5: String): Future[(List[MAction], Xml)] = {
    val shopId = yakaUtil.SHOP_ID
    LOGGER.error(s"_badMd5Xml($yakaAction): yaka req binded, but action/shop/md5 invalid:\n $yReq\n calculated = $expMd5 , but provided = ${yReq.md5} shopId=$shopId")
    // Записывать в статистику.
    val maErr = MAction(
      actions = MActionTypes.PayBadReq :: Nil,
      textNi  = s"$expMd5\nMD5 != ${yReq.md5}" :: Nil
    )
    val xml = _yakaRespTpl(
      yakaAction  = yakaAction,
      errCode     = yakaUtil.ErrorCodes.MD5_ERROR,
      invoiceId   = Some( yReq.invoiceId ),
      shopId      = shopId
    )
    val statMas2 = maErr :: Nil
    Future.successful((statMas2, xml))
  }


  /** Рендер XML для положительного ответа для яндекс-кассы по экшену. */
  private def _successXml(yakaAction: MYakaAction, invoiceId: Long): Xml = {
    _yakaRespTpl(
      yakaAction  = yakaAction,
      errCode     = yakaUtil.ErrorCodes.NO_ERROR,
      shopId      = yakaUtil.SHOP_ID,
      invoiceId   = Some(invoiceId)
    )
  }


  /** Закончить подготовку записи по статистике и сохранить. */
  private def _saveStats(yReq: MYakaReq, statsMas: List[MAction], userSaOpt1: Option[MAction])(implicit ctx1: Context): Future[_] = {
    import ctx1.request
    val sa0Opt = _logIfHasCookies
    // Записать в статистику результат этой работы
    // Используем mstat для логгирования, чтобы всё стало видно в kibana.
    val maPc = MAction(
      actions = MActionTypes.PayCheck :: Nil,
      nodeId = PAY_SYS_NODE_ID :: Nil,
      textNi = yReq.toString :: Nil
    )
    val s2 = new statUtil.Stat2 {
      override def statActions = {
        Lists.prependOpt(sa0Opt) {
          maPc :: statsMas
        }
      }
      override def userSaOpt = userSaOpt1
      override def ctx = ctx1
    }
    val fut = statUtil.saveStat(s2)
    fut.onFailure { case ex: Throwable =>
      LOGGER.error(s"_saveStats(): Unable to save stat $s2\n yReq = $yReq\n statsMas = $statsMas\n userSaOpt1 = $userSaOpt1", ex)
    }
    fut
  }

  // TODO success(): https://my.suggest.io/pay/yaka/success?orderNumber=617&orderSumAmount=2379.07&cdd_exp_date=1117&shopArticleId=391660&paymentPayerCode=4100322062290&paymentDatetime=2017-02-14T10%3A38%3A39.751%2B03%3A00&cdd_rrn=217175135289&external_id=deposit&paymentType=AC&requestDatetime=2017-02-14T10%3A38%3A39.663%2B03%3A00&depositNumber=97QmIfZ5P9JSF-mqD0uEbYeRXY0Z.001f.201702&nst_eplPayment=true&cdd_response_code=00&cps_user_country_code=PL&orderCreatedDatetime=2017-02-14T10%3A38%3A38.780%2B03%3A00&sk=yde0255436f276b59f1642648b119b0d0&action=PaymentSuccess&shopId=84780&scid=548806&rebillingOn=false&orderSumBankPaycash=1003&cps_region_id=2&orderSumCurrencyPaycash=10643&merchant_order_id=617_140217103753_00000_84780&unilabel=2034c791-0009-5000-8000-00001cc939aa&cdd_pan_mask=444444%7C4448&customerNumber=rosOKrUOT4Wu0Bj139F1WA&yandexPaymentId=2570071018240&invoiceId=2000001037346

}
