package com.github.balloob.react.sidebar

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.17 12:15
  * Description: Interface for styles object.
  * @see [[https://github.com/balloob/react-sidebar#styles]]
  */
trait SidebarStyles extends js.Object {

  val root        : js.UndefOr[js.Object] = js.undefined
  val sidebar     : js.UndefOr[js.Object] = js.undefined
  val content     : js.UndefOr[js.Object] = js.undefined
  val overlay     : js.UndefOr[js.Object] = js.undefined
  val dragHandle  : js.UndefOr[js.Object] = js.undefined

}
