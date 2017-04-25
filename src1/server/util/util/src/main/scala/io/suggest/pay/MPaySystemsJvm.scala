package io.suggest.pay

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.17 17:36
  * Description: Доп.утиль для [[MPaySystems]] на стороне сервера/jvm.
  */
object MPaySystemsJvm {

  /** Поддержка опционального Query String Bindable для инстансов [[MPaySystem]]. */
  implicit def mPaySystemsQsbOpt(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[Option[MPaySystem]] = {
    new QueryStringBindableImpl[Option[MPaySystem]] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[MPaySystem]]] = {
        for {
          strIdOptEith <- strOptB.bind(key, params)
        } yield {
          strIdOptEith.right.flatMap { strIdOpt =>
            val paySysOpt = strIdOpt.flatMap ( MPaySystems.withNameOption )
            paySysOpt
              .fold[Either[String, Option[MPaySystem]]] (Left("e.invalid")) (_ => Right(paySysOpt))
          }
        }
      }

      override def unbind(key: String, value: Option[MPaySystem]): String = {
        strOptB.unbind(key, value.map(_.strId))
      }
    }
  }

}
