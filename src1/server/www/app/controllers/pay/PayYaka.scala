package controllers.pay

import javax.inject.Singleton
import javax.inject.Inject
import controllers.SioControllerImpl
import io.suggest.bill.MCurrencies
import io.suggest.common.coll.Lists
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.order.MOrders
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.stat.m.{MAction, MActionTypes}
import io.suggest.util.logs.MacroLogsImpl
import models.mbill.MEmailOrderPaidTplArgs
import models.mctx.Context
import models.mdr.MMdrNotifyMeta
import models.mpay.yaka._
import models.mproj.ICommonDi
import models.req.{INodeOrderReq, IReq, IReqHdr}
import models.usr.{MPersonIdents, MSuperUsers}
import play.api.i18n.Messages
import play.api.mvc._
import play.twirl.api.Xml
import util.acl._
import util.adn.NodesUtil
import util.billing.Bill2Util
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import util.mdr.MdrUtil
import util.pay.yaka.YakaUtil
import util.stat.StatUtil
import util.xplay.SecHeadersFilterUtil
import views.html.lk.billing.order.OrderPaidEmailTpl
import views.html.lk.billing.pay._
import views.html.lk.billing.pay.yaka._
import views.html.stuff.PleaseWaitTpl
import views.xml.lk.billing.pay.yaka._YakaRespTpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 15:02
  * Description: Контроллер для ПС Яндекс.Касса, которая к сбербанку ближе.
  *
  * 2016.mar.6: Для эксперимента реализована возможность одновременного использования демо и продакшен кассы
  * через разные экшены с похожими названиями.
  * Какой-то быдлокод получился (Может лучше бы по scId разделять, и карту профилей по scId?),
  * но анкета с новыми ссылками уже отправлена, пока будет терпеть и наблюдать.
  *
  * @see Прямая ссылка: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9D%D0%B0%D1%87%D0%B0%D0%BB%D0%BE-%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D0%B8]]
  *      Короткая ссылка: [[https://goo.gl/Zfwt15]]
  */
@Singleton
class PayYaka @Inject() (
                          esModel                  : EsModel,
                          mNodes                   : MNodes,
                          maybeAuth                : MaybeAuth,
                          canPayOrder              : CanPayOrder,
                          canViewOrder             : CanViewOrder,
                          statUtil                 : StatUtil,
                          mItems                   : MItems,
                          yakaUtil                 : YakaUtil,
                          mBalances                : MBalances,
                          isSuOrNotProduction      : IsSuOrNotProduction,
                          mOrders                  : MOrders,
                          bill2Util                : Bill2Util,
                          secHeadersFilterUtil     : SecHeadersFilterUtil,
                          mailerWrapper            : IMailerWrapper,
                          mPersonIdents            : MPersonIdents,
                          nodesUtil                : NodesUtil,
                          mSuperUsers              : MSuperUsers,
                          mdrUtil                  : MdrUtil,
                          identUtil                : IdentUtil,
                          override val mCommonDi   : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi.{ec, slick, csrf, errorHandler}
  import esModel.api._


  /** Заголовок ответа, разрешающий открытие ресурсов sio из фреймов.
    * Яндекс-касса использует фреймы в продакшен режиме.
    * Если этот заголовок больше не будет нужен, то нужно вычистить метод и весь сопутствующий код.
    */
  // TODO Удалить. Уже должно всё работать ок без этих костылей.
  //@deprecated("YaKa resovled back-to-site frames problem. Delete it.", "2017.05.12")
  private def FRAMES_ALLOWED: List[(String, String)] = {
    yakaUtil.paySystem
      .returnRespHdr_XFrameOptions_AllowFrom
      .iterator
      .flatMap { url =>
        secHeadersFilterUtil.FRAMES_ALLOWED_FROM( url )
      }
      .toList
  }


  /**
    * Реакция контроллера на невозможность оплатить ордер, т.к. ордер уже оплачен или не требует оплаты.
    * @param request Реквест c ордером.
    * @return Редирект.
    */
  private def _alreadyPaid(request: INodeOrderReq[_]): Future[Result] = {
    implicit val req = request
    val messages = implicitly[Messages]
    val call = _callToOrder(
      orderId  = request.morder.id.get,
      onNodeId = request.mnode.id.get
    )
    Redirect( call )
      .flashing(FLASH.ERROR -> messages("Order.already.paid"))
  }

  /** Платеж через демо-кассу. */
  def demoPayForm(orderId: Gid_t, onNodeId: MEsUuId) = {
    val action = _payForm(yakaUtil.DEMO, orderId, onNodeId)
    if (yakaUtil.DEMO_ALLOWED_FOR_ALL) {
      action
    } else {
      isSuOrNotProduction(action)
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
  def payForm(orderId: Gid_t, onNodeId: MEsUuId): Action[AnyContent] = {
    yakaUtil.PRODUCTION_OPT.fold {
      canPayOrder(orderId, onNodeId, _alreadyPaid) { implicit request =>
        LOGGER.debug(s"payForm($orderId, $onNodeId): PRODUCTION mode unavailable, will try demo-mode...")
        Redirect( routes.PayYaka.demoPayForm(orderId, onNodeId) )
      }
    } { prod =>
      _payForm(prod, orderId, onNodeId)
    }
  }

  private def _payForm(profile: IYakaProfile, orderId: Gid_t, onNodeId: MEsUuId) = csrf.AddToken {
    canPayOrder(orderId, onNodeId, _alreadyPaid, U.Balance).async { implicit request =>
      val orderPricesFut = slick.db.run {
        bill2Util.getOrderPrices(orderId)
      }

      val payPriceFut = for {
        payPrices0 <- bill2Util.getPayPrices(orderPricesFut, request.user.balancesFut)
      } yield {
        yakaUtil.assertPricesForPay(payPrices0)
      }

      val personId = request.user.personIdOpt.get

      // Попытаться определить email клиента.
      val userEmailOptFut = for {
        // TODO Opt Надо бы искать максимум 1 элемент.
        epws <- mPersonIdents.findEmails(personId)
      } yield {
        epws.headOption
      }

      // Подготовить контекст рендера страницы личного кабинета.
      val ctxFut = request.user.lkCtxDataFut.map { implicit lkCtxData =>
        implicitly[Context]
      }

      // Проверить итоговую стоимость по minPrice
      val minPayPriceOptFut = for {
        payPrice <- payPriceFut
      } yield {
        // Сравнить с минимальной разрешенной стоимостью платежа в данном направлении.
        // Вернуть минималку, если текущий платёж ниже необходимой планки.
        bill2Util.minPayPrice( yakaUtil.paySystem, payPrice )
      }

      // Собираем данные для рендера формы отправки в платёжку.
      for {
        payPrice        <- payPriceFut
        userEmailOpt    <- userEmailOptFut
        minPayPriceOpt  <- minPayPriceOptFut
        ctx             <- ctxFut
      } yield {
        // Цена в одной единственной валюте, которая поддерживается яндекс-кассой.
        // Собрать аргументы для рендера, отрендерить страницу с формой.
        val formData = MYakaFormData(
          profile         = profile,
          orderPrice      = payPrice,
          minPayPrice     = minPayPriceOpt,
          onNodeId        = onNodeId,
          customerNumber  = request.user.personIdOpt.get,
          orderNumber     = Some(orderId),
          clientEmail     = userEmailOpt
        )

        // Отрендерить шаблон страницы.
        val html = PayFormTpl(orderId, request.mnode) {
          // Отрендерить саму форму в HTML. Форма может меняться от платежки к платёжке, поэтому вставляется в общую страницу в виде HTML.
          _YakaFormTpl(formData)(ctx)
        }(ctx)
        Ok(html)
      }
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
      LOGGER.error(s"_logIfHasCookies[${System.currentTimeMillis()}]: Unexpected cookies ${request.cookies} from client ${request.remoteClientAddress}, personId = ${request.user.personIdOpt}")
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

  def YREQ_BP = parse.formUrlEncoded(60000)


  /** Сделать исключение, если не-суперюзер пытается платить через демокассу. */
  private def _assertDemoSu(profile: IYakaProfile, yReq: IYakaReq): Unit = {
    if (
      // Если сейчас используется демо-профиль...
      profile.isDemo &&
        // И если demo-режим разрешён не для всех
        !yakaUtil.DEMO_ALLOWED_FOR_ALL &&
        // то нужно убедится, что текущий юзер -- это суперпользователь
        !mSuperUsers.isSuperuserId(yReq.personId)
    ) {
      // Текущему юзеру нельзя юзать demo-режим, т.к. он не является суперюзером.
      throw new SecurityException(s"_assertDemoSu($profile): Non-SU user tried to pay via DEMOkassa.")
    }
  }


  def check     = _check( yakaUtil.PRODUCTION )
  def demoCheck = _check( yakaUtil.DEMO )

  /**
    * Экшен проверки платежа яндекс-кассой.
    *
    * POST body содержит вот такие данные:
    *
    * @see Пример с CURL: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9F%D1%80%D0%B8%D0%BC%D0%B5%D1%80-curl]]
    * @return 200 OK + XML, когда всё нормально.
    */
  private def _check(profile: IYakaProfile) = maybeAuth().async(YREQ_BP) { implicit request =>
    lazy val logPrefix = s"check[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteClientAddress} body: ${formatReqBody(request.body)}")

    val yakaAction = MYakaActions.Check

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form(profile).bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind yaka request: ${formatFormErrors(formWithErrors)}")
        _badRequest(profile, yakaAction)
      },

      // Всё ок забиндилось. Выверяем все данные.
      {yReq =>
        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val resDataFut: Future[(List[MAction], Xml)] = {
          val expMd5 = yakaUtil.getMd5(profile, yReq)
          // Проверяем shopId и md5 одновременно, чтобы по-лучше идентифицировать реквест.
          if (
            yReq.shopId == profile.shopId &&
            yReq.action == yakaAction &&
            expMd5.equalsIgnoreCase( yReq.md5 )
          ) {
            // Если демо-режим, то разрешить только для суперюзеров:
            _assertDemoSu(profile, yReq)

            // Проверяем связь юзера и ордера: Узнать id контракта юзера.
            val usrNodeOptFut = mNodes.getByIdCache(yReq.personId)

            // Начальные stat-экшены.
            val statMas0Fut = _statActions0(yReq, usrNodeOptFut)

            val resFut = for {
              // Дождаться получения ноды обрабатываемого юзера.
              usrNodeOpt <- usrNodeOptFut

              // Проверить ордер, цену, захолдить ордер если требуется.
              _ <- slick.db.run {
                import slick.profile.api._
                val a = for {
                  // Обсчитать ордер
                  mOrder0 <- bill2Util.checkPayableOrder(
                    orderId             = yReq.orderId,
                    validContractId     = usrNodeOpt.get.billing.contractId.get,
                    paySys              = yakaUtil.paySystem,
                    claimedOrderPrices  = MCurrencies.hardMapByCurrency( yReq.price :: Nil )
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
              val xml = _successXml(profile, yakaAction, yReq.invoiceId)
              val checkMa = MAction(
                actions = MActionTypes.Success :: Nil
              )
              (checkMa :: statMas0, xml)
            }

            resFut.recoverWith { case ex: Throwable =>
              LOGGER.error(s"$logPrefix billing failed to check current order.", ex)
              // Вернуть ошибку 100, понятную для яндекс-кассы:
              for (statMas0 <- statMas0Fut) yield {
                val xml = _YakaRespTpl(
                  yakaAction  = yakaAction,
                  errCode     = yakaUtil.ErrorCodes.ORDER_ERROR,
                  invoiceId   = Some( yReq.invoiceId ),
                  shopId      = profile.shopId,
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
            _badMd5(profile, yakaAction, yReq, expMd5)
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
  private def _badRequest(profile: IYakaProfile, yakaAction: MYakaAction): Result = {
    val xml = _YakaRespTpl(
      yakaAction = yakaAction,
      errCode    = yakaUtil.ErrorCodes.BAD_REQUEST,
      shopId     = profile.shopId,
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


  def payment = _payment( yakaUtil.PRODUCTION )
  def demoPayment = _payment( yakaUtil.DEMO )

  /** Уведомление о прошедшем платеже.
    *
    * @return 200 + XML.
    */
  private def _payment(profile: IYakaProfile) = maybeAuth().async(YREQ_BP) { implicit request =>
    lazy val logPrefix = s"payment[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteClientAddress} body:\n ${formatReqBody(request.body)}")

    val yakaAction = MYakaActions.Payment

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form(profile).bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix unable to bind form: ${formatFormErrors(formWithErrors)}")
        _badRequest(profile, yakaAction)
      },

      // Удалось забиндить данные оплаты в форму. Надо проверить данные реквеста, в т.ч. md5.
      {yReq =>
        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val expMd5 = yakaUtil.getMd5(profile, yReq)
        val resDataFut: Future[(List[MAction], Xml)] = {
          // Проверяем shopId и md5 одновременно, чтобы по-лучше идентифицировать реквест.
          if ( expMd5.equalsIgnoreCase(yReq.md5) &&
            yReq.shopId == profile.shopId &&
            yReq.action == yakaAction
          ) {
            // Если демо-режим, то разрешить только для суперюзеров:
            _assertDemoSu(profile, yReq)

            // Базовые поля реквеста и md5 совпадают с ожидаемыми. Зачислить на баланс юзера оплаченные бабки, попытаться исполнить ордер.
            // Узнать узел юзера, для его contract_id и человеческого названия.
            val usrNodeOptFut = mNodes.getByIdCache( yReq.personId )
            val mprice = yReq.price

            // Собрать начальные stat-экшены.
            val statMas0Fut = _statActions0(yReq, usrNodeOptFut)

            // В фоне узнать все email'ы юзера-плательщика.
            val userEmailsFut = mPersonIdents.findEmails( yReq.personId )

            val payFut: Future[(List[MAction], Xml)] = for {
              // Дождаться данных по узлу юзера.
              usrNodeOpt <- usrNodeOptFut
              usrNode = usrNodeOpt.get
              contractId = usrNode.billing.contractId.get

              // Выполнить действия в биллинге, связанные с проведением платежа.
              (balTxn, mdrNotifyCtx, ffor) <- slick.db.run {
                import slick.profile.api._
                val a = for {
                  // Проверить ордер, что он в статусе HOLD или DRAFT.
                  mOrder0  <- bill2Util.getOpenedOrderForUpdate(yReq.orderId, validContractId = contractId)

                  // Закинуть объем перечисленных денег на баланс указанного юзера.
                  balTxn   <- bill2Util.incrUserBalanceFromPaySys(
                    contractId  = contractId,
                    mprice      = mprice,
                    orderIdOpt  = Some(yReq.orderId),
                    psTxnUid    = yReq.invoiceId.toString,
                    comment     = Some( MsgCodes.`Yandex.Kassa` )
                  )

                  // Подготовить ордер корзины к исполнению.
                  owi <- bill2Util.prepareCartOrderItems(mOrder0)

                  // Узнать, потребуется ли уведомлять модеров по email при успешном завершении транзакции.
                  mdrNotifyCtx1 <- mdrUtil.mdrNotifyPrepareCtx(owi)

                  // Запустить действия, связанные с вычитанием бабла с баланса юзера и реализацией MItem'ов заказа.
                  // Здесь используется более сложный и толерантный вариант экзекуции ордера. Fallback-вариант -- это bill2Util.maybeExecuteOrder(owi).
                  ffor1 <- bill2Util.forceFinalizeOrder(owi)

                } yield {
                  (balTxn, mdrNotifyCtx1, ffor1)
                }

                a.transactionally
              }

              // Узнать начальные данные для статистики.
              statMas0 <- statMas0Fut

            } yield {
              var statMasAcc = statMas0

              // Если есть успешно обработанные item'ы, то Success наверное.
              if (ffor.okItemsCount > 0) {
                LOGGER.info(s"$logPrefix Order ${yReq.orderId} closed successfully. Invoice ${yReq.invoiceId}")
                // Уведомить модераторов, если необходимо.
                if (mdrUtil.isMdrNotifyNeeded(mdrNotifyCtx)) {
                  val usrDisplayNameOptFut = nodesUtil.getPersonName( usrNodeOptFut, Some(userEmailsFut) )
                  val ctx = implicitly[Context]
                  for {
                    usrDisplayNameOpt <- usrDisplayNameOptFut

                    tplArgs = MMdrNotifyMeta(
                      paidTotal   = Some( mprice ),
                      orderId     = Some( yReq.orderId ),
                      txn         = Some( balTxn ),
                      personId    = Some( yReq.personId ),
                      personName  = usrDisplayNameOpt
                    )
                    _ <- mdrUtil.sendMdrNotify( mdrNotifyCtx, tplArgs )(ctx)
                  } {
                    LOGGER.trace(s"$logPrefix mdr notify finished ok")
                  }
                }

                // Собрать stat-экшен.
                statMasAcc ::= MAction(
                  actions = MActionTypes.Success :: Nil
                )
              }

              // Если были проблемы при закрытии заказа, то надо уведомить программистов о наличии проблемы.
              for (skippedCart <- ffor.skippedCartOpt) {
                LOGGER.trace(s"$logPrefix Was not able to close order ${yReq.orderId} clearly. There are skipped cart-order#${skippedCart.id.orNull}")
                val fforStr = ffor.toString
                Future {
                  mailerWrapper.instance
                    .setSubject(s"[YaKa] Проблемы с завершением заказа ${yReq.invoiceId}")
                    .setText(s"order ${yReq.orderId}\n\ninvoice ${yReq.invoiceId}\n\n\n${request.method} ${request.uri}\n\n$yReq\n\n$fforStr")
                    .setRecipients(mailerWrapper.EMAILS_PROGRAMMERS: _*)
                    .send()
                }
                statMasAcc ::= MAction(
                  actions = MActionTypes.PayBadBalance :: Nil,
                  textNi  = fforStr :: Nil
                )
              }

              // Надо уведомить юзера о поступившем платеже.
              for {
                userEmails <- userEmailsFut
                if userEmails.nonEmpty
              } {
                // TODO Нужно определять messages в контексте текущего юзера по yReq.*, а не из автоматического HTTP-реквеста яндекс-кассы.
                implicit val uCtx = implicitly[Context]
                val orderIdStr = yReq.orderId.toString
                val tplArgs = MEmailOrderPaidTplArgs(
                  asEmail     = true,
                  orderId     = yReq.orderId,
                  orderIdStr  = orderIdStr,
                  onNodeId    = yReq.onNodeId,
                  withHello   = Some( None )    // TODO Поискать имя юзера надо как-то?,
                )
                mailerWrapper.instance
                  .setSubject( uCtx.messages( MsgCodes.`Order.0.is.paid`, orderIdStr ) )
                  .setHtml( OrderPaidEmailTpl(tplArgs)(uCtx).body )
                  .setRecipients( userEmails: _* )
                  .send()
              }

              // Рендер XML-ответа яндекс-кассе.
              val xml = _successXml(profile, yakaAction, yReq.invoiceId)

              // Собрать результат.
              (statMasAcc, xml)
            }

            payFut.recoverWith { case ex: Throwable =>
              val yReqStr = yReq.toString
              LOGGER.error(s"$logPrefix Unable to process payment $yReqStr", ex)
              for (statMas0 <- statMas0Fut) yield {
                val maErr = MAction(
                  actions = MActionTypes.PayBadOrder :: Nil,
                  textNi  = ex.toString :: yReqStr :: Nil
                )
                val xml = _YakaRespTpl(
                  yakaAction  = yakaAction,
                  errCode     = yakaUtil.ErrorCodes.ORDER_ERROR,
                  shopId      = profile.shopId,
                  invoiceId   = Some( yReq.invoiceId ),
                  errMsg      = Some( ex.getMessage )
                )
                (maErr :: statMas0, xml)
              }
            }

          } else {
            _badMd5(profile, yakaAction, yReq, expMd5)
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


  private def _badMd5(profile: IYakaProfile, yakaAction: MYakaAction, yReq: MYakaReq, expMd5: String): Future[(List[MAction], Xml)] = {
    LOGGER.error(s"_badMd5Xml($yakaAction): yaka req binded, but action/shop/md5 invalid:\n $yReq\n calculated = $expMd5 , but provided = ${yReq.md5} shopId=${profile.shopId}")
    // Записывать в статистику.
    val maErr = MAction(
      actions = MActionTypes.PayBadReq :: Nil,
      textNi  = s"$expMd5\nMD5 != ${yReq.md5}" :: Nil
    )
    val xml = _YakaRespTpl(
      yakaAction  = yakaAction,
      errCode     = yakaUtil.ErrorCodes.MD5_ERROR,
      invoiceId   = Some( yReq.invoiceId ),
      shopId      = profile.shopId
    )
    val statMas2 = maErr :: Nil
    Future.successful((statMas2, xml))
  }


  /** Рендер XML для положительного ответа для яндекс-кассы по экшену. */
  private def _successXml(profile: IYakaProfile, yakaAction: MYakaAction, invoiceId: Long): Xml = {
    _YakaRespTpl(
      yakaAction  = yakaAction,
      errCode     = yakaUtil.ErrorCodes.NO_ERROR,
      shopId      = profile.shopId,
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
      nodeId  = yakaUtil.paySystem.nodeIdOpt.toList,
      textNi  = yReq.toString :: Nil
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
    for (ex <- fut.failed) {
      LOGGER.error(s"_saveStats(): Unable to save stat $s2\n yReq = $yReq\n statsMas = $statsMas\n userSaOpt1 = $userSaOpt1", ex)
    }
    fut
  }


  def success(qs: MYakaReturnQs) = maybeAuth().async { implicit request =>
    _success(yakaUtil.PRODUCTION, qs)
  }
  def demoSuccess(qs: MYakaReturnQs) = {
    val actionBuilder = if (yakaUtil.DEMO_ALLOWED_FOR_ALL)
      maybeAuth()
    else
      isSuOrNotProduction()
    actionBuilder.async { implicit request =>
      _success(yakaUtil.DEMO, qs)
    }
  }

  /** Яндекс.касса вернула сюда юзера после удачной оплаты.
    * Сессия юзера могла истечь, пока он платил, поэтому тут maybeAuth.
    * @param qs QS-аргументы запроса.
    */
  private def _success(profile: IYakaProfile, qs: MYakaReturnQs)(implicit request: IReq[_]): Future[Result] = {
    lazy val logPrefix = s"success[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix User returned with $qs")

    val respFut: Future[Result] = if (qs.action != MYakaReturnActions.Success) {
      LOGGER.warn(s"$logPrefix unexpected qs action: ${qs.action}")
      errorHandler.onClientError(request, EXPECTATION_FAILED, s"No success: ${qs.action}")

    } else if (qs.shopId != profile.shopId) {
      LOGGER.warn(s"$logPrefix Unexpected shopId for returned user: ${qs.shopId}")
      errorHandler.onClientError(request, NOT_ACCEPTABLE, "Invalid shopID.")

    } else if (request.user.personIdOpt.nonEmpty && !request.user.personIdOpt.contains(qs.personId)) {
      LOGGER.warn(s"$logPrefix Unexpected personId for returned user: ${qs.personId}, but expected ${request.user.personIdOpt}")
      errorHandler.onClientError(request, FORBIDDEN, "Invalid user.")

    } else {
      // Юзер либо аноним, либо правильный. Надо отредиректить юзера на его узел, где он может просмотреть итоги оплаты.
      val rdr = Redirect( _callToOrder(qs.orderId, qs.onNodeId) )
        .flashing(FLASH.SUCCESS -> implicitly[Context].messages("Thanks.for.buy"))
      Future.successful(rdr)
    }

    for (resp <- respFut) yield
      resp.withHeaders( FRAMES_ALLOWED: _* )
  }



  /** Аналог success() для POST-запросов.
    *
    * Судя по докам, возможен переход на этот экшен через POST из яндекс-кошелька.
    * @see [[https://tech.yandex.ru/money/doc/payment-solution/shop-config/parameters-docpage/]]
    */
  def failPostQs(qs: MYakaReturnQs) = fail(qs)
  def demoFailPostQs(qs: MYakaReturnQs) = fail(qs)


  /** Какая-то непонятная ошибка без подробностей на стороне кассы. И полезных данных в qs нет.
    * Такое бывает, когда у юзера слетела/истекла сессия в платежной системе.
    * @return Редирект куда-нибудь.
    */
  def failUnknown = maybeAuth().async { implicit request =>
    LOGGER.warn(s"failUnknown(): Unknown error, user[${request.user.personIdOpt}] ip=${request.remoteClientAddress} qs: ${request.rawQueryString}")
    val callFut = request.user.personIdOpt.fold [Future[Call]] {
      Future.successful( controllers.routes.Ident.emailPwLoginForm() )
    } { personId =>
      // hold-ордер не ищем, т.к. повисший ордер крайне маловероятен в данной ситуации.
      identUtil.redirectCallUserSomewhere(personId)
    }
    val ctx = implicitly[Context]
    for (call <- callFut) yield {
      Redirect(call)
        .flashing( FLASH.ERROR -> ctx.messages("Unknown.error") )
        .withHeaders( FRAMES_ALLOWED: _* )
    }
  }

  def demoFailUnknown = failUnknown


  /** Яндекс.касса вернула юзера сюда из-за ошибки оплаты.
    * Сессия юзера могла закончится, пока он платил, поэтому тут maybeAuth.
    *
    * @param qs QS-аргументы запроса, если есть.
    *           None, если яндекс-касса не понимает, что за юзер и по какой причине к ней лезет с оплатой.
    *           Такое возможно, если закрыть браузер с открытой вкладкой оплаты, потом снова запустить браузер
    *           с сохраненной вкладкой.
    */
  def fail(qs: MYakaReturnQs) = maybeAuth() { implicit request =>
    LOGGER.trace(s"fail(): $qs <=\n ${request.rawQueryString}")
    // Т.к. будет небыстрый двойной редирект с ожиданием транзакции, используем 200 OK + Location: вместо обычного редиректа.
    Ok( PleaseWaitTpl() )
      .withHeaders(
        (LOCATION -> routes.PayYaka.failLoggedIn(qs.orderId, qs.onNodeId).url) ::
          FRAMES_ALLOWED: _*
      )
  }
  def demoFail(qs: MYakaReturnQs) = fail(qs)


  /** Переброска сюда из fail() с отбросом ненужного мусора. */
  def failLoggedIn(orderId: Gid_t, onNodeId: MEsUuId) = {
    canViewOrder(orderId, onNodeId = Some(onNodeId)).async { implicit request =>
      // Есть права на просмотр ордера. Попытаться разморозить этот ордер.
      val unholdFut = slick.db
        .run {
          bill2Util.unholdOrder(orderId)
        }
        .recover { case ex: Throwable =>
          LOGGER.error(s"failLoggedIn($orderId, $onNodeId): Failed to unhold the order", ex)
          null
        }

      // Когда обработка зафейленного ордера будет завершена, вернуть окончательный редирект.
      for {
        _ <- unholdFut
      } yield {
        Redirect( _callToOrder(orderId, onNodeId) )
          .flashing(FLASH.ERROR -> implicitly[Context].messages("Pay.error"))
          .withHeaders( FRAMES_ALLOWED: _* )
      }
    }
  }


  /** Общий код сборки ссылки на возврат юзера в личный кабинет вынесен сюда.
    *
    * @param orderId id ордера.
    * @param onNodeId На каком узле идёт просмотр ордера.
    * @return Call до страницы, показывающей ордер.
    */
  private def _callToOrder(orderId: Gid_t, onNodeId: MEsUuId): Call = {
    controllers.routes.LkBill2.orderPage(
      orderId     = Some(orderId),
      nodeId      = onNodeId
    )
  }

}
