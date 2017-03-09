package models.req

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.03.17 11:02
  * Description: Аргумент для вызова [[util.acl.BruteForceProtect]].
  */
case class BfpArgs(
                   lagMs              : Int       = 222,
                   attackLagMs        : Int       = 2000,
                   tryCountDivisor    : Int       = 2,
                   cachePrefix        : String    = "bfp:",
                   cacheTtl           : Duration  = 30.second,
                   tryCountDeadline   : Int       = 40
                  ) {

  def withCachePrefix(cp: String) = copy(cachePrefix = cp)
  def withTryCountDivisor(tcDiv: Int) = copy(tryCountDivisor = tcDiv)
  def withTryCountDeadline(tcDead: Int) = copy(tryCountDeadline = tcDead)

}
