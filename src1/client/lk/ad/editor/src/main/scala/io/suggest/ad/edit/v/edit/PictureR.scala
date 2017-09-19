package io.suggest.ad.edit.v.edit

import diode.react.ModelProxy
import io.suggest.ad.edit.m.PictureFileChanged
import io.suggest.common.html.HtmlConstants
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 17:14
  * Description: Элемент формы для управления картинкой-изображением.
  * Изначально -- только из файла.
  */
class PictureR {

  case class PropsVal(

                     )

  type Props = ModelProxy[Option[PropsVal]]

  protected case class State()


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на выбор файла в file input. */
    private def fileChanged(e: ReactEventFromInput): Callback = {
      val files = DomListSeq( e.target.files )
      dispatchOnProxyScopeCB($, PictureFileChanged(files))
    }


    def render(p: Props, s: State): VdomElement = {
      <.div(

        // Можно загрузить файл.
        <.input(
          ^.`type`    := HtmlConstants.Input.file,
          ^.onChange ==> fileChanged
        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Pic")
    .initialStateFromProps { p =>
      State()
    }
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
