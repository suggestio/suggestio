package io.suggest.lk.r.img

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{MFormResourceKey, PictureFileChanged}
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.18 10:36
  * Description: Кнопка редактора какой-то картинки: картинку можно загрузить, убрать, кропнуть и т.д.
  */
class ImgEditBtnR {

  case class PropsVal(
                       src        : Option[String],
                       resKey     : MFormResourceKey
                     )
  implicit object ImgEditBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.src ===* b.src) &&
        (a.resKey ==* b.resKey)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на выбор файла в file input. */
    private def _onFileChange(e: ReactEventFromInput): Callback = {
      // Тут передаётся hosted-object внутри сообщения, но вроде бы это безопасно и не глючит нигде (files)
      _picFileChanged( DomListSeq(e.target.files) )
    }

    /** Клик по кнопке удаления файла. */
    private def _onDeleteFileClick: Callback = {
      _picFileChanged( Nil )
    }

    private def _picFileChanged(files: Seq[dom.File]): Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        PictureFileChanged(files, props.value.resKey)
      }
    }


    def render(propsValProxy: Props): VdomElement = {
      val C = Css.Lk.AdEdit.Image
      val propsVal = propsValProxy.value

      <.div(
        ^.`class` := Css.flat( C.IMAGE, Css.Size.M ),

        propsVal.src.fold[VdomElement] {
          val upCss = Css.Lk.Uploads
          // Картинки нет. Можно загрузить файл.
          <.div(
            <.input(
              ^.`type`    := HtmlConstants.Input.file,
              ^.`class`   := upCss.ADD_FILE_INPUT,
              ^.onChange ==> _onFileChange
            ),

            <.a(
              ^.`class` := upCss.IMAGE_ADD_BTN,
              <.span(
                Messages( MsgCodes.`Upload.file`, HtmlConstants.SPACE )
              )
            )
          )

        } { blobUrl =>
          <.div(
            // thumbnail картинки.
            <.img(
              ^.src := blobUrl,
              ^.width := 140.px
              //^.height := 85.px   // Чтобы не плющило картинки, пусть лучше обрезает снизу
            ),

            // Ссылка-кнопка удаления картинки.
            <.a(
              ^.`class` := C.IMAGE_REMOVE_BTN,
              ^.title   := Messages( MsgCodes.`Delete` ),
              ^.onClick --> _onDeleteFileClick,
              <.span
            )
          )
        }
      )
    }

  }


  val component = ScalaComponent.builder[Props]("ImgEditBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def _apply(propsValProxy: Props) = component( propsValProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}