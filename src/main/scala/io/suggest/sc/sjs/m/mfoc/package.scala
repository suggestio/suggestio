package io.suggest.sc.sjs.m

import io.suggest.sc.sjs.m.msrv.foc.find.IFocAd

import scala.collection.immutable.Queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 11:48
 * Description:
 */
package object mfoc {

  type FAdQueue         = Queue[IFocAd]

  type CarState         = List[FAdShown]

}
