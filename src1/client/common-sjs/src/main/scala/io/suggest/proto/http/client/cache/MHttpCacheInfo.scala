package io.suggest.proto.http.client.cache

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
    UnivEq.derive
  }

}


/** Требования по кэшированию.
  *
  * @param policy Политика кэширования.
  * @param rewriteUrl Ссылка, под которой надо сохранять в кэш.
  *                   По задумке, было множество ссылок, но IndexedDB требует на это отдельное разрешение и доп.пиление.
  */
case class MHttpCacheInfo(
                           policy           : MHttpCachingPolicy      = MHttpCachingPolicies.NetworkOnly,
                           rewriteUrl       : Option[String]          = None,
                           // TODO Надо предусмотреть возможность догоняющего network-ответа, когда кэш уже ответ вернул (и основной resp Future завершен).
                         )
