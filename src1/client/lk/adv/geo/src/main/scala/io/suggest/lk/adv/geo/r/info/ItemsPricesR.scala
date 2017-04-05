package io.suggest.lk.adv.geo.r.info

import diode.data.Pot
import diode.react.ReactPot.potWithReact
import diode.react.ModelProxy
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 18:23
  * Description: React-компонент с таблицей текущих псевдо-item'ов и цен по ним.
  */
object ItemsPricesR {

  type Props = ModelProxy[Pot[MGetPriceResp]]

  class Backend($: BackendScope[Props, Unit]) {

    def render(potProxy: Props): ReactElement = {
      <.div(
        potProxy().render { mresp =>
          val tdCss = Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE )

          <.table(
            ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL ),

            for (i <- mresp.items) yield {
              <.tr(
                // Общая инфа по размещению
                <.td(
                  ^.`class` := tdCss, // Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                  Messages( i.iType.nameI18n ),
                  // Инфа по узлу
                  for (rcvr <- i.rcvr) yield {
                    <.span(
                      HtmlConstants.SPACE,
                      rcvr.name
                    )
                  },
                  // Инфа по географии размещения:
                  for (gsInfo <- i.gsInfo) yield {
                    <.span(
                      HtmlConstants.SPACE,
                      gsInfo
                    )
                  }
                ),

                // Цена
                <.td(
                  ^.`class` := tdCss,
                  Messages( i.price.currency.i18nPriceCode, MPrice.amountStr(i.price) )
                )

              )
            }
          )

        }
      )
    }

  }


  val component = ReactComponentB[Props]("ItemsPrices")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(respPotProxy: Props) = component( respPotProxy )

}
