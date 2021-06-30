package io.suggest.captcha

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.19 14:30
  * Description: JVM-only поддержа для моделей капч.
  */
object MCaptchaJvm {

  implicit def captchaCookiePathQsb: CrossQsBindable[MCaptchaCookiePath] =
    EnumeratumJvmUtil.valueEnumQsb( MCaptchaCookiePaths )

}
