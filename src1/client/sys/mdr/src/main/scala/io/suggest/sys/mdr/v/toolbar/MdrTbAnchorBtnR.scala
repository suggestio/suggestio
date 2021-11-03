package io.suggest.sys.mdr.v.toolbar

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiLink, MuiLinkProps, MuiSvgIcon, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.routes.{PlayRoute, routes}
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
                       route      : PlayRoute,
                       icon       : MuiSvgIcon,
                       disabled   : Boolean,
                     )
  object PropsVal {
    def SysNodeShow(nodeId: String, disabled: Boolean) =
      PropsVal( MsgCodes.`Node`, routes.controllers.SysMarket.showAdnNode( nodeId ), Mui.SvgIcons.Settings, disabled )

    def Edit(route: PlayRoute, disabled: Boolean) = PropsVal(MsgCodes.`Edit`, route, Mui.SvgIcons.Edit, disabled)
    def LkAdEdit(nodeId: String, disabled: Boolean) = Edit( routes.controllers.LkAdEdit.editAd( nodeId ), disabled )
    def LkAdnEdit(nodeId: String, disabled: Boolean) = Edit( routes.controllers.LkAdnEdit.editNodePage( nodeId ), disabled )

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
        MuiLink(
          new MuiLinkProps {
            val href = p.route.url
            val target = "_blank"
          }
        )(
          MuiIconButton(
            new MuiIconButtonProps {
              override val disabled = p.disabled
            }
          )(
            p.icon()(),
          ),
        ),
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
