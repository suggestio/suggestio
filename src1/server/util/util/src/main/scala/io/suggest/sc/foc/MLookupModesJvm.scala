package io.suggest.sc.foc

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.sc.focus.{MLookupMode, MLookupModes}
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.11.17 21:40
  * Description: jvm-only утиль для модели MLookupModes.
  */
object MLookupModesJvm {

  /** Поддержка QueryStringBindable. */
  implicit def mLookupModeQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MLookupMode] = {
    EnumeratumJvmUtil.valueEnumQsb( MLookupModes )
  }

}
