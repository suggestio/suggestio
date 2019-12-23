package io.suggest.pay

import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.17 17:36
  * Description: Доп.утиль для [[MPaySystems]] на стороне сервера/jvm.
  */
object MPaySystemsJvm {

  // TODO Заменить опциональный qsb на EnumeratumJvmUtil.valueEnumQsb( MPaySystems )
  /** Поддержка опционального Query String Bindable для инстансов [[MPaySystem]]. */
  implicit def mPaySystemsQsbOpt(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[Option[MPaySystem]] = {
    new QueryStringBindableImpl[Option[MPaySystem]] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[MPaySystem]]] = {
        for {
          strIdOptEith <- strOptB.bind(key, params)
        } yield {
          strIdOptEith.flatMap { strIdOpt =>
            strIdOpt.fold[Either[String, Option[MPaySystem]]] {
              Right(None)
            } { strId =>
              val paySysOpt = MPaySystems.withValueOpt( strId )
              paySysOpt
                .fold[Either[String, Option[MPaySystem]]] (Left("pay.sys.id.invalid")) (_ => Right(paySysOpt))
            }
          }
        }
      }

      override def unbind(key: String, paySysOpt: Option[MPaySystem]): String = {
        strOptB.unbind(key, paySysOpt.map(_.value))
      }

    }
  }

}
