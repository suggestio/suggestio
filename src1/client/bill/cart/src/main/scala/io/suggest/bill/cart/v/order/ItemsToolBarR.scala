package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiTableBodyClasses, MuiToolBar, MuiToolBarClasses, MuiToolBarProps, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import scalacss.ScalaCssReact._
import diode.{FastEq, UseValueEq}
import diode.react.ModelProxy
import io.suggest.bill.cart.m.CartDeleteBtnClick
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.18 15:27
  * Description: Тулбар для функций таблицы.
  */
class ItemsToolBarR(
                     orderCss: OrderCss
                   ) {

  case class PropsVal(
                       countSelected  : Int,
                       isPendingReq   : Boolean
                     )
  implicit object ItemsToolBarRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.countSelected ==* b.countSelected) &&
      (a.isPendingReq ==* b.isPendingReq)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  /** Ядро компонента тулбара. */
  class Backend( $: BackendScope[Props, Unit] ) {

    /** Сигнализировать о клике по кноке удаления. */
    def _onDeleteBtnClick(e: ReactEvent): Callback =
      dispatchOnProxyScopeCB( $, CartDeleteBtnClick )
    lazy val _onDeleteBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onDeleteBtnClick )


    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      val tbCss = new MuiToolBarClasses {
        override val root = orderCss.ItemsTable.ToolBar.root.htmlClass
      }

      MuiToolBar(
        new MuiToolBarProps {
          override val classes = tbCss
          override val disableGutters = true
        }
      )(

        // Кол-во выбранных элементов.
        ReactCommonUtil.maybeNode( props.countSelected > 0 ) {
          VdomArray(

            <.div(
              ^.key := "t",
              orderCss.ItemsTable.ToolBar.title,

              // Кол-во выбранных элементов:
              MuiTypoGraphy.component.withKey("n")(
                new MuiTypoGraphyProps {
                  override val variant = MuiTypoGraphyVariants.subheading
                  override val noWrap = true
                }
              )(
                Messages( MsgCodes.`N.selected`, props.countSelected )
              )
            ),

            <.div(
              ^.key := "s",
              orderCss.ItemsTable.ToolBar.spacer
            ),

            // Кнопка удаления:
            MuiToolTip.component.withKey("d")(
              new MuiToolTipProps {
                override val title: React.Node = Messages( MsgCodes.`Delete` )
                override val placement = MuiToolTipPlacements.Top
              }
            )(
              MuiIconButton(
                new MuiIconButtonProps {
                  override val onClick = _onDeleteBtnClickJsCbF
                  override val disabled = props.isPendingReq
                }
              )(
                Mui.SvgIcons.Delete()()
              )
            )

          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
