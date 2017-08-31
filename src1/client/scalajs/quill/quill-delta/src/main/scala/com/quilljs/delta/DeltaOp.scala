package com.quilljs.delta

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:15
  * Description: Single delta operation JSON.
  */

@js.native
trait DeltaOp extends js.Object {

  /** undefined | String | Int | js.Object*/
  var insert: UndefOr[js.Any] = js.undefined

  var attributes: UndefOr[DeltaOpAttrs] = js.undefined

  var delete: UndefOr[Int] = js.undefined

  var retain: UndefOr[Int] = js.undefined

}

