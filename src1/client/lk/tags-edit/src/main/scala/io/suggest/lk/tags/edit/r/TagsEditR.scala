package io.suggest.lk.tags.edit.r

import com.materialui.MuiAutoComplete.{OnChangeDetails, OnChangeReason, OnInputChangeReason}
import com.materialui.{MuiAutoComplete, MuiAvatar, MuiChip, MuiChipProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.common.tags.search.MTagFound
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.tags.edit.m.{AddCurrentTag, AddTagFound, MTagsEditState, RmTag, SetTagSearchQuery, StartSearchReq}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.ext.KeyCode
import japgolly.univeq._

import scala.scalajs.js
import js.JSConverters._
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 22:12
  * Description: Реализация редактора тегов на базе react + diode.
  *
  * Тут всё спроектировано под M.wrap(TagsEditR(_)), без connect.
  * Внутренние же компоненты прилинкованы через .connect().
  */
final class TagsEditR(
                       crCtxP: React.Context[MCommonReactCtx],
                     ) {

  type Props = ModelProxy[MTagsEditState]


  /** Состояние компонента содержит только immutable-инстансы коннекшенов до суб-моделей.  */
  case class State(
                    tagsExistConn       : ReactConnectProxy[Set[String]],
                    tagsFoundEditStateC : ReactConnectProxy[MTagsEditState],
                  )


  /** React-компонент редактора тегов. */
  class Backend($: BackendScope[Props, State]) {

    private def _onTagDeleteCb(tagName: String) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactUIEventFromHtml =>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, RmTag(tagName))
      }
    }


    /**
      * Коллбэк ввода текста в поле имени нового тега.
      * Надо обновить состояние и запустить поисковый запрос, если требуется.
      */
    private val _onTextChangeCb = ReactCommonUtil.cbFun3ToJsCb { (e: ReactEventFromInput, queryStr2: String, reason: OnInputChangeReason) =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetTagSearchQuery(queryStr2) )
    }


    /** Коллбек для реакции на нажатие некоторых особых клавиш на клавиатуре во время ввода. */
    private val _onKeyUpCb = ReactCommonUtil.cbFun1ToJsCb { e: ReactKeyboardEvent =>
      if (e.keyCode ==* KeyCode.Enter) {
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, AddCurrentTag )
      } else {
        Callback.empty
      }
    }


    private val _getOptionLabelJs = { (mtf: MTagFound) =>
      mtf.face
    }: js.Function1[MTagFound, String]


    private val _onOptionChangeJs = ReactCommonUtil.cbFun4ToJsCb {
      (e: ReactEvent,
       option: MTagFound | js.Array[MTagFound] | Null,
       reason: OnChangeReason,
       details: js.UndefOr[OnChangeDetails[MTagFound]]) =>
        // Требуется реакция на select-option. И, возможно, на clear.
        if (reason == MuiAutoComplete.OnChangeReason.SELECT_OPTION) {
          ReactDiodeUtil.dispatchOnProxyScopeCB( $, AddTagFound( option.asInstanceOf[MTagFound].face ) )
        } else {
          Callback.empty
        }
    }


    /** Если юзер нажимает стрелочку, принудительно раскрывая подсказки - тут надо запустить поиск. */
    private val _onOptionsOpenJs = ReactCommonUtil.cbFun1ToJsCb { (e: ReactEvent) =>
      $.props >>= { p: Props =>
        val v = p.value
        if (v.renderOptions.isEmpty) {
          ReactDiodeUtil.dispatchOnProxyScopeCB($, StartSearchReq())
        } else {
          Callback.empty
        }
      }
    }


    /** Выполнить рендер редактора тегов. */
    def render(s: State): VdomElement = {
      val addTagsMsg = crCtxP.message( MsgCodes.`Add.tags` )
      val _tagAva = MuiAvatar()( HtmlConstants.DIEZ )
      val mtfEmpty = MTagFound("", 0)

      <.div(
        // Локализованный заголовок виджета
        <.h2(
          ^.`class` := Css.Lk.MINOR_TITLE,
          crCtxP.message( MsgCodes.`Tags.choosing` ),
        ),

        // tagExistsCont: Уже добавленные к заказу гео-теги.
        s.tagsExistConn { tagsExistsProxy =>
          <.div(
            tagsExistsProxy()
              .toVdomArray { tagName =>
                React.Fragment.withKey( tagName )(
                  MuiChip(
                    new MuiChipProps {
                      // TODO variant=outlined для уже установленных тегов на текущий момент.
                      override val variant    = MuiChip.Variant.DEFAULT
                      override val onDelete   = _onTagDeleteCb( tagName )
                      override val label      = tagName
                      // Визуально разгружаем через отказ от #-аватара.
                      //override val avatar     = _tagAva.rawElement
                    }
                  ),
                  HtmlConstants.SPACE,
                )
              }
          )
        },

        // поле ввода имени тега.
        {
          val _renderInputJsF: js.Function1[MuiTextFieldProps, raw.React.Node] = {
            (textFieldProps: MuiTextFieldProps) =>
              MuiTextField(
                new MuiTextFieldProps {
                  override val id               = textFieldProps.id
                  override val disabled         = textFieldProps.disabled
                  override val fullWidth        = textFieldProps.fullWidth
                  override val InputLabelProps  = textFieldProps.InputLabelProps
                  override val InputProps       = textFieldProps.InputProps
                  override val inputProps       = textFieldProps.inputProps
                  override val label            = addTagsMsg.rawElement
                  override val onKeyUp          = _onKeyUpCb
                }
              )()
                .rawElement
          }

          val _getOptionSelectedJsF = { (a: MTagFound, b: MTagFound) =>
            (a.face ==* b.face)
          }: js.Function2[MTagFound, MTagFound, Boolean]

          val _renderOneOptionJsF = { (mtf: MTagFound, _: js.Object) =>
            MuiChip.component(
              new MuiChipProps {
                override val variant    = MuiChip.Variant.OUTLINED
                override val label      = mtf.face
                override val avatar     = _tagAva.rawElement
              }
            )
              .rawNode
          }: js.Function2[MTagFound, js.Object, raw.React.Node]

          crCtxP.consume { crCtx =>
            val clearMsgStr = crCtx.messages( MsgCodes.`Clear` )
            s.tagsFoundEditStateC { tagsEditStateProxy =>
              val tagsEditState = tagsEditStateProxy.value
              var tagsAcc = tagsEditState.renderOptions

              MuiAutoComplete(
                new MuiAutoComplete.Props[MTagFound] {
                  override val open                 = tagsAcc.nonEmpty
                  override val options              = tagsAcc.toJSArray
                  override val getOptionLabel       = _getOptionLabelJs
                  override val value                = mtfEmpty
                  override val inputValue           = tagsEditState.props.query.text
                  override val onInputChange        = _onTextChangeCb
                  override val getOptionSelected    = _getOptionSelectedJsF
                  override val renderOption         = _renderOneOptionJsF
                  override val renderInput          = _renderInputJsF
                  @JSName("onChange")
                  override val onChange4            = _onOptionChangeJs
                  override val clearOnBlur          = true
                  override val onOpen               = _onOptionsOpenJs
                  override val freeSolo             = true
                  override val clearText            = clearMsgStr
                }
              )
            }
          }
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        tagsExistConn = p.connect(_.props.tagsExists),
        tagsFoundEditStateC = p.connect(identity)( FastEqUtil[MTagsEditState] { (a, b) =>
          (a.found ===* b.found) &&
          (a.props.query ===* b.props.query)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
