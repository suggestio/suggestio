package controllers.pay

import com.google.inject.Inject
import controllers.SioControllerImpl
import io.suggest.bill.MPrice
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.order.MOrders
import io.suggest.stat.m.{MAction, MActionTypes}
import io.suggest.util.Lists
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mpay.yaka.{MYakaActions, MYakaFormData}
import models.mproj.ICommonDi
import models.req.{INodeOrderReq, IReq, IReqHdr}
import models.usr.MPersonIdents
import play.api.mvc.Result
import util.acl.{CanPayOrder, MaybeAuth}
import util.billing.Bill2Util
import util.pay.yaka.YakaUtil
import util.stat.StatUtil
import views.html.lk.billing.pay._
import views.html.lk.billing.pay.yaka._

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
               mPersonIdents            : MPersonIdents,
               override val mCommonDi   : ICommonDi
             )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi.{ec, errorHandler, mNodesCache, slick}


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


  /** id узла платежной системы. Узел не существует, просто нужен был идентифицируемый id для статистики. */
  private def PAY_SYS_NODE_ID = "Yandex.Kassa"

  /**
    * Экшен проверки платежа яндекс-кассой.
    *
    * @see Пример с CURL: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9F%D1%80%D0%B8%D0%BC%D0%B5%D1%80-curl]]
    * @return 200 OK + XML, когда всё нормально.
    */
  def check = maybeAuth().async { implicit request =>
    lazy val logPrefix = s"check[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteAddress} ${request.body}")
    implicit val ctx1 = implicitly[Context]

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind yaka request: ${formatFormErrors(formWithErrors)}")
        // Скрываемся за страницей 404. Для снижения потока мусора от URL-сканнеров.
        errorHandler.http404ctx(ctx1)
      },

      {yReq =>
        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val yPrice = MPrice(yReq.amount, yReq.currency)
        val expMd5 = yakaUtil.getMd5(yReq, yPrice)
        val (maRes, res): (MAction, Future[Result]) = {
          // Проверяем shopId и md5 одновременно, чтобы по-лучше идентифицировать реквест.
          if ( expMd5.equalsIgnoreCase(yReq.md5) &&
            yReq.shopId == yakaUtil.SC_ID &&
            yReq.action == MYakaActions.Check
          ) {
            // Реквест послан от того, кто знает пароль, shopId. TODO Проверить данные в реквесте, пересчитав сумму заказа/юзера.
            // Пересчитываем ценник...
            val payPriceFut = _getPayPrice(yReq.orderId)

            // Проверяем связь юзера и ордера: Узнать id контракта юзера.
            val usrNodeOptFut = mNodesCache.getById(yReq.personId)
            // Прочитать ордер из БД, это понадобиться на следующих шагах:
            val yReqOrderOptFut = slick.db.run {
              mOrders.getById( yReq.orderId )
            }

            // Узнать id контракта.
            val yReqContractIdOptFut = for (yReqOrderOpt <- yReqOrderOptFut) yield {
              LOGGER.trace(s"$logPrefix YaKa req order: $yReqOrderOpt")
              yReqOrderOpt.map(_.contractId)
            }

            // Ордер должен иметь открытый для начала платежа статус.
            val isOrderStatusOkFut: Future[Boolean] = for (yReqOrderOpt <- yReqOrderOptFut) yield {
              yReqOrderOpt.exists(_.status.canGoToPaySys)
            }

            // Объединяем все предыдущие измышления в единственный ответ.
            val isOrderOkExFut = for {
              isOrderStatusOk         <- isOrderStatusOkFut
              if isOrderStatusOk
              usrNodeOpt              <- usrNodeOptFut
              usrNode = usrNodeOpt.get
              yReqOrderContractIdOpt  <- yReqContractIdOptFut
            } yield {
              // Тут неленивые вычисления, но маловерятно, что вторая часть выражения не будет вычислена.
              yReqOrderContractIdOpt.exists { yReqOrderContractId =>
                usrNode.billing.contractId.contains(yReqOrderContractId)
              }
            }
            val isOrderOkFut = isOrderOkExFut.recover { case ex: NoSuchElementException =>
              LOGGER.trace(s"$logPrefix isOrderOk throwed NSEE => false", ex)
              false
            }

            // Проверяем стоимость размещения.
            val isPayPriceOkFut = for (payPrice <- payPriceFut) yield {
              // Сопоставить валюты...
              val matches = payPrice.currency == yReq.currency && {
                // Double сравнивать -- дело неблагодатное. Поэтому сравниваем в рамках погрешности: а это - 1 копейка
                val maxDiff = Math.pow(10, -payPrice.currency.exponent)
                Math.abs(payPrice.amount - yReq.amount) < maxDiff
              }
              LOGGER.trace(s"$logPrefix YakaReq price = ${yReq.amount} ${yReq.currency}\n ReCalculated price = $payPrice\n Prices matching? $matches")
              matches
            }

            for {
              isOrderOk     <- isOrderOkFut
              isPayPriceOk  <- isPayPriceOkFut
            } yield {
              if (isOrderOk && isPayPriceOk) {
                // Всё ок, TODO холдим ордер, возращаем 200 + XML
                ???
              } else {
                //BadRequest("invalid data")
                ???
              }
            }
            ???   // TODO MAction -> Future[Result]

          } else {
            LOGGER.error(s"$logPrefix yaka req binded, but md5 or shopId is invalid:\n $yReq\n calculated = $expMd5 , but provided = ${yReq.md5} shopId=${yakaUtil.SHOP_ID}")
            // TODO Записывать в статистику.
            val maErr = MAction(
              actions = MActionTypes.PayBadReq :: Nil,
              textNi  = s"$expMd5\nMD5 != $expMd5" :: Nil
            )
            maErr -> errorHandler.http404ctx(ctx1)
          }
        }

        // Запустить подготовку статистики. Тут Future[], но в данном случае этот код синхронный O(1).
        val userSaOptFut = statUtil.userSaOptFutFromRequest()

        val sa0Opt = _logIfHasCookies
        // Записать в статистику результат этой работы
        // Используем mstat для логгирования, чтобы всё стало видно в kibana.
        val maPc = MAction(
          actions = MActionTypes.PayCheck :: Nil,
          nodeId  = PAY_SYS_NODE_ID :: Nil
        )
        userSaOptFut
          .flatMap { userSaOpt1 =>
            val s2 = new statUtil.Stat2 {
              override def statActions = {
                Lists.prependOpt(sa0Opt) {
                  maPc :: maRes :: Nil
                }
              }
              override def userSaOpt = userSaOpt1
              override def ctx = ctx1
            }
            val saveFut = statUtil.saveStat(s2)
            saveFut.onFailure { case ex: Throwable =>
              LOGGER.error(s"$logPrefix Unable to save stat $s2", ex)
            }
            saveFut
          }

        res
      }
    )
  }

}
