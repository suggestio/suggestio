package io.suggest.sc.v.menu

import com.materialui.{MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import io.suggest.git.SioGitUtil
import io.suggest.sc.styl.ScCssStatic
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.2019 0:27
  * Description: Версия сборки из git.
  */
class VersionR {

  // TODO builder.static-компонент почему-то не рендерится на iOS 13 Safari (в браузере).
  // Какой-то race condition с инициализацией js-компонента MuiTypography:
  // sjs-react выдаёт ошибку: Invalid JsComponent! ...

  val component = ScalaComponent
    .builder[Unit]( getClass.getSimpleName )
    .stateless
    .render_P { _ =>
      MuiTypoGraphy {
        val css = new MuiTypoGraphyClasses {
          override val root = ScCssStatic.Menu.version.htmlClass
        }
        new MuiTypoGraphyProps {
          override val variant = MuiTypoGraphyVariants.body1
          override val color = MuiTypoGraphyColors.primary
          override val classes = css
        }
      } (
        SioGitUtil.currentRevision,
      )
    }
    .build

}
