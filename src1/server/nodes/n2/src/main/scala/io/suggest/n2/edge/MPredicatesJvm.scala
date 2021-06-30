package io.suggest.n2.edge

import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 17:49
  * Description: JVM-only утиль для модели MPredicates.
  */
object MPredicatesJvm {

  /** Поддержка URL qs-биндинга из play routes. */
  implicit def mPredicateQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MPredicate] = {
    new AbstractQueryStringBindable[MPredicate] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MPredicate]] = {
        for (strIdEith <- strB.bind(key, params)) yield {
          strIdEith.flatMap { strId =>
            MPredicates.withValueOpt(strId)
              .toRight("e.predicate.unknown")
          }
        }
      }

      override def unbind(key: String, value: MPredicate): String = {
        strB.unbind(key, value.value)
      }

    }
  }

}
