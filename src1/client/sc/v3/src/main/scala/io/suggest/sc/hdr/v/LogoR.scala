package io.suggest.sc.hdr.v

import diode.react.{ModelProxy, ReactConnectProxy}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.spa.OptFastEq.Plain
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.styl.ScCss.scCss

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 21:14
  * Description: Компонент логотипа узла для заголовка выдачи.
  *
  * В отличии от исходной концепции, здесь у нас узел может быть не совсем существующим:
  * например, просмотр карточек-предложений в каком-то теге.
  * Логотип узла как бы отражает эту сущность.
  */
object LogoR {

  type PropsVal = Option[MSc3IndexResp]
  type Props = ModelProxy[PropsVal]


  protected[this] case class State(
                                   nodeNameOptC: ReactConnectProxy[Option[String]]
                                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      propsProxy().whenDefinedEl { nodeInfo =>

        val logoCss = scCss.Header.Logo

        // Нужно решить, что вообще надо рендерить: логотип, название узла или что-то иное?
        nodeInfo.logoOpt.fold[VdomElement] {
          // Графический логотип не доступен. Попробовать отрендерить текстовый логотип.
          s.nodeNameOptC { NodeNameR.apply }

        } { logoInfo =>
          val imgStyles = logoCss.Img
          // Рендерим картинку графического логотипа узла.
          <.img(
            imgStyles.logo,
            ^.src := logoInfo.url,
            nodeInfo.name.whenDefined { nodeName =>
              ^.title := nodeName
            },
            ^.height := imgStyles.IMG_HEIGHT_CSSPX.px
          )
        }

      }
    }

  }


  val component = ScalaComponent.builder[Props]("Logo")
    .initialStateFromProps { proxy =>
      State(
        nodeNameOptC = proxy.connect( _.flatMap(_.name) )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(nodeInfoOptProxy: Props) = component( nodeInfoOptProxy )

}
