package io.suggest.sc.v.hdr

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.media.IMediaInfo
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.ScConstants
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import scalacss.ScalaCssReact._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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

  /**
    * @param logoOpt Графический логотип узла.
    * @param nodeNameOpt Текстовое имя узла.
    * @param styled Стилизовать цветами узла?
    */
  case class PropsVal(
                       logoOpt      : Option[IMediaInfo],
                       nodeNameOpt  : Option[String],
                       styled       : Boolean,
                     )
  implicit object LogoPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.logoOpt     ===* b.logoOpt) &&
      (a.nodeNameOpt ===* b.nodeNameOpt) &&
      (a.styled       ==* b.styled)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { nodeInfo =>
        // Нужно решить, что вообще надо рендерить: логотип, название узла или что-то иное?
        nodeInfo.logoOpt.fold[VdomElement] {
          // Графический логотип не доступен. Попробовать отрендерить текстовый логотип.
          propsProxy.wrap { _ =>
            for (nodeName <- nodeInfo.nodeNameOpt) yield {
              nodeNameR.PropsVal(
                nodeName = nodeName,
                styled   = nodeInfo.styled
              )
            }
          }( nodeNameR.apply )(implicitly, OptFastEq.Wrapped(nodeNameR.NodeNameRPropsValFastEq) )

        } { logoInfo =>
          // Рендерим картинку графического логотипа узла.
          <.img(
            ScCssStatic.Header.Logo.Img.hdr,
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodeInfoOptProxy: Props) = component( nodeInfoOptProxy )

}
