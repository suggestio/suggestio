package io.suggest.lk.nodes.form.r

import com.materialui.{MuiListItemSecondaryActionClasses, MuiListItemSecondaryActionProps}
import io.suggest.css.ScalaCssDefaults._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.08.2020 21:16
  * Description: Доп.стили для lk-nodes формы.
  */
case class LkNodesFormCss() extends StyleSheet.Inline {

  import dsl._


  object Node {

    val linearProgress = style(
      width( 150.px ),
      height( 1.px ),
    )

    /** MuiList SecondaryAction, содержащая progress внутри. */
    val secActProgress = style(
      display.flex,
      alignItems.center,
    )
    def sceActProgressProps: MuiListItemSecondaryActionProps = {
      val css = new MuiListItemSecondaryActionClasses {
        override val root = secActProgress.htmlClass
      }
      new MuiListItemSecondaryActionProps {
        override val classes = css
      }
    }

    val toolBar = style(
      justifyContent.spaceAround
    )

  }


  initInnerObjects(
    Node.linearProgress,
  )

}


case object LkNodesFormCssStd extends StyleSheet.Standalone {

  import dsl._

  // Чтобы switch и progress-bar не пересекались, увеличиваем правый отступ для контейнера в MuiListItem:
  ".MuiListItem-secondaryAction" - (
     paddingRight( 72.px )
  )

}
