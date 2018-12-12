package io.suggest.sys.mdr.v.toolbar

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiSvgIcon, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.proto.http.model.Route
import io.suggest.routes.routes
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.18 18:38
  * Description: Кнопка-ссылка на панели инструментов.
  */
class MdrTbAnchorBtnR {

  case class PropsVal(
                       hintCode   : String,
                       route      : Route,
                       icon       : MuiSvgIcon,
                     )
  object PropsVal {
    def SysNodeShow(nodeId: String) =
      PropsVal( MsgCodes.`Node`, routes.controllers.SysMarket.showAdnNode( nodeId ), Mui.SvgIcons.Settings )

    def Edit(route: Route ) = PropsVal(MsgCodes.`Edit`, route, Mui.SvgIcons.Edit)
    def LkAdEdit(nodeId: String) = Edit( routes.controllers.LkAdEdit.editAd( nodeId ) )
    def LkAdnEdit(nodeId: String) = Edit( routes.controllers.LkAdnEdit.editNodePage( nodeId ) )

    @inline implicit def univEq: UnivEq[PropsVal] = UnivEq.force
  }
  implicit object MdrPanelAnchorBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.hintCode ===* b.hintCode) &&
      (a.route ===* b.route) &&
      (a.icon eq b.icon)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value

      MuiToolTip(
        new MuiToolTipProps {
          override val title: React.Node = Messages( p.hintCode )
        }
      )(
        <.a(
          ^.href := p.route.url,
          ^.target.blank,
          MuiIconButton()(
            p.icon()()
          )
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def _apply(propsProxy: Props) = component( propsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
