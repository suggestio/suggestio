package io.suggest.es.model

import io.suggest.model.play.psb.PathBindableImpl
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 17:55
  * Description: JVM-only поддержка для модели MEsUuId.
  */
object MEsUuIdJvm {

  /** Поддержка биндинга из/в qs. */
  implicit def nEsUuIdQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MEsUuId] = {
    new QueryStringBindableImpl[MEsUuId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsUuId]] = {
        for (esIdE <- strB.bind(key, params)) yield {
          esIdE.right
            .flatMap(MEsUuId.fromStringEith)
        }
      }

      override def unbind(key: String, value: MEsUuId): String = {
        strB.unbind(key, value.id)
      }
    }
  }


  /** PathBinadable для биндинга значения id прямо из URL path. */
  implicit def psb(implicit strB: PathBindable[String]): PathBindable[MEsUuId] = {
    new PathBindableImpl[MEsUuId] {
      override def bind(key: String, value: String): Either[String, MEsUuId] = {
        MEsUuId.fromStringEith(value)
      }
      override def unbind(key: String, value: MEsUuId): String = {
        value
      }
    }
  }

}
