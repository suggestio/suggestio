package io.suggest.common

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.10.2019 1:46
  */
object BooleanUtil {

  object Implicits {

    implicit class BoolOpsExt( val bool: Boolean ) extends AnyVal {

      def invertIf(isInvert: Boolean): Boolean =
        if (isInvert) !bool
        else bool

    }

  }

}
