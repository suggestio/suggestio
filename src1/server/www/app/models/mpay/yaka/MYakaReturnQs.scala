package models.mpay.yaka

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.pay.yaka.YakaConst._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.17 18:40
  * Description: QS-модель, присланных от яндекс-кассы в рамках query-string юзера.
  *
  * Примеры успешных и зафейленных return-ссылок.
  *
  * success(): /pay/yaka/success?orderNumber=617&orderSumAmount=2379.07&cdd_exp_date=1117&
  *  shopArticleId=391660&paymentPayerCode=4100322062290&paymentDatetime=2017-02-14T10%3A38%3A39.751%2B03%3A00&
  *  cdd_rrn=217175135289&external_id=deposit&paymentType=AC&requestDatetime=2017-02-14T10%3A38%3A39.663%2B03%3A00&
  *  depositNumber=97QmIfZ5P9JSF-mqD0uEbYeRXY0Z.001f.201702&nst_eplPayment=true&cdd_response_code=00&cps_user_country_code=PL&
  *  orderCreatedDatetime=2017-02-14T10%3A38%3A38.780%2B03%3A00&sk=yde0255436f276b59f1642648b119b0d0&action=PaymentSuccess&
  *  shopId=84780&scid=548806&rebillingOn=false&orderSumBankPaycash=1003&cps_region_id=2&orderSumCurrencyPaycash=10643&
  *  merchant_order_id=617_140217103753_00000_84780&unilabel=2034c791-0009-5000-8000-00001cc939aa&cdd_pan_mask=444444%7C4448&
  *  customerNumber=rosOKrUOT4Wu0Bj139F1WA&yandexPaymentId=2570071018240&invoiceId=2000001037346
  *
  * fail() /pay/yaka/fail?orderNumber=617&orderSumAmount=2379.07&cdd_exp_date=1117&shopArticleId=391660&paymentPayerCode=4100322062290&
  *  cdd_rrn=320140056033&external_id=deposit&paymentType=AC&requestDatetime=2017-02-13T22%3A10%3A23.246%2B03%3A00
  *  &depositNumber=28oM3Qpkb-0Vv1QCyA5FO0vGOH4Z.001f.201702&nst_eplPayment=true&cps_user_country_code=PL&cdd_response_code=00&orderCreatedDatetime
  *  =2017-02-13T22%3A10%3A22.043%2B03%3A00&sk=u3d6833225b42a97580e3f864b7a78e70&action=PaymentFail&shopId=84780&scid=548806&rebillingOn=false&orde
  *  rSumBankPaycash=1003&cps_region_id=2&orderSumCurrencyPaycash=10643&merchant_order_id=617_130217220753_00000_84780&unilabel=203417c7-0009-5000-
  *  8000-00001cc50c36&cdd_pan_mask=444444%7C4448&customerNumber=rosOKrUOT4Wu0Bj139F1WA&yandexPaymentId=2570070997021&invoiceId=2000001037346
  *
  * Как видно, всё примерно совпадает.
  *
  * @see [[https://tech.yandex.ru/money/doc/payment-solution/shop-config/parameters-docpage/]]
  */
object MYakaReturnQs {

  implicit def qsb(implicit
                   longB      : QueryStringBindable[Long],
                   strB       : QueryStringBindable[String]
                  ): QueryStringBindable[MYakaReturnQs] = {

    val retActionB = MYakaReturnActions.qsb

    new QueryStringBindableImpl[MYakaReturnQs] {

      override def bind(key: String, params: Map[String, Seq[String]]) = {
        for {
          retActionEith <- retActionB.bind(ACTION_FN,         params)
          orderIdEith   <- longB.bind     (ORDER_ID_FN,       params)
          personIdEith  <- strB.bind      (PERSON_ID_FN,      params)
          invoiceIdEith <- longB.bind     (INVOICE_ID_FN,     params)
          shopIdEith    <- longB.bind     (SHOP_ID_FN,        params)
        } yield {
          for {
            retAction <- retActionEith.right
            orderId   <- orderIdEith.right
            personId  <- personIdEith.right
            invoiceId <- invoiceIdEith.right
            shopId    <- shopIdEith.right
          } yield {
            MYakaReturnQs(
              action    = retAction,
              orderId   = orderId,
              personId  = personId,
              invoiceId = invoiceId,
              shopId    = shopId
            )
          }
        }
      }

      // unbind() скорее всего не будет использоваться.
      override def unbind(key: String, value: MYakaReturnQs) = {
        _mergeUnbinded1(
          retActionB.unbind (ACTION_FN,     value.action),
          longB.unbind      (ORDER_ID_FN,   value.orderId),
          strB.unbind       (PERSON_ID_FN,  value.personId),
          longB.unbind      (INVOICE_ID_FN, value.invoiceId),
          longB.unbind      (SHOP_ID_FN,    value.shopId)
        )
      }

    }
  }

}

case class MYakaReturnQs(
                          // Игнорим цены для упрощения и в целях защиты от возможной подмены ссылок.
                          //amount    : Amount_t,
                          //currency  : MCurrency,
                          action    : MYakaAction,
                          orderId   : Gid_t,
                          personId  : String,
                          invoiceId : Long,
                          shopId    : Long
                        )
