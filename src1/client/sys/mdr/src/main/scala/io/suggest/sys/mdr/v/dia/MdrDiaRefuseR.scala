package io.suggest.sys.mdr.v.dia

import com.materialui.{Mui, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiFab, MuiFabProps, MuiFabVariants, MuiLinearProgress, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.{DismissCancelClick, DismissOkClick, MMdrRefuseDialogS, SetDismissReason}
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 22:36
  * Description: Компонент диалога отказа от размещения.
  */
class MdrDiaRefuseR {

  case class PropsVal(
                       state        : MMdrRefuseDialogS,
                       dismissReq   : Pot[_]
                     )
  implicit object MdrDiaRefuseRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.state ===* b.state) &&
      (a.dismissReq ===* b.dismissReq)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Ввод текста в поле причины. */
    private def _onReasonChanged(e: ReactEventFromInput): Callback = {
      val reason2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SetDismissReason(reason2))
    }
    private val _onReasonChangedF = ReactCommonUtil.cbFun1ToJsCb( _onReasonChanged )


    /** Клик по кнопке подтверждения отказа в размещении. */
    private def _onRefuseClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DismissOkClick)
    private val _onRefuseClickF = ReactCommonUtil.cbFun1ToJsCb( _onRefuseClick )


    /** Клик по кнопке закрытия диалога. */
    private def _onCancelClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DismissCancelClick)
    private val _onCancelClickF = ReactCommonUtil.cbFun1ToJsCb( _onCancelClick )


    def render(propsOptProxy: Props): VdomElement = {
      val props = propsOptProxy.value

      val inputsDisabled = props.dismissReq.isPending
      val refuseMsg = Messages( MsgCodes.`Refuse` )
      val isShown = props.state.actionInfo.isDefined

      MuiDialog(
        new MuiDialogProps {
          override val open = isShown
          override val onEscapeKeyDown = _onCancelClickF
          override val maxWidth = js.defined( MuiDialogMaxWidths.md )
          override val fullWidth = true
        }
      )(
        // Заголовок диалога
        MuiDialogTitle()(
          refuseMsg
          // TODO Добавить подробности?
        ),

        // Тело диалога
        MuiDialogContent()(

          MuiTextField(
            new MuiTextFieldProps {
              override val autoFocus = isShown
              override val placeholder = Messages( MsgCodes.`Reason` )
              override val `type` = HtmlConstants.Input.text
              override val value = js.defined( props.state.reason )
              override val onChange = _onReasonChangedF
              override val disabled = inputsDisabled
              override val fullWidth = true
            }
          )(),

          // При ожидании запроса - рендерить прогресс-бар:
          props.dismissReq.renderPending { _ =>
            MuiLinearProgress()
          },

          // При ошибках - тоже что-нибудь отрендерить:
          props.dismissReq.renderFailed { ex =>
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val color = MuiColorTypes.secondary
              }
            )(
              <.br,
              <.br,
              Messages( MsgCodes.`Error` ),
              <.br,
              ex.toString()
            )
          },

        ),


        // Кнопки действий диалога:
        MuiDialogActions()(

          // Кнопка отмены. Активная всегда, чтобы на зависших запросах можно было тоже закрыть окно.
          MuiFab(
            new MuiFabProps {
              override val variant = MuiFabVariants.extended
              override val color = MuiColorTypes.secondary
              override val onClick = _onCancelClickF
            }
          )(
            Mui.SvgIcons.Undo()(),
            Messages( MsgCodes.`Cancel` )
          ),

          // Кнопка подтверждения отказа в размещении.
          MuiFab(
            new MuiFabProps {
              override val variant = MuiFabVariants.extended
              override val color = MuiColorTypes.primary
              override val onClick = _onRefuseClickF
              override val disabled = inputsDisabled
            }
          )(
            Mui.SvgIcons.DeleteForever()(),
            refuseMsg
          ),

        ),

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
