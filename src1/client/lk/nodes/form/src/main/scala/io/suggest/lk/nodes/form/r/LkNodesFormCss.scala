package io.suggest.lk.nodes.form.r

import io.suggest.css.ScalaCssDefaults._

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
      flexGrow( 3 ),
    )

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
