package models.sms.smsRu

import io.suggest.common.empty.OptionUtil
import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.sms.MSmsSend
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:36
  * Description: typeclass'ы для sms.ru API.
  */
object MSmsRuApi {

  /** qs-поля запроса. */
  object Qs {
    def to          = "to"
    def msg         = "msg"
    def json        = "json"
    def from        = "from"
    def time        = "time"
    def ttl         = "ttl"
    def daytime     = "daytime"
    def translit    = "translit"
    def test        = "test"
    def partnerId   = "partner_id"
  }


  implicit def msmsQsb(implicit
                       strB       : QueryStringBindable[String],
                       strOptB    : QueryStringBindable[Option[String]],
                       longB      : QueryStringBindable[Long],
                       longOptB   : QueryStringBindable[Option[Long]],
                      ): QueryStringBindable[MSmsSend] = {
    new QueryStringBindableImpl[MSmsSend] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSmsSend]] =
        throw new UnsupportedOperationException("Not planned to implement")

      override def unbind(key: String, sms: MSmsSend): String = {
        _mergeUnbinded1(
          // TODO msgs
          //strB.unbind       ( Qs.to,          sms.numbers.mkString(",") ),
          //strB.unbind       ( Qs.msg,         sms.text ),
          longB.unbind      ( Qs.json,        1 ),
          strOptB.unbind    ( Qs.from,        sms.from ),
          longOptB.unbind   ( Qs.time,        sms.timeAt.map(_.toEpochSecond) ),
          longOptB.unbind   ( Qs.ttl,         sms.ttl.map(_.toMinutes) ),
          longOptB.unbind   ( Qs.translit,    OptionUtil.maybe(sms.translit)(1L) ),
          longOptB.unbind   ( Qs.test,        OptionUtil.maybe(sms.isTest)(1L) ),
        )
      }
    }
  }

}
