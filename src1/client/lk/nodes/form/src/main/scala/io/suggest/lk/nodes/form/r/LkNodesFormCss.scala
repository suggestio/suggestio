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

  }


  initInnerObjects(
    Node.linearProgress,
  )

}


case object LkNodesFormCssStd extends StyleSheet.Standalone {

  import dsl._

  // Сбросить состояние listStyle, которые зачем-то выставлено в common.css в точку для всех списов по дефолту.
  // Если поправить common.styl, то этот костыль можно удалить.
  "ul li" - (
    (listStyle := none),
  )

}
