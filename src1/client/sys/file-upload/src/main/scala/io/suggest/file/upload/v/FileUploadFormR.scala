package io.suggest.file.upload.v

import com.materialui.{MuiInput, MuiInputProps, MuiPaper}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.file.upload.m.MFupRoot
import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 10:43
  * Description: Компонент формы заливки файла.
  */
class FileUploadFormR {

  type Props_t = MFupRoot
  type Props = ModelProxy[Props_t]

  case class State(
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      MuiPaper()(

        MuiInput(
          new MuiInputProps {
            override val `type` = HtmlConstants.Input.file

          }
        )

      )
    }

  }

}
