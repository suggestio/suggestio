package io.suggest.sc.sjs.m

import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.raw.HTMLDivElement

import scala.concurrent.Promise

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 15:47
 */
package object mwc {

  type WcHidePromise_t = Promise[None.type]

  type SafeRootDiv_t = SafeEl[HTMLDivElement]

}
