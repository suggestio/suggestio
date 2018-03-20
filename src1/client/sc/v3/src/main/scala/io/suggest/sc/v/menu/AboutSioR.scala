package io.suggest.sc.v.menu

import diode.FastEq
import diode.react.ModelProxy
import _root_.io.suggest.sc.GetRouterCtlF
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.Sc3Pages

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 16:04
  * Description: Пункт "О проекте" в левом меню выдачи.
  */
class AboutSioR(
                 getScCssF  : GetScCssF,
                 spaRouter  : GetRouterCtlF
               ) {

  type Props = ModelProxy[Option[PropsVal]]

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
      propsValProxy.value.whenDefinedEl { props =>
        val menuRowsCss = getScCssF().Menu.Rows
        <.div(
          menuRowsCss.rowOuter,

          // Это типа ссылка <a>, но уже с выставленным href + go-событие.
          spaRouter()
            .link( Sc3Pages.MainScreen(
              nodeId = Some( props.aboutNodeId )
            ))(

              menuRowsCss.rowLink,

              ^.title := Messages( MsgCodes.`Suggest.io._transcription` ),

              // Сборка ссылки для входа туда, куда наверное хочет попасть пользователь.

              <.div(
                menuRowsCss.rowContent,
                Messages( MsgCodes.`Suggest.io.Project` )
              )
            )

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props]("AboutSio")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component( propsValProxy )

}