package io.suggest.sc.v.dia.settings

import com.materialui.{MuiListItem, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiMenuItem, MuiMenuItemProps, MuiSelectProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MLanguages, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.LangSwitch
import io.suggest.sc.m.in.MScReactCtx
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.spa.FastEqUtil
import japgolly.univeq._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

class LangSettingR(
                    crCtxProv               : React.Context[MCommonReactCtx],
                  ) {

  type Props_t = MScReactCtx
  type Props = ModelProxy[Props_t]

  case class State(
                    ctxC            : ReactConnectProxy[Props_t],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val systemDefaultId = "_"

    private val _onSelectChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val value = e.target.value
      val langOpt = OptionUtil.maybeOpt(
        value.nonEmpty &&
        (value !=* systemDefaultId)
      ) {
        MLanguages.withValueOpt( value )
      }
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, LangSwitch(langOpt) )
    }

    def render(s: State): VdomElement = {
      MuiListItem()(

        MuiListItemText(
          new MuiListItemTextProps {
            override val primary = crCtxProv.message( MsgCodes.`Language` ).rawNode
          }
        )(),

        MuiListItemSecondaryAction()(
          {
            val selectLanguages = (for {
              lang <- MLanguages.values.iterator
            } yield {
              MuiMenuItem.component.withKey( lang.value )(
                new MuiMenuItemProps {
                  override val value = lang.value
                }
              )(
                {
                  val content = TagMod(
                    // Country flag using unicode Emoji table:
                    <.span(
                      lang.countryFlagEmoji,
                    ),
                    HtmlConstants.SPACE,
                    lang.singularNative,
                  )
                  crCtxProv.consume { crCtx =>
                    <.span(
                      ^.title := crCtx.messages( lang.singularMsgCode ),
                      content,
                    )
                  }
                }

              ): VdomNode
            })
              .toList
            val systemDefaultLang = MuiMenuItem.component.withKey( systemDefaultId )(
              new MuiMenuItemProps {
                override val value = systemDefaultId
              }
            )(
              crCtxProv.message( MsgCodes.`System._adjective` ),
            ): VdomNode

            val allSelectItems =
              systemDefaultLang ::
              // TODO Add - delimiter here
              selectLanguages

            val _selectProps = new MuiSelectProps {
              override val variant = MuiTextField.Variants.standard
              override val native = false
            }

            s.ctxC { ctxProxy =>
              val ctx = ctxProxy.value
              val _value = ctx.language.fold( systemDefaultId )(_.value)
              MuiTextField(
                new MuiTextFieldProps {
                  override val value = _value
                  override val select = true
                  override val SelectProps = _selectProps
                  override val onChange = _onSelectChange
                  override val disabled = ctx.langSwitch.isPending
                }
              )( allSelectItems: _* )
            }
          },
        ),

      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        ctxC = propsProxy.connect( identity ) {
          FastEqUtil[Props_t] { (a, b) =>
            (a.language ===* b.language) &&
            (a.langSwitch ===* b.langSwitch)
          }
        },
      )
    }
    .renderBackend[Backend]
    .build

}
