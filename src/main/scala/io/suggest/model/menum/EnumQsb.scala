package io.suggest.model.menum

import io.suggest.common.menum.{IMaybeWithName, StrIdValT}
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.05.16 10:17
  * Description: Аддон для enum-моделей, желающих биндинг для обязательного значения qs-аргумента.
  */
trait EnumQsb extends StrIdValT with IMaybeWithName {

  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for (strEith <- strB.bind(key, params)) yield {
          strEith.right.flatMap { strId =>
            maybeWithName(strId).fold [Either[String, T]] {
              Left("error.invalid")
            } { v =>
              Right(v)
            }
          }
        }
      }

      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

}
