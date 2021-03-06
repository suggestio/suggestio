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

  /** Стиль текстового поля для ввода имени или пароля. */
  val formControl = {
    style(
      margin( 10.px, 20.px ),
      //marginRight( px20 ),
      //maxWidth( 300.px ),   // 4.x - не нужно, видимо.
    )
  }


  /** Параметры анимированного прогресса ожидания. */
  val progressBar = {
    style(
      width( 100.%% ),
      height( 2.px ),
    )
  }


  /** Стили для переключателя регистрации. */
  val regStepper = style(
    position.relative,
    background := none,
  )


  val forgotPassword = style(
    marginLeft( 20.px )
  )


  /** Цвет фона для успешных сообщений. */
  val bgColorSuccess = style(
    backgroundColor.green
  )


  val lkDiaRoot = style(
    zIndex( 130 ).important,
  )


  /** Контейнер заголовков табов. */
  val tabsCont = style(
    justifyContent.spaceAround,
  )

  /** Доп.утиль под полями ввода имени-пароля. */
  val epwUtil = style(
    display.flex,
    justifyContent.spaceBetween,
    alignItems.center,
  )


  /** Стили для окошка формы логина. */
  val diaWindow = style(
    minHeight( 360.px )
  )

  /** Высота 100% */
  val h100 = style(
    height( 100.%% ),
  )

  val stepperContent = style(
    display.flex,
    flexDirection.column,
    justifyContent.spaceBetween,
    flexGrow( 50 ),
  )

}
