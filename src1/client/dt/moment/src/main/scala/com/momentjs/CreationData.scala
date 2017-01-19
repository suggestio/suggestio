package com.momentjs

import scala.scalajs.js

/** Result of Moment#creationData(). */
@js.native
class CreationData extends js.Object {

  /** @return "2013-01-02" */
  val input: String = js.native

  /** @return "YYYY-MM-DD" */
  val format: String = js.native

  /** @return Locale obj. */
  val locale: js.Object = js.native

  val isUTC: Boolean = js.native

  val strict: Boolean = js.native

}
