package io.suggest.sc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.17 10:43
  * Description: JVM-утиль для модели MScApiVsns.
  */
object MScApiVsnsJvm extends MacroLogsImpl {

  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] = {
    new QueryStringBindableImpl[MScApiVsn] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScApiVsn]] = {
        val optRes = for {
          maybeVsn <- intB.bind(key, params)
        } yield {
          maybeVsn.right.flatMap { vsnNum =>
            MScApiVsns.withValueOpt(vsnNum).toRight {
              // Довольно неожиданная ситуация, что выкинута версия, используемая на клиентах. Или ксакеп какой-то ковыряется.
              val msg = "Unknown API version: " + vsnNum
              LOGGER.warn(msg)
              msg
            }
          }
        }
        // Если версия не задана вообще, то выставить её в дефолтовую. Первая выдача не возвращала никаких версий.
        optRes.orElse {
          val vsn = MScApiVsns.unknownVsn
          LOGGER.warn(s"Sc API vsn undefined, will try $vsn")
          Some( Right( vsn ) )
        }
      }

      override def unbind(key: String, value: MScApiVsn): String = {
        intB.unbind(key, value.versionNumber)
      }

    }
  }

}
