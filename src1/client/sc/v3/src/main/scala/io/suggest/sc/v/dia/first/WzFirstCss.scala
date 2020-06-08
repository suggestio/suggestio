package io.suggest.sc.v.dia.first

import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.dev.MTlbr
import japgolly.univeq.UnivEq
import scalacss.internal.DslBase.ToStyle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.2020 16:57
  * Description: CSS для диалогов, т.к. fullscreen-диалоги рендерятся прямо в body,
  * и общие ScCss-стили на них набросить крайне тяжело.
  */
object WzFirstCss {
  @inline implicit def univEq: UnivEq[WzFirstCss] = UnivEq.derive
}

final case class WzFirstCss( unsafeOffsets: MTlbr ) extends StyleSheet.Inline {

  import dsl._


  val header = {
    var acc = List.empty[ToStyle]

    // padding трогать нельзя: material-ui использует.
    for (topPx <- unsafeOffsets.topO)
      acc ::= marginTop( topPx.px )

    style( acc: _* )
  }


  val footer = {
    var acc = List.empty[ToStyle]

    // padding трогать нельзя: material-ui использует.
    for (bottomPx <- unsafeOffsets.bottomO)
      acc ::= marginBottom( bottomPx.px )

    style( acc: _* )
  }

}
