package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.PictureFileChanged
import io.suggest.ad.edit.m.edit.MFileInfo
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.spa.OptFastEq
import io.suggest.react.ReactCommonUtil.Implicits._
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
                       img: MFileInfo
                     )
  implicit object PictureRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.img eq b.img)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]

  protected case class State(
                              blobUrlOptC     : ReactConnectProxy[Option[String]]
                            )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на выбор файла в file input. */
    private def fileChanged(e: ReactEventFromInput): Callback = {
      val files = DomListSeq( e.target.files )
      dispatchOnProxyScopeCB($, PictureFileChanged(files))
    }

    private def fileDeleted: Callback ={
      dispatchOnProxyScopeCB($, PictureFileChanged(Nil))
    }


    def render(p: Props, s: State): VdomElement = {
      val C = Css.Lk.AdEdit.Image
      <.div(
        ^.`class` := Css.flat( C.IMAGE, Css.Size.M ),

        // Отрендерить текущую картинку
        s.blobUrlOptC { blobUrlOpt =>
          blobUrlOpt.value.fold[VdomElement] {
            // Картинки нет. Можно загрузить файл.
            <.input(
              ^.`type`    := HtmlConstants.Input.file,
              ^.onChange ==> fileChanged
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
                ^.onClick --> fileDeleted,
                <.span
              )

            )
          }
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Pic")
    .initialStateFromProps { propsOptProxy =>
      State(
        blobUrlOptC = propsOptProxy.connect { propsOpt =>
          propsOpt.flatMap(_.img.blobUrl)
        }(OptFastEq.Plain)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
