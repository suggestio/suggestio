package io.suggest.sc.v

import diode.react.ModelProxy
import io.suggest.sc.styl.ScScalaCssDefaults._
import io.suggest.sc.styl.IScCssArgs
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 16:09
  * Description: React-компонент, рендерящий динамический css выдачи.
  */
class ScCssR(
              scCssFactory: ScCssFactory
            ) {

  type Props = ModelProxy[IScCssArgs]

  class Backend($: BackendScope[Props, Unit]) {
    def render(props: Props): VdomElement = {
      val scCss = scCssFactory.mkScCss(props.value)

      <.styleTag(
        scCss.render[String]
      )
    }
  }


  val component = ScalaComponent.builder[Props]("ScCss")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(colorsProxy: Props) = component( colorsProxy )

}


import com.softwaremill.macwire._
import io.suggest.sc.styl.{IScCssArgs, ScCss}

/** Factory-модуль для сборки инстансов ScCss, который зависит от аргументов рендера,
  * но допускает использование как-то внешних зависимостей.
  */
class ScCssFactory {

  /** Параметризованная сборка ScCss (здесь можно добавлять DI-зависимости). */
  def mkScCss(args: IScCssArgs): ScCss = {
    wire[ScCss]
  }

}
