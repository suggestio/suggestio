package io.suggest.css

import io.suggest.css.ScalaCssDefaults._
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import scalacss.internal.DslBase.ToStyle
import scalacss.internal.ValueT.TypedAttrBase
import scalacss.internal.mutable.StyleSheet
import scalacss.internal.{Literal, StyleA, StyleS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 16:06
  * Description: Внутренняя утиль для ScalaCSS.
  */
object ScalaCssUtil {

  object Implicits {

    implicit class ScssOptionExt[T](val opt: Option[T]) extends AnyVal {
      def whenDefinedStyleS(f: T => StyleS): StyleS = {
        opt.fold(StyleS.empty)(f)
      }
    }


    /** Дополнительные литералы для CSS. */
    implicit class CssLiteralsExt( val literals: Literal.type ) extends AnyVal {
      import scalacss.internal.Value

      def env: Value = "env"
      def constant: Value = "constant"

      /** Названия функций iOS для доступа к переменным окружения CSS в порядке css-fallback
        * (новые - в конце, старые - в начале). */
      def iosEnvFcns: List[Value] =
        constant :: env :: Nil

      /** Сдвиг контента для iOS, когда экран содержит небезопасные для рендера области (iphone 10 часы в углу). */
      def `safe-area-inset-`: Value = "safe-area-inset-"
      def `safe-area-inset-top`: Value = `safe-area-inset-` + literals.top
      def `safe-area-inset-left`: Value = `safe-area-inset-` + literals.left

      /** Отрендерить css-функцию. */
      def envFcnCall(fcnName: Value, arg0: Value): Value =
        fcnName + `(` + arg0 + `)`

    }

  }

}


/** Аддон для впихивания стилей для  */
trait SafeAreaInsetStyles extends StyleSheet.Inline {

  import dsl._
  import ScalaCssUtil.Implicits._


  object IosSafeAreaInset {

    protected[this] def _safeAreaPaddingInset(typAttr: TypedAttrBase, cssFcnArgName: String): StyleA = {
      // TODO Opt Не надо генерить эти стили, если iOS < 10. В scalacss есть для этого утиль кое-какая.
      //      https://japgolly.github.io/scalacss/book/features/typed-values.html
      //      val alignItems = Attr.real("align-items", Transform keys CanIUse.flexbox)
      val styles = for {
        fcnName <- Literal.iosEnvFcns
      } yield {
        (typAttr.attr := Literal.envFcnCall(fcnName, cssFcnArgName)): ToStyle
      }
      style( styles: _* )
    }

    /** Сдвиг вниз по вертикали */
    val marginSafeAreaInsetTop =
      _safeAreaPaddingInset( marginTop,  Literal.`safe-area-inset-top` )

    /** Сдвиг вправо по горизонтали. */
    val marginSafeAreaInsetLeft =
      _safeAreaPaddingInset( marginLeft, Literal.`safe-area-inset-left` )

    /** В зависимости от ориентации экрана, вернуть тот или иной сдвиг экрана для ios. */
    def marginSafeAreaInsetTopLeft(isVertical: Boolean): StyleA = {
      if (isVertical) marginSafeAreaInsetTop
      else marginSafeAreaInsetLeft
    }

  }

  initInnerObjects( IosSafeAreaInset.marginSafeAreaInsetTop )

}
