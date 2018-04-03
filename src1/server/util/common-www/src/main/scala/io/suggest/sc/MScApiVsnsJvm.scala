package io.suggest.sc

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.17 10:43
  * Description: JVM-утиль для модели MScApiVsns.
  */
object MScApiVsnsJvm extends MacroLogsImpl {

  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] = {
    EnumeratumJvmUtil.valueEnumQsb( MScApiVsns )
  }

}
