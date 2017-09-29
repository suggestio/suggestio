package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.{CropOpen, PictureFileChanged}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 17:14
  * Description: Элемент формы для управления картинкой-изображением.
  * Изначально -- только из файла.
  */
class PictureR {

  case class PropsVal(
                       imgSrcOpt: Option[String]
                     )
  implicit object PictureRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.imgSrcOpt ===* b.imgSrcOpt
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на выбор файла в file input. */
    private def _onFileChange(e: ReactEventFromInput): Callback = {
      val files = DomListSeq( e.target.files )
      dispatchOnProxyScopeCB($, PictureFileChanged(files))
    }

    /** Клик по кнопке удаления файла. */
    private def _onDeleteFileClick: Callback = {
      dispatchOnProxyScopeCB($, PictureFileChanged(Nil))
    }

    /** Клик по кнопке кадрирования картинки. */
    private def _onCropClick: Callback = {
      dispatchOnProxyScopeCB($, CropOpen)
    }


    def render(p: Props): VdomElement = {
      val C = Css.Lk.AdEdit.Image
      // Отрендерить текущую картинку
      p.value.whenDefinedEl { props =>
        val blobUrlOpt = props.imgSrcOpt
        <.div(
          <.div(
            ^.`class` := Css.flat( C.IMAGE, Css.Size.M ),

            blobUrlOpt.fold[VdomElement] {
              // Картинки нет. Можно загрузить файл.
              <.input(
                ^.`type`    := HtmlConstants.Input.file,
                ^.onChange ==> _onFileChange
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
          ),

          // Кнопка для кропа изображения.
          blobUrlOpt.whenDefined { _ =>
            val B = Css.Buttons
            <.a(
              ^.`class` := Css.flat( B.BTN, B.BTN_W, B.MINOR, Css.Size.M ),
              ^.onClick --> _onCropClick,
              Messages( MsgCodes.`Crop` ), HtmlConstants.ELLIPSIS
            )
          }

        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("Pic")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}