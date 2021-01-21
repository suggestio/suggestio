package io.suggest.sys.mdr.v.dia

import com.materialui.{Mui, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiFab, MuiFabProps, MuiFabVariants, MuiLinearProgress, MuiModalCloseReason, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps}
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
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

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
    private val _onReasonChangedF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val reason2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SetDismissReason(reason2))
    }


    /** Клик по кнопке подтверждения отказа в размещении. */
    private val _onRefuseClickF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DismissOkClick)
    }


    /** Клик по кнопке закрытия диалога. */
    private def _onCancelClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DismissCancelClick)
    private val _onCancelClickF = ReactCommonUtil.cbFun1ToJsCb( _onCancelClick )


    private val _onDiaCloseCbF = ReactCommonUtil.cbFun2ToJsCb { (e: ReactEvent, reason: String) =>
      if (reason ==* MuiModalCloseReason.escapeKeyDown) {
        _onCancelClick(e)
      } else {
        Callback.empty
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      val props = propsOptProxy.value

      val inputsDisabled = props.dismissReq.isPending
      val refuseMsg = Messages( MsgCodes.`Refuse` )
      val isShown = props.state.actionInfo.isDefined

      MuiDialog(
        new MuiDialogProps {
          override val open = isShown
          @JSName("onClose")
          override val onClose2 = _onDiaCloseCbF
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
              override val variant = MuiTextField.Variants.standard
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
