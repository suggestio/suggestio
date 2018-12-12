package io.suggest.sc.v.hdr

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.media.IMediaInfo
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.ScConstants
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

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
class LogoR(
             nodeNameR: NodeNameR,
           ) {

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class PropsVal(
                       logoOpt      : Option[IMediaInfo],
                       nodeNameOpt  : Option[String]
                     )

  implicit object LogoPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.logoOpt eq b.logoOpt) &&
        (a.nodeNameOpt eq b.nodeNameOpt)
    }
  }


  protected[this] case class State(
                                   nodeNameOptC: ReactConnectProxy[Option[String]]
                                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      propsProxy().whenDefinedEl { nodeInfo =>
        // Нужно решить, что вообще надо рендерить: логотип, название узла или что-то иное?
        nodeInfo.logoOpt.fold[VdomElement] {
          // Графический логотип не доступен. Попробовать отрендерить текстовый логотип.
          s.nodeNameOptC { nodeNameR.apply }

        } { logoInfo =>
          // Рендерим картинку графического логотипа узла.
          <.img(
            ^.src := logoInfo.url,
            nodeInfo.nodeNameOpt.whenDefined { nodeName =>
              ^.title := nodeName
            },
            ^.height := ScConstants.Logo.HEIGHT_CSSPX.px
          )
        }

      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { proxy =>
      State(
        nodeNameOptC = proxy.connect( _.flatMap(_.nodeNameOpt) )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(nodeInfoOptProxy: Props) = component( nodeInfoOptProxy )

}
