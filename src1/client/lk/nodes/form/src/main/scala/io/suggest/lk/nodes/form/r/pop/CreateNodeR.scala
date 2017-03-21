package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.common.html.HtmlConstants
import io.suggest.common.radio.BleConstants.Beacon.EddyStone
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.pop.PopupR
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:31
  * Description: Компонент попапа с формой создания узла.
  */
object CreateNodeR {

  type Props = ModelProxy[Option[MCreateNodeS]]

  class Backend($: BackendScope[Props, Unit]) {

    /** Callback для ввода названия добавляемого под-узла. */
    private def onNameChange(e: ReactEventI): Callback = {
      dispatchOnProxyScopeCB(
        $, CreateNodeNameChange(name = e.target.value)
      )
    }

    /** Callback редактирования id создаваемого узла. */
    private def onIdChange(e: ReactEventI): Callback = {
      dispatchOnProxyScopeCB(
        $, CreateNodeIdChange(id = e.target.value)
      )
    }

    /** Callback нажатия на кнопку "сохранить" при добавлении нового узла. */
    private def onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, CreateNodeSaveClick )
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, CreateNodeCancelClick )
    }


    def render(propsProxy: Props): ReactElement = {
      for (addState <- propsProxy()) yield {

        val isSaving = addState.saving.isPending
        val disabledAttr = isSaving ?= {
          ^.disabled := true
        }

        propsProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(onCancelClick)
          )
        } { popPropsProxy =>
          PopupR( popPropsProxy ) {

            // Сейчас открыта форма добавление под-узла для текущего узла.
            <.div(

              isSaving ?= {
                ^.title := Messages( MsgCodes.`Server.request.in.progress.wait` )
              },

              <.h2(
                ^.`class` := Css.Lk.MINOR_TITLE,
                Messages( MsgCodes.`New.node` )
              ),


              <.div(
                ^.`class` := Css.Text.CENTERED,

                // Поле ввода названия маячка.
                <.div(
                  ^.`class` := Css.flat( Css.Input.INPUT, Css.Lk.Nodes.Inputs.INPUT90 ),

                  <.label(
                    Messages( MsgCodes.`Name` ), ":",
                    <.input(
                      ^.`type`      := "text",
                      ^.value       := addState.name,
                      ^.onChange   ==> onNameChange,
                      ^.placeholder := Messages( MsgCodes.`Beacon.name.example` ),
                      disabledAttr
                    )
                  )
                ),

                <.br,

                // Поля для ввода id маячка.
                <.div(
                  ^.`class` := Css.flat( Css.Input.INPUT, Css.Lk.Nodes.Inputs.INPUT90 ),
                  <.label(
                    Messages( MsgCodes.`Identifier` ),
                    " (EddyStone-UID)",
                    <.input(
                      ^.`type`      := "text",
                      ^.value       := addState.id.getOrElse(""),
                      ^.onChange   ==> onIdChange,
                      ^.placeholder := EddyStone.EXAMPLE_UID,
                      !isSaving ?= {
                        ^.title := Messages( MsgCodes.`Example.id.0`, EddyStone.EXAMPLE_UID )
                      },
                      disabledAttr
                    )
                  )
                )
              ),


              // Кнопки сохранения/отмены.
              <.div(
                ^.`class` := Css.flat( Css.Buttons.BTN_W, Css.Size.M ),

                // Кнопка сохранения. Активна только когда юзером введено достаточно данных.
                (addState.saving.isEmpty && !isSaving) ?= {
                  val isSaveBtnEnabled = addState.isValid
                  <.span(
                    <.a(
                      ^.classSet1(
                        Css.flat(Css.Buttons.BTN, Css.Size.M),
                        Css.Buttons.MAJOR     -> isSaveBtnEnabled,
                        Css.Buttons.DISABLED  -> !isSaveBtnEnabled
                      ),
                      isSaveBtnEnabled ?= {
                        ^.onClick --> onSaveClick
                      },
                      Messages( MsgCodes.`Save` )
                    ),
                    HtmlConstants.SPACE,

                    // Кнопка отмены.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Size.M, Css.Buttons.NEGATIVE, Css.Buttons.LIST),
                      ^.onClick --> onCancelClick,
                      Messages( MsgCodes.`Cancel` )
                    )
                  )
                },

                // Крутилка ожидания сохранения.
                isSaving ?= LkPreLoaderR.AnimMedium,

                // Вывести инфу, что что-то пошло не так при ошибке сохранения.
                addState.saving.renderFailed {
                  // Исключение в норме заворачивается в ILknException на уровне TreeAh.
                  case ex: ILknException =>
                    <.span(
                      ^.`class` := Css.Colors.RED,
                      for (title <- ex.titleOpt) yield {
                        ^.title := title
                      },
                      Messages( ex.msgCode )
                    )
                  // should never happen
                  case ex =>
                    <.span(
                      Messages("Error"), ": ",
                      ex.toString()
                    )
                }

              )

            )
          }
        }
      }
    }

  }

  val component = ReactComponentB[Props]("CreateNodePop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}