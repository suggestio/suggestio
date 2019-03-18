package io.suggest.id.login.v

import io.suggest.css.ScalaCssDefaults._
import japgolly.univeq.UnivEq
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.03.19 12:18
  * Description: Стили для login-формы.
  */
object LoginFormCss {

  @inline implicit def univEq: UnivEq[LoginFormCss] = UnivEq.derive

}


/** CSS-стили для формы логина. */
case class LoginFormCss() extends StyleSheet.Inline {

  import dsl._

  /** Стиль контейнера диалога. */
  val dialogCont = {
    style(
      maxWidth( 500.px )
    )
  }

  /** Стиль текстового поля для ввода имени или пароля. */
  val epwFormControl = {
    style(
      margin( 10.px, 20.px ),
      //marginRight( px20 ),
      maxWidth( 300.px ),
    )
  }

}
