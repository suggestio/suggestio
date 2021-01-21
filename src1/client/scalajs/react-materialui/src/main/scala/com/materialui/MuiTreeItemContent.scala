package com.materialui

import japgolly.scalajs.react._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 17:47
  * Description:
  * @see [[https://github.com/mui-org/material-ui/pull/22846]]
  */

/** m-ui v5.alpha23: Content-компонент пока является internal, но его props уже доступны. */
trait MuiTreeItemContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTreeItemContentClasses]
{

  val label,
      icon,
      expansionIcon,
      displayIcon
      : js.UndefOr[raw.React.Node] = js.undefined

  val nodeId: String

}


trait MuiTreeItemContentClasses extends MuiClassesBase {
  val expanded,
      selected,
      focused,
      disabled,
      iconContainer,
      label
      : js.UndefOr[String] = js.undefined
}