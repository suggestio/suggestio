package models.req

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.03.17 11:02
  * Description: Аргумент для вызова [[util.acl.BruteForceProtect]].
  */
object BfpArgs {

  /** Дефолтовые настройки противодействия брут-форсам. */
  def default = apply()

  def tryCountDivisor = GenLens[BfpArgs](_.tryCountDivisor)

  @inline implicit def univEq: UnivEq[BfpArgs] = UnivEq.derive

}

case class BfpArgs(
                   lagMs              : Int       = 111,
                   attackLagMs        : Int       = 2000,
                   tryCountDivisor    : Int       = 5,
                   cachePrefix        : String    = "bfp:",
                   cacheTtl           : Duration  = 30.second,
                   tryCountDeadline   : Int       = 100
                  ) {

  def withTryCountDivisor(tcDiv: Int) = copy(tryCountDivisor = tcDiv)

}
