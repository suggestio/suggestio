package io.suggest.n2

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

  def RCVR_KEY_MAXLEN = 7

  /** Неявности всякие живут в отдельном контейнере: */
  object Implicits {

    /** PathBindable для инстанса RcvrKey. */
    implicit def rcvrKeyPb(implicit strB: PathBindable[String]): PathBindable[RcvrKey] = {
      new PathBindableImpl[RcvrKey] {

        override def bind(key: String, value: String): Either[String, RcvrKey] = {
          if (value.isEmpty) {
            Left("error.required")
          } else {
            val nodeIdsArr = value.split('/')
            if (nodeIdsArr.length < 1) {
              Left( "error.empty" )
            } else if (nodeIdsArr.length <= RCVR_KEY_MAXLEN) {
              Right( nodeIdsArr.toList )
            } else {
              Left( "e.rcvrKey.tooLong" )
            }
          }
        }

        override def unbind(key: String, value: RcvrKey): String = {
          value.mkString("/")
        }

      }
    }

  }

}
