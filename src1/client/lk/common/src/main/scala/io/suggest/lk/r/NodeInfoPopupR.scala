package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.css.Css
import io.suggest.lk.m.NodeInfoPopupClose
import io.suggest.lk.pop.PopupR
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 16:54
  * Description: Компонент попапа-окна, содержит метаданные по какому-то узлу.
  * Построен на базе
  */
object NodeInfoPopupR {

  case class PropsVal(
                       info: MNodeAdvInfo
                     )

  /** Поддержка FastEq для модели аргументов PropsVal. */
  implicit object NodeMetaPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      true    // TODO
    }
  }


  type Props = ModelProxy[Option[PropsVal]]



  /** Ядро компонента содержимого попапа. */
  class Backend($: BackendScope[Props, Unit]) {

    private def onCloseClick: Callback = {
      dispatchOnProxyScopeCB( $, NodeInfoPopupClose )
    }

    def render(p: Props): ReactElement = {
      for (v <- p()) yield {

        // Завернуть аргументы попапа в прокси, как требует интерфейс PopupR.
        p.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some( onCloseClick )
          )
        } { popPropsProxy =>

          PopupR( popPropsProxy ) (
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages("???")
            ),

            <.div(
              "TODO"
            )
          )

        }

      }
    }

  }


  val component = ReactComponentB[Props]("NodeInfoPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
