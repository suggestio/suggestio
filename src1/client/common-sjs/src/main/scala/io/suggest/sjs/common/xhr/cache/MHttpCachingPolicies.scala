package io.suggest.sjs.common.xhr.cache

import enumeratum._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 15:52
  * Description: Модель политик кэширования.
  */
object MHttpCachingPolicies extends Enum[MHttpCachingPolicy] {

  /** Выключение xhr-кэширования в приложении. */
  case object NetworkOnly extends MHttpCachingPolicy

  /** Сначала запрос, потом - кэш. */
  case object NetworkFirst extends MHttpCachingPolicy

  /** Что быстрее, то и ответ, но ответ - кэшируется. */
  case object Fastest extends MHttpCachingPolicy

  /** Сначала поискать в кэше. Если нет - делать запрос. */
  case object CacheFirst extends MHttpCachingPolicy


  override def values = findValues

}


sealed abstract class MHttpCachingPolicy extends EnumEntry

object MHttpCachingPolicy {

  @inline implicit def univEq: UnivEq[MHttpCachingPolicy] = UnivEq.derive

}
