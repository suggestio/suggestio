package models.sms.smsRu

import java.time.Instant

import io.suggest.common.empty.OptionUtil
import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.sms.MSmsSend
import play.api.mvc.QueryStringBindable

import scala.concurrent.duration.FiniteDuration

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:36
  * Description: typeclass'ы для sms.ru API.
  */
object MSmsRuSendQs {

  /** qs-поля запроса. */
  object F {
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
    def apiId       = "api_id"
  }


  implicit def msmsRuSendQsb(implicit
                             strB       : QueryStringBindable[String],
                             strOptB    : QueryStringBindable[Option[String]],
                             longB      : QueryStringBindable[Long],
                             longOptB   : QueryStringBindable[Option[Long]],
                            ): QueryStringBindable[MSmsRuSendQs] = {
    new QueryStringBindableImpl[MSmsRuSendQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSmsRuSendQs]] =
        throw new UnsupportedOperationException("Not planned to implement")

      override def unbind(key: String, sms: MSmsRuSendQs): String = {
        _mergeUnbinded1(
          longB.unbind      ( F.json,        1 ),
          strOptB.unbind    ( F.from,        sms.from ),
          longOptB.unbind   ( F.time,        sms.timeAt.map(_.getEpochSecond) ),
          longOptB.unbind   ( F.ttl,         sms.ttl.map(_.toMinutes) ),
          longOptB.unbind   ( F.translit,    OptionUtil.maybe(sms.translit)(1L) ),
          longOptB.unbind   ( F.test,        OptionUtil.maybe(sms.isTest)(1L) ),
          strB.unbind       ( F.apiId,       sms.apiId ),
        )
      }
    }
  }


  /** Извелечение данных из MSmsSend.
    *
    * @param m Данные смс для отсылки.
    * @return Инстанс [[MSmsRuSendQs]].
    */
  def from(m: MSmsSend, apiId: String, fromDflt: Option[String]): MSmsRuSendQs = {
    MSmsRuSendQs(
      from      = m.from orElse fromDflt,
      timeAt    = m.timeAt,
      ttl       = m.ttl,
      translit  = m.translit,
      isTest    = m.isTest,
      apiId     = apiId,
    )
  }

}


case class MSmsRuSendQs(
                         from       : Option[String],
                         timeAt     : Option[Instant],
                         ttl        : Option[FiniteDuration],
                         translit   : Boolean,
                         isTest     : Boolean,
                         apiId     : String,
                       )
