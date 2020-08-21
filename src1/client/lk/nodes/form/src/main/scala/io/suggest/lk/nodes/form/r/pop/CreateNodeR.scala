package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiLinearProgress, MuiLinearProgressProps, MuiList, MuiListItem, MuiListItemText, MuiProgressVariants, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ble.BleConstants.Beacon.EddyStone
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.OptFastEq
import monocle.Lens

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:31
  * Description: Компонент попапа с формой создания узла.
  */
class CreateNodeR(
                   platformCssStatic      : () => PlatformCssStatic,
                   platformComponents     : PlatformComponents,
                   crCtxP                 : React.Context[MCommonReactCtx],
                 ) {

  type Props = ModelProxy[Option[MCreateNodeS]]

  case class State(
                    openedSomeC                 : ReactConnectProxy[Some[Boolean]],
                    nameOptC                    : ReactConnectProxy[Option[MTextFieldS]],
                    idOptC                      : ReactConnectProxy[Option[MTextFieldS]],
                    saveBtnDisabledSomeC        : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC              : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC               : ReactConnectProxy[Option[Throwable]],
                  )

  private def __mkTextField(
                             labelI18n: String,
                             placeHolderI18n: String,
                             conn: ReactConnectProxy[Option[MTextFieldS]],
                             onChanged: js.Function1[ReactEventFromInput, Unit],
                             helpText: js.UndefOr[raw.React.Node] = js.undefined,
                           ): VdomElement = {
    MuiListItem()(
      conn { mtfOptProxy =>
        val mtfOpt = mtfOptProxy.value
        val _value = mtfOpt.fold("")(_.value)
        val _isValid = mtfOpt.exists(_.isValid)
        MuiTextField(
          new MuiTextFieldProps {
            override val fullWidth = true
            override val label = labelI18n
            override val value = _value
            override val error = !_isValid
            override val placeholder = placeHolderI18n
            override val onChange = onChanged
            override val helperText = helpText
            override val required = true
          }
        )()
      }
    )
  }

  class Backend($: BackendScope[Props, State]) {

    /** Callback для ввода названия добавляемого под-узла. */
    private val _onNameChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val name = e.target.value
      dispatchOnProxyScopeCB( $, CreateNodeNameChange(name = name) )
    }

    /** Callback редактирования id создаваемого узла. */
    private val _onIdChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val id = e.target.value
      dispatchOnProxyScopeCB( $, CreateNodeIdChange(id = id) )
    }

    /** Реакция на кнопку "Сохранить". */
    private val onSaveClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, CreateNodeSaveClick )
    }

    /** Реакция на отмену или сокрытие диалога. */
    private val onCloseClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, CreateNodeCloseClick )
    }


    def render(s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        val platCss = platformCssStatic()

        val diaChs = List[VdomElement](
          // Заголовок окна:
          platformComponents.diaTitle( Nil )(
            crCtx.messages( MsgCodes.`New.node` ),
          ),

          // Содержимое диалога:
          MuiDialogContent()(
            MuiList()(
              // Название узла (маячка).
              __mkTextField(
                crCtx.messages( MsgCodes.`Name` ),
                crCtx.messages( MsgCodes.`Beacon.name.example` ),
                s.nameOptC,
                _onNameChanged,
              ),

              // id узла/маячка.
              __mkTextField(
                crCtx.messages( MsgCodes.`Identifier` ),
                EddyStone.EXAMPLE_UID,
                s.idOptC,
                _onIdChanged,
                helpText = "EddyStone-UID",
              ),

              // pending progress bar
              s.isPendingSomeC { isPendingSomeProxy =>
                val isPending = isPendingSomeProxy.value.value
                <.span(
                  if (isPending) ^.visibility.visible else ^.visibility.hidden,
                  MuiLinearProgress(
                    new MuiLinearProgressProps {
                      override val variant = if (isPending) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
                      override val value = JsOptionUtil.maybeDefined( !isPending )(0)
                    }
                  )
                )
              },

              s.exceptionOptC { exceptionOptProxy =>
                exceptionOptProxy.value.whenDefinedEl { ex =>
                  MuiListItem()(
                    MuiListItemText()(
                      MuiTypoGraphy(
                        new MuiTypoGraphyProps {
                          override val color = MuiColorTypes.error
                        }
                      )(
                        ex match {
                          case ex: LknException =>
                            <.span(
                              crCtx.messages( ex.msgCode ),
                              ex.titleOpt.whenDefined( crCtx.messages(_) ),

                              HtmlConstants.SPACE, HtmlConstants.`(`,
                              ex.getCause.getClass.getSimpleName,
                              HtmlConstants.`)`,
                            )
                          case other => other.getMessage
                        }
                      )
                    )
                  )
                }
              },
            ),

          ),

          // Кнопки внизу окна:
          MuiDialogActions(
            platformComponents.diaActionsProps()(platCss)
          )(
            // Кнопка "Сохранить"
            {
              val saveMsg = crCtx.messages( MsgCodes.`Save` )
              s.saveBtnDisabledSomeC { saveBtnDisabledSomeProxy =>
                MuiButton(
                  new MuiButtonProps {
                    override val onClick = onSaveClickCbF
                    override val disabled = saveBtnDisabledSomeProxy.value.value
                  }
                )(
                  saveMsg,
                )
              }
            },

            // Кнопка "Закрыть"
            MuiButton(
              new MuiButtonProps {
                override val onClick = onCloseClickCbF
              }
            )(
              crCtx.messages( MsgCodes.`Close` ),
            ),
          ),

        )

        // Наконец, рендер окна самого диалога:
        val diaCss = new MuiDialogClasses {
          override val paper = platCss.Dialogs.paper.htmlClass
        }
        s.openedSomeC { openedSomeProxy =>
          MuiDialog(
            new MuiDialogProps {
              override val open = openedSomeProxy.value.value
              override val onClose = onCloseClickCbF
              override val maxWidth = MuiDialogMaxWidths.sm
              override val fullWidth = true
              override val classes = diaCss
            }
          )( diaChs: _* )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      // Сборка коннекшенов до полей:
      val mtfFeq = OptFastEq.Wrapped(FastEq.AnyRefEq)
      def __mkMtfConn(lens: Lens[MCreateNodeS, MTextFieldS]) =
        propsProxy.connect(_.map(lens.get))( mtfFeq )

      // Сборка состояния:
      State(

        openedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.isDefined )
        },

        nameOptC = __mkMtfConn( MCreateNodeS.name ),

        idOptC = __mkMtfConn( MCreateNodeS.id ),

        saveBtnDisabledSomeC = propsProxy.connect { propsOpt =>
          val saveDisabled = propsOpt.fold(true) { props =>
            !props.isValid ||
            props.saving.isPending
          }
          OptionUtil.SomeBool( saveDisabled )
        },

        isPendingSomeC = propsProxy.connect { propsOpt =>
          val isPending = propsOpt.exists(_.saving.isPending)
          OptionUtil.SomeBool( isPending )
        },

        exceptionOptC = propsProxy.connect( _.flatMap(_.saving.exceptionOption) )( OptFastEq.Wrapped(FastEq.AnyRefEq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
