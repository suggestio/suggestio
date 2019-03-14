package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react.{Children, JsComponent, raw}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 21:47
  * Description: Mui one-tab btn API inside [[MuiTabs]].
  */
object MuiTab {

  val component = JsComponent[MuiTabProps, Children.None, Null]( Mui.Tab )

  def apply(props: MuiTabProps) = component(props)

}


trait MuiTabProps
  extends MuiButtonBaseCommonProps
  with MuiPropsBaseClasses[MuiTabClasses]
{
  val icon : js.UndefOr[raw.React.Node] = js.undefined
  val label: js.UndefOr[raw.React.Node] = js.undefined
  /** You can provide your own value. Otherwise, we fallback to the child position index. */
  val value: js.UndefOr[js.Any] = js.undefined
}


trait MuiTabClasses extends MuiClassesBase {
  val labelIcon: js.UndefOr[String] = js.undefined
  val textColorInherit: js.UndefOr[String] = js.undefined
  val textColorPrimary: js.UndefOr[String] = js.undefined
  val textColorSecondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val selected: js.UndefOr[String] = js.undefined
  val fullWidth: js.UndefOr[String] = js.undefined
  val wrapper: js.UndefOr[String] = js.undefined
  val labelContainer: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
  val labelWrapped: js.UndefOr[String] = js.undefined
}
