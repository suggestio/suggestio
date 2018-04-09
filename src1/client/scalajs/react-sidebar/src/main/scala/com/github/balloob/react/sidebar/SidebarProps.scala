package com.github.balloob.react.sidebar

import japgolly.scalajs.react.raw.React.Node

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.17 12:02
  * Description: Properties for [[Sidebar]] component.
  */

trait SidebarProps extends js.Object {

  val rootClassName       : js.UndefOr[String]          = js.undefined

  val sidebarClassName    : js.UndefOr[String]          = js.undefined

  val contentClassName    : js.UndefOr[String]          = js.undefined

  val overlayClassName    : js.UndefOr[String]          = js.undefined

  val sidebar             : js.UndefOr[Node]   = js.undefined

  val onSetOpen           : js.UndefOr[js.Function1[Boolean, _]]  = js.undefined

  val docked              : js.UndefOr[Boolean]         = js.undefined

  val open                : js.UndefOr[Boolean]         = js.undefined

  val transitions         : js.UndefOr[Boolean]         = js.undefined

  val touch               : js.UndefOr[Boolean]         = js.undefined

  val touchHandleWidth    : js.UndefOr[Int]             = js.undefined

  val dragToggleDistance  : js.UndefOr[Int]             = js.undefined

  val pullRight           : js.UndefOr[Boolean]         = js.undefined

  val shadow              : js.UndefOr[Boolean]         = js.undefined

  val styles              : js.UndefOr[js.Object]       = js.undefined

}
