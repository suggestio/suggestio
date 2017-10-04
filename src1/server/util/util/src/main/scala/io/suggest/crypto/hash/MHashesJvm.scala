package io.suggest.crypto.hash

import io.suggest.enum2.EnumeratumJvmUtil
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 15:40
  * Description: JVM-only утиль для моделей [[MHash]]/[[MHashes]].
  */
object MHashesJvm {

  /** Поддержка значений в URL qs. */
  implicit def mHashQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MHash] = {
    EnumeratumJvmUtil.valueEnumQsb( MHashes )
  }

}
