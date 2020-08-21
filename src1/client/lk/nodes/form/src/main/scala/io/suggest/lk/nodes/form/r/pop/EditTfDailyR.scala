package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiFormControl, MuiFormControlLabel, MuiFormControlLabelProps, MuiInput, MuiInputAdornment, MuiInputAdornmentPositions, MuiInputAdornmentProps, MuiInputLabel, MuiInputLabelProps, MuiInputProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiRadio, MuiRadioGroup, MuiRadioGroupProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.{MCurrencies, MCurrency}
import io.suggest.bill.tf.daily.ITfDailyMode
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.OptFastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:57
  * Description: Компонент попапа редактирования тарифа.
  */
class EditTfDailyR(
                    platformCssStatic     : () => PlatformCssStatic,
                    platformComponents    : PlatformComponents,
                    crCtxP                : React.Context[MCommonReactCtx],
                  ) {

  type Props = ModelProxy[Option[MEditTfDailyS]]


  case class State(
                    diaOpenedSomeC              : ReactConnectProxy[Some[Boolean]],
                    saveBtnDisabledSomeC        : ReactConnectProxy[Some[Boolean]],
                    tfModeIdC                   : ReactConnectProxy[String],
                    tfManualAmountC             : ReactConnectProxy[Option[MTextFieldS]],
                    tfManualCurrencyC           : ReactConnectProxy[MCurrency],
                    isPendingSomeC              : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC               : ReactConnectProxy[Option[Throwable]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onModeChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val modeId = e.target.value
      dispatchOnProxyScopeCB( $, TfDailyModeChanged(modeId) )
    }

    private val _onManualAmountChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val v = e.target.value
      dispatchOnProxyScopeCB( $, TfDailyManualAmountChanged(v) )
    }

    private val _onSaveClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, TfDailySaveClick )
    }

    private val _onCloseClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, TfDailyCancelClick )
    }


    def render(s: State): VdomElement = {
      val platCss = platformCssStatic()

      // id для связки label+input ниже
      val mfcId = MsgCodes.`Set.manually`

      val currencyAdornment = MuiInputAdornment(
        new MuiInputAdornmentProps {
          override val position = MuiInputAdornmentPositions.start
        }
      )(
        s.tfManualCurrencyC { tfManualCurrencyProxy =>
          <.span(
            tfManualCurrencyProxy.value.symbol,
          )
        },
      )

      val perDayAdornment = MuiInputAdornment(
        new MuiInputAdornmentProps {
          override val position = MuiInputAdornmentPositions.end
        }
      )(
        crCtxP.message( MsgCodes.`_per_.day` ),
      )

      // Инпут для ввода значения тарифа вручную:
      val inputRow = MuiFormControl()(
        MuiInputLabel(
          new MuiInputLabelProps {
            override val htmlFor = mfcId
          }
        )(
          crCtxP.message( MsgCodes.`Cost` ),
        ),

        s.tfManualAmountC { tfManualAmountProxy =>
          val mtfOpt = tfManualAmountProxy.value
          val _isError = mtfOpt.fold(true) { m =>
            !m.isValidNonEmpty
          }
          val _textValue = mtfOpt.fold("")(_.value)
          MuiInput(
            new MuiInputProps {
              override val id               = mfcId
              override val error            = _isError
              override val value            = _textValue
              override val onChange         = _onManualAmountChangeCbF
              override val required         = true
              override val startAdornment   = currencyAdornment.rawNode
              override val endAdornment     = perDayAdornment.rawNode
            }
          )
        },
      )

      // radio "Наследовать"
      val radioInherit: VdomElement = MuiFormControlLabel(
        new MuiFormControlLabelProps {
          override val control = MuiRadio().rawElement
          override val value = ITfDailyMode.ModeId.Inherit
          override val label = crCtxP.message( MsgCodes.`Inherited` ).rawNode
        }
      )

      // radio "Вручную"
      val radioManual: VdomElement = MuiFormControlLabel(
        new MuiFormControlLabelProps {
          override val control = MuiRadio().rawElement
          override val value = ITfDailyMode.ModeId.Manual
          override val label = crCtxP.message( MsgCodes.`Set.manually` ).rawNode
        }
      )

      val chs = List[VdomElement](

        // Заголовок окна:
        platformComponents.diaTitle( Nil )(
          crCtxP.message( MsgCodes.`Adv.tariff` ),
        ),

        // Содержимое диалога:
        MuiDialogContent()(

          // Основное управление тарифом:
          s.tfModeIdC { tfModeIdProxy =>
            val tfModeId = tfModeIdProxy.value
            React.Fragment(

              // Радио-кнопки:
              MuiRadioGroup(
                new MuiRadioGroupProps {
                  override val value = tfModeId
                  override val onChange = _onModeChange
                }
              )(
                radioInherit,
                radioManual,
              ),

              <.div(
                if (tfModeId ==* ITfDailyMode.ModeId.Manual)
                  ^.visibility.visible
                else
                  ^.visibility.hidden,

                inputRow,
              )
            )
          },

          // pending progress-bar:
          s.isPendingSomeC { isPendingSomeProxy =>
            val isPending = isPendingSomeProxy.value.value
            <.div(
              if (isPending) ^.visibility.visible
              else ^.visibility.hidden,
              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val value = JsOptionUtil.maybeDefined( !isPending )(0)
                  override val variant = if (isPending) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
                }
              ),
            )
          },

          // Рендер возможной ошибки:
          s.exceptionOptC { exceptionOptProxy =>
            exceptionOptProxy.value.whenDefinedEl { ex =>
              MuiTypoGraphy(
                new MuiTypoGraphyProps {
                  override val color = MuiTypoGraphyColors.error
                }
              )(
                ex.getMessage,
              )
            }
          },

        ),

        // Кнопки внизу диалога:
        MuiDialogActions(
          platformComponents.diaActionsProps()(platCss)
        )(
          // Кнопка "Сохранить"
          {
            val saveMsg = crCtxP.message( MsgCodes.`Save` )
            s.saveBtnDisabledSomeC { saveBtnDisabledSomeProxy =>
              val isDisabled = saveBtnDisabledSomeProxy.value.value
              MuiButton(
                new MuiButtonProps {
                  override val onClick = _onSaveClickCbF
                  override val disabled = isDisabled
                  override val size = MuiButtonSizes.large
                }
              )( saveMsg )
            }
          },

          // Кнопка "Закрыть"
          MuiButton(
            new MuiButtonProps {
              override val onClick = _onCloseClickCbF
              override val size = MuiButtonSizes.large
            }
          )(
            crCtxP.message( MsgCodes.`Close` ),
          ),
        ),

      )

      // Рендер диалога:
      val diaCss = new MuiDialogClasses {
        override val paper = platCss.Dialogs.paper.htmlClass
      }
      s.diaOpenedSomeC { diaOpenedSomeProxy =>
        val isOpened = diaOpenedSomeProxy.value.value
        MuiDialog(
          new MuiDialogProps {
            override val open = isOpened
            override val maxWidth = MuiDialogMaxWidths.sm
            override val onClose = _onCloseClickCbF
            override val classes = diaCss
          }
        )( chs: _* )
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        diaOpenedSomeC = propsProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.nonEmpty )
        },

        saveBtnDisabledSomeC = propsProxy.connect { propsOpt =>
          val isDisabled = propsOpt.exists { p =>
            p.request.isPending || !p.isValid
          }
          OptionUtil.SomeBool( isDisabled )
        },

        tfModeIdC = propsProxy.connect { propsOpt =>
          propsOpt.fold( ITfDailyMode.ModeId.Inherit )( _.mode.modeId )
        },

        tfManualAmountC = propsProxy.connect { propsOpt =>
          propsOpt.flatMap(_.inputAmount)
        },

        tfManualCurrencyC = propsProxy.connect { propsOpt =>
          propsOpt
            .flatMap(_.nodeTfOpt)
            .fold(MCurrencies.default)(_.currency)
        },

        isPendingSomeC = propsProxy.connect { propsOpt =>
          val isPending = propsOpt.fold(false)(_.request.isPending)
          OptionUtil.SomeBool( isPending )
        },

        exceptionOptC = propsProxy.connect { propsOpt =>
          propsOpt.flatMap(_.request.exceptionOption)
        }( OptFastEq.Wrapped(FastEq.AnyRefEq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
