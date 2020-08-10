package io.suggest.sjs.dom2

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.08.2020 17:26
  * Description: var-доступ к dom.File-полям.
  */
@js.native
trait Dom2FileVars extends js.Object {

  var name: js.UndefOr[String] = js.native
  var lastModifiedDate: js.UndefOr[Double] = js.native

}
