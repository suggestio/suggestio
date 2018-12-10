package io.suggest.sjs.common.xhr.cache

import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 16:03
  * Description: Контейнер данных по кэшированию.
  */
object MHttpCacheInfo {

  def default = apply()

  @inline implicit def univEq: UnivEq[MHttpCacheInfo] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}

case class MHttpCacheInfo(
                           policy           : MHttpCachingPolicy   = MHttpCachingPolicies.NetworkOnly,
                           rewriteUrl       : Iterable[String]     = Nil
                         )
