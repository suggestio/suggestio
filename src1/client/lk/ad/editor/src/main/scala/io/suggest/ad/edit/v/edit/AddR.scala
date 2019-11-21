package io.suggest.ad.edit.v.edit

import com.materialui.{MuiFab, MuiFabProps, MuiFabVariants, MuiSvgIconProps, MuiSvgIcons}
import diode.react.ModelProxy
import scalacss.ScalaCssReact._
import io.suggest.ad.edit.m.{AddBlockClick, AddBlockLessContentClick, AddContentClick}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.09.17 14:58
  * Description: Кнопка-форма добавления стрипа.
  */
class AddR(
            lkAdEditCss        : LkAdEditCss,
          ) {

  type Props = ModelProxy[_]


  class Backend($: BackendScope[Props, Unit]) {

    private val onAddContentClick: Callback =
      dispatchOnProxyScopeCB($, AddContentClick)
    private val onAddContentClickJsCbF =
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => onAddContentClick }

    private val onAddBlockClick: Callback =
      dispatchOnProxyScopeCB($, AddBlockClick)
    private val onAddBlockClickJsCbF =
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => onAddBlockClick }

    private val onAddBlockLessContentClick: Callback =
      dispatchOnProxyScopeCB($, AddBlockLessContentClick)
    private val onAddBlockLessContentClickJsCbF =
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => onAddBlockLessContentClick }


    def render(p: Props): VdomElement = {
      val svgProps = new MuiSvgIconProps {
        override val className = lkAdEditCss.Layout.fabIcon.htmlClass
      }
      val br = <.br

      <.div(
        lkAdEditCss.Layout.addCont,

        MuiFab {
          new MuiFabProps {
            override val onClick = onAddBlockClickJsCbF
            override val variant = MuiFabVariants.extended
          }
        } (
          MuiSvgIcons.BorderOuter(svgProps)(),
          Messages( MsgCodes.`Block` ),
        ),

        br, br,

        MuiFab {
          new MuiFabProps {
            override val onClick = onAddContentClickJsCbF
            override val variant = MuiFabVariants.extended
          }
        } (
          MuiSvgIcons.ShortText(svgProps)(),
          Messages( MsgCodes.`Content` ),
        ),

        br, br,

        // Кнопка "описание" - это внеблоковый контент.:
        MuiFab {
          new MuiFabProps {
            override val onClick = onAddBlockLessContentClickJsCbF
            override val variant = MuiFabVariants.extended
          }
        } (
          MuiSvgIcons.Subject(svgProps)(),
          Messages( MsgCodes.`Description` ),
        ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(addSProxy: Props) = component(addSProxy)

}
