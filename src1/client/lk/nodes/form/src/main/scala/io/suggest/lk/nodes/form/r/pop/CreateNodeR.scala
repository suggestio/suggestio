package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.common.html.HtmlConstants
import io.suggest.ble.BleConstants.Beacon.EddyStone
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.lk.r.popup.PopupR
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:31
  * Description: Компонент попапа с формой создания узла.
  */
class CreateNodeR(
                   crCtxP: React.Context[MCommonReactCtx],
                 ) {

  type Props = ModelProxy[Option[MCreateNodeS]]

  class Backend($: BackendScope[Props, Unit]) {

    /** Callback для ввода названия добавляемого под-узла. */
    private def onNameChange(e: ReactEventFromInput): Callback = {
      val name = e.target.value
      dispatchOnProxyScopeCB(
        $, CreateNodeNameChange(name = name)
      )
    }

    /** Callback редактирования id создаваемого узла. */
    private def onIdChange(e: ReactEventFromInput): Callback = {
      val id = e.target.value
      dispatchOnProxyScopeCB(
        $, CreateNodeIdChange(id = id)
      )
    }

    /** Callback нажатия на кнопку "сохранить" при добавлении нового узла. */
    private val onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, CreateNodeSaveClick )
    }

    private val onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, CreateNodeCancelClick )
    }


    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { addState =>

        val isSaving = addState.saving.isPending

        val disabledAttr = {
          ^.disabled := true
        }.when( isSaving )

        propsProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(onCancelClick)
          )
        } { popPropsProxy =>
          PopupR( popPropsProxy ) {

            // Сейчас открыта форма добавление под-узла для текущего узла.
            crCtxP.consume { crCtx =>
              <.div(

                ReactCommonUtil.maybe(isSaving) {
                  ^.title := crCtx.messages( MsgCodes.`Server.request.in.progress.wait` )
                },

                <.h2(
                  ^.`class` := Css.Lk.MINOR_TITLE,
                  crCtx.messages( MsgCodes.`New.node` ),
                ),


                <.div(
                  ^.`class` := Css.Text.CENTERED,

                  // Поле ввода названия маячка.
                  <.div(
                    ^.`class` := Css.flat( Css.Input.INPUT, Css.Lk.Nodes.Inputs.INPUT90 ),

                    <.label(
                      crCtx.messages( MsgCodes.`Name` ), ":",
                      <.input(
                        ^.`type`      := HtmlConstants.Input.text,
                        ^.value       := addState.name,
                        ^.onChange   ==> onNameChange,
                        ^.placeholder := crCtx.messages( MsgCodes.`Beacon.name.example` ),
                        disabledAttr,
                      )
                    )
                  ),

                  <.br,

                  // Поля для ввода id маячка.
                  <.div(
                    ^.`class` := Css.flat( Css.Input.INPUT, Css.Lk.Nodes.Inputs.INPUT90 ),
                    <.label(
                      crCtx.messages( MsgCodes.`Identifier` ),
                      " (EddyStone-UID)",
                      <.input(
                        ^.`type`      := HtmlConstants.Input.text,
                        ^.value       := addState.id.getOrElse(""),
                        ^.onChange   ==> onIdChange,
                        ^.placeholder := EddyStone.EXAMPLE_UID,

                        ReactCommonUtil.maybe(!isSaving) {
                          ^.title := crCtx.messages( MsgCodes.`Example.id.0`, EddyStone.EXAMPLE_UID )
                        },

                        disabledAttr,
                      )
                    )
                  )
                ),


                // Кнопки сохранения/отмены.
                <.div(
                  ^.`class` := Css.flat( Css.Buttons.BTN_W, Css.Size.M ),

                  // Кнопка сохранения. Активна только когда юзером введено достаточно данных.
                  ReactCommonUtil.maybeEl( addState.saving.isEmpty && !isSaving ) {
                    val isSaveBtnEnabled = addState.isValid
                    <.span(
                      <.a(
                        ^.classSet1(
                          Css.flat(Css.Buttons.BTN, Css.Size.M),
                          Css.Buttons.MAJOR     -> isSaveBtnEnabled,
                          Css.Buttons.DISABLED  -> !isSaveBtnEnabled
                        ),

                        ReactCommonUtil.maybe( isSaveBtnEnabled ) {
                          ^.onClick --> onSaveClick
                        },

                        crCtx.messages( MsgCodes.`Save` ),
                      ),
                      HtmlConstants.SPACE,

                      // Кнопка отмены.
                      <.a(
                        ^.`class` := Css.flat(Css.Buttons.BTN, Css.Size.M, Css.Buttons.NEGATIVE, Css.Buttons.LIST),
                        ^.onClick --> onCancelClick,
                        crCtx.messages( MsgCodes.`Cancel` ),
                      )
                    )
                  },

                  // Крутилка ожидания сохранения.
                  ReactCommonUtil.maybeEl(isSaving)( LkPreLoaderR.AnimMedium ),

                  // Вывести инфу, что что-то пошло не так при ошибке сохранения.
                  addState.saving.renderFailed {
                    // Исключение в норме заворачивается в ILknException на уровне TreeAh.
                    case ex: ILknException =>
                      <.span(
                        ^.`class` := Css.Colors.RED,
                        ex.titleOpt.whenDefined { title =>
                          ^.title := title
                        },
                        crCtx.messages( ex.msgCode ),
                      )
                    // should never happen
                    case ex =>
                      <.span(
                        crCtx.messages( MsgCodes.`Error` ), ": ",
                        ex.toString(),
                      )
                  }

                )

              )
            }
          }
        }
      }
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
