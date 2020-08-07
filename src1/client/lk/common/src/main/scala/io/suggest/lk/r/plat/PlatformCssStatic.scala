package io.suggest.lk.r.plat

import io.suggest.css.ScalaCssDefaults._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.07.2020 17:48
  * Description: Статические стили, зависящие от платформы.
  * Платформа не меняется, поэтому стили статические.
  * Многие вещи заточены на неизменяемость инстанса PlatformCssStatic.
  */
object PlatformCssStatic {

  @inline implicit def univEq: UnivEq[PlatformCssStatic] = UnivEq.derive

  def isRenderIos = GenLens[PlatformCssStatic]( _.isRenderIos )

}


final case class PlatformCssStatic(
                                    isRenderIos: Boolean,
                                  )
  extends StyleSheet.Inline
{

  import dsl._

  /** Компоненты. */
  object Dialogs {

    val title = {
      if (isRenderIos)
        style(
          fontWeight.bolder.important,
          justifyContent.center,
          display.flex,
        )
      else
        style()
    }


    val titleIcon = {
      if (isRenderIos) {
        style(
          position.absolute,
          top( 0.8.em ),
          right( 0.6.em ),
        )
      } else {
        style(
          float.right,
        )
      }
    }


    val paper = {
      if (isRenderIos) {
        style(
          borderRadius( 20.px ),
          opacity.attr := 90.%%.value,
        )
      } else {
        style()
      }
    }


    val text = {
      if (isRenderIos) {
        style(
          lineHeight( 1.2 ).important,
          textAlign.center,
        )
      } else {
        style()
      }
    }


    val actions = {
      if (isRenderIos) {
        style(
          justifyContent.spaceEvenly,
        )
      } else {
        style()
      }
    }

  }


  initInnerObjects(
    Dialogs.text,
  )

}
