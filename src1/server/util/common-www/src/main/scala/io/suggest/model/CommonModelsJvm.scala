package io.suggest.model

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.model.n2.media.storage.{MStorage, MStorages}
import _root_.play.api.mvc.QueryStringBindable
import io.suggest.sc.{MScApiVsn, MScApiVsns}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 8:25
  * Description: Контейнер jvm-only данных для разных пошаренных common-моделей.
  */
object CommonModelsJvm {

  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] =
    EnumeratumJvmUtil.valueEnumQsb( MScApiVsns )

  /** QSB для инстансов [[MStorage]]. */
  implicit def mStorageQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MStorage] =
    EnumeratumJvmUtil.valueEnumQsb( MStorages )

}
