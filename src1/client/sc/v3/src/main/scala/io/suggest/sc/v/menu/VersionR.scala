package io.suggest.sc.v.menu

import com.materialui.{MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import io.suggest.css.Css
import io.suggest.git.SioGitUtil
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react.{React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.2019 0:27
  * Description: Версия сборки из git.
  */
class VersionR(
                scCssP    : React.Context[ScCss],
              ) {

  // TODO builder.static-компонент почему-то не рендерится на iOS 13 Safari (в браузере).
  // Какой-то race condition с инициализацией js-компонента MuiTypography:
  // sjs-react выдаёт ошибку: Invalid JsComponent! ...

  val VERSION_STRING = {
    var vsn = SioGitUtil.currentRevision

    // Добавить "-dev" в конце, чтобы отличать отладочный билд.
    if (scalajs.LinkingInfo.developmentMode)
      vsn += "-dev"

    vsn
  }

  val component = ScalaComponent
    .builder
    .static( getClass.getSimpleName ) {
      scCssP.consume { scCss =>
        MuiTypoGraphy {
          val css = new MuiTypoGraphyClasses {
            override val root = Css.flat(
              ScCssStatic.Menu.version.htmlClass,
              scCss.Menu.version.htmlClass,
            )
          }
          new MuiTypoGraphyProps {
            override val variant = MuiTypoGraphyVariants.body1
            override val color = MuiTypoGraphyColors.primary
            override val classes = css
          }
        } (
          VERSION_STRING,
        )
      }
    }
    .build

}
