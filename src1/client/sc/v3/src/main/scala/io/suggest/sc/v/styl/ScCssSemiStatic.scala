package io.suggest.sc.v.styl

import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MPlatformS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.07.2020 17:48
  * Description: Полу-статические стили, зависящие от платформы.
  */
final class ScCssSemiStatic( plat: MPlatformS ) extends StyleSheet.Inline {

  import dsl._

  private def _renderIos: Boolean =
    plat.isUseIosStyles

  /** Компоненты. */
  object Dialogs {

    val title = {
      if (_renderIos)
        style(
          fontWeight.bolder.important,
          justifyContent.center,
          display.flex,
        )
      else
        style()
    }


    val titleIcon = {
      if (_renderIos) {
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
      if (_renderIos) {
        style(
          borderRadius( 20.px ),
          opacity.attr := 90.%%.value,
        )
      } else {
        style()
      }
    }


    val text = {
      if (_renderIos) {
        style(
          lineHeight( 1.2 ).important,
          textAlign.center,
        )
      } else {
        style()
      }
    }


    val actions = {
      if (_renderIos) {
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
