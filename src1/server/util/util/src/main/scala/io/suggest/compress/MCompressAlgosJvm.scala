package io.suggest.compress

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 22:14
  * Description: JVM-only утиль для модели MCompressAlgos.
  */
object MCompressAlgosJvm {

  implicit def compressAlgoQsb(implicit strB: QueryStringBindable[String]): CrossQsBindable[MCompressAlgo] = {
    EnumeratumJvmUtil.valueEnumQsb( MCompressAlgos )
  }

}
