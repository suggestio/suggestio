package io.suggest.ueq

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:03
  * Description: UnivEq support for some scalajs-react data-types.
  */
object ReactUnivEqUtil {

  @inline implicit def tagModUnivEq: UnivEq[TagMod] = UnivEq.force

  @inline implicit def callbackUnivEq: UnivEq[Callback] = UnivEq.force

}
