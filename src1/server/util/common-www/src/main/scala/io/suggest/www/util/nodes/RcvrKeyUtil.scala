package io.suggest.www.util.nodes

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.model.play.psb.PathBindableImpl
import play.api.mvc.PathBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 11:56
  * Description: Утиль для серверной работы с моделью RcvrKey.
  */
object RcvrKeyUtil {

  /** Неявности всякие живут в отдельном контейнере: */
  object Implicits {

    /** PathBindable для инстанса RcvrKey. */
    implicit def rcvrKeyPb(implicit strB: PathBindable[String]): PathBindable[RcvrKey] = {
      new PathBindableImpl[RcvrKey] {

        override def bind(key: String, value: String): Either[String, RcvrKey] = {
          if (value.isEmpty) {
            Left("error.required")
          } else {
            val rcvrKey: RcvrKey = {
              value
                .split('/')
                .toList
            }
            Right(rcvrKey)
          }
        }

        override def unbind(key: String, value: RcvrKey): String = {
          value.mkString("/")
        }

      }
    }

  }

}
