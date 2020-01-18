package io.suggest.sc.v.menu

import diode.FastEq
import diode.react.ModelProxy
import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.sc3.Sc3Pages
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 16:04
  * Description: Пункт "О проекте" в левом меню выдачи.
  */
class AboutSioR(
                 scReactCtxP            : React.Context[MScReactCtx],
                 crCtxProv     : React.Context[MCommonReactCtx],
               ) {

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class PropsVal(
                       aboutNodeId  : String,
                     )
  implicit object AboutSioRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.aboutNodeId ===* b.aboutNodeId
    }
  }


  class Backend($: BackendScope[Props, Unit]) {
    def render(propsValProxy: Props): VdomElement = {
      scReactCtxP.consume { scReactCtx =>
        val R = ScCssStatic.Menu.Rows
        lazy val _listItem = MuiListItem(
          new MuiListItemProps {
            override val disableGutters = true
            override val button = true
          }
        )(
          MuiListItemText()(
            <.span(
              R.rowContent,
              scReactCtx.scCss.fgColor,
              crCtxProv.message( MsgCodes.`Suggest.io.Project` ),
            )
          )
        )

        crCtxProv.consume { crCtx =>
          lazy val linkChildren = List[TagMod](
            R.rowLink,
            ^.title := crCtx.messages( MsgCodes.`Suggest.io._transcription` ),
            _listItem,
          )

          propsValProxy.value.whenDefinedEl { props =>
            // Это типа ссылка <a>, но уже с выставленным href + go-событие.
            scReactCtx
              .routerCtl
              .link( Sc3Pages.MainScreen(
                nodeId = Some( props.aboutNodeId )
              ))(
                linkChildren: _*
              )
          }
        }
      }
    }
  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component( propsValProxy )

}
