package controllers.pay

import com.google.inject.Inject
import controllers.SioControllerImpl
import io.suggest.bill.{MCurrencies, MPrice}
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
  private def _getPayPrice(orderId: Gid_t)(implicit request: IReq[_]): Future[MPrice] = {
    // Посчитать, сколько юзеру надо доплатить за вычетом остатков по балансам.
    val payPricesFut = bill2Util.getPayPrices(orderId, request.user.mBalancesFut)

    lazy val logPrefix = s"_getPayPrice($orderId):"

    // Собираем данные для рендера формы отправки в платёжку.
    for {
      payPrices     <- payPricesFut
    } yield {
      val ppsSize = payPrices.length
      if (ppsSize == 0) {
        // Нечего платить. По идее, деньги должны были списаться на предыдущем шаге
        throw new IllegalStateException(s"$logPrefix Nothing to pay remotely via yaka. User balances are enought.")

      } else if (ppsSize == 1) {
        implicit val ctx = implicitly[Context]

        val pp = payPrices.head
        if (pp.currency == MCurrencies.RUB) {
          // Цена в одной единственной валюте, которая поддерживается яндекс-кассой. Вернуть её наверх.
          pp

        } else {
          // Какая-то валюта, которая не поддерживается яндекс-кассой.
          throw new IllegalArgumentException(s"$logPrefix Yaka unsupported currency: ${pp.currency}. Price = $pp")
        }

      } else {
        // Сразу несколько валют. Не поддерживается яндекс.кассой.
        throw new UnsupportedOperationException(s"$logPrefix too many currencies need to pay, but yaka supports only RUB:\n$payPrices")
      }
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
    if (mCommonDi.isProd && request.cookies.nonEmpty) {
      LOGGER.error(s"_logIfHasCookies[${System.currentTimeMillis()}]: Unexpected cookies ${request.cookies} from client ${request.remoteAddress}, personId = ${request.user.personIdOpt}")
      val ma = MAction(
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
      Some(ma)

    } else {
      None
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

            // Проверяем связь юзера и ордера...
            val usrNodeOptFut = mNodesCache.getById(yReq.personId)
            val yReqOrderContractIdOptFut = slick.db.run {
              mOrders.getContractId( yReq.orderId )
            }

            val isOrderMatchUserFut = for {
              usrNodeOpt <- usrNodeOptFut
              yReqOrderContractIdOpt <- yReqOrderContractIdOptFut
            } yield {
              val opt = for {
                usrNode             <- usrNodeOpt
                yReqOrderContractId <- yReqOrderContractIdOpt
              } yield {
                usrNode.billing.contractId.contains( yReqOrderContractId )
              }
              opt.contains(true)
            }

            // Проверяем стоимость размещения.
            val isPayPriceOkFut = for (payPrice <- payPriceFut) yield {
              // Сопоставить валюты...
              payPrice.currency == yReq.currency && {
                // Double сравнивать -- дело неблагодатное. Поэтому сравниваем в рамках погрешности: а это - 1 копейка
                val maxDiff = Math.pow(10, -payPrice.currency.exponent)
                Math.abs(payPrice.amount - yReq.amount) < maxDiff
              }
            }

            for {
              isOrderMatchUser <- isOrderMatchUserFut
              isPayPriceOk     <- isPayPriceOkFut
            } yield {
              if (isOrderMatchUser && isPayPriceOk) {
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
