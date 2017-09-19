package io.suggest.css

import scalacss.internal.StyleS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 16:06
  * Description: Внутренняя утиль для ScalaCSS.
  */
object ScalaCssUtil {

  object Implicits {

    implicit class ScssOptionExt[T](val opt: Option[T]) extends AnyVal {
      def whenDefinedStyleS(f: T => StyleS): StyleS = {
        opt.fold(StyleS.empty)(f)
      }
    }

  }

}
