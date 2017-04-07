package io.suggest.lk.adv.geo.r.bill

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot.potWithReact
import io.suggest.bill.MGetPriceResp
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.{MBillDetailedS, MBillS, ShowHideItemPriceDetails}
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import MBillDetailedS.MBillDatailedSFastEq
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.r.YmdR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 18:23
  * Description: React-компонент с таблицей текущих псевдо-item'ов и цен по ним.
  */
object ItemsPricesR {

  type Props = ModelProxy[MBillS] //Pot[MGetPriceResp]]

  protected case class State(
                              priceRespPotConn : ReactConnectProxy[Pot[MGetPriceResp]],
                              detailedOptConn  : ReactConnectProxy[Option[MBillDetailedS]]
                            )

  class Backend($: BackendScope[Props, State]) {

    private def onItemPriceClick(itemIndex: Int): Callback = {
      dispatchOnProxyScopeCB( $, ShowHideItemPriceDetails(itemIndex) )
    }

    def render(proxy: Props, s: State): ReactElement = {
      <.div(
        s.priceRespPotConn { mrespPotProxy =>
          mrespPotProxy().render { mresp =>
            val tdCss = Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE )

            <.table(
              ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL ),

              <.tbody(
                for (i <- mresp.items) yield {
                  Seq[ReactElement](
                    <.tr(
                      ^.key := i.index + "a",
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
                        ^.onClick --> onItemPriceClick( i.index ),
                        JsFormatUtil.formatPrice( i.price )
                      )
                    ),

                    s.detailedOptConn { detailedOpt =>
                      for {
                        detailed <- detailedOpt()
                        if detailed.itemIndex == i.index
                      } yield {
                        <.tr(
                          ^.key := detailed.itemIndex.toString + "d",

                          <.td(
                            ^.colSpan := 2,

                            // Ждём сервер...
                            detailed.req.renderPending { _ =>
                              <.span(
                                LkPreLoaderR.AnimMedium,
                                HtmlConstants.SPACE,
                                Messages( MsgCodes.`Please.wait` )
                              )
                            },

                            // Есть детали, рендерим ответ:
                            detailed.req.render { det =>
                              <.table(
                                ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL ),

                                <.tbody(
                                  for (d <- det.days) yield {
                                    <.tr(
                                      ^.key := d.ymd.toString,
                                      // День
                                      <.td(
                                        ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.GRAY, Css.Table.Td.Radial.FIRST ),
                                        YmdR(d.ymd)(
                                          HtmlConstants.COMMA,
                                          HtmlConstants.SPACE,
                                          JsFormatUtil.formatDow( d.ymd )
                                        )
                                      ),

                                      <.td(
                                        ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                                        Messages( d.calType.name )
                                      ),
                                      <.td(
                                        ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                                        JsFormatUtil.formatPrice( d.baseDayPrice ),
                                        Messages( MsgCodes.`_per_.day` )
                                      ),

                                      <.td(
                                        ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                                        "x", Messages( MsgCodes.`N.modules`, det.blockModulesCount )
                                      ),

                                      // Колонка информации по географии
                                      for (geoInfo <- det.geoInfo) yield {
                                        <.td(
                                          ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                                          "x",
                                          <.span(
                                            ^.title := geoInfo.priceMult.toString,
                                            "%1.2f".format( geoInfo.priceMult )
                                          ),
                                          for (radiusMeters <- geoInfo.radiusM) yield {
                                            <.span(
                                              <.br,
                                              Messages( MsgCodes.`Radius` ),
                                              " = ",
                                              Messages(
                                                MsgCodes.`n.m._meters`,
                                                radiusMeters
                                              )
                                            )
                                          }
                                        )
                                      },

                                      for (omsMult <- det.onMainScreenMult) yield {
                                        <.td(
                                          ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE ),
                                          <.span(
                                            ^.title := omsMult.toString,
                                            "x",
                                            "%1.2f".format(omsMult)
                                          )
                                        )
                                      },
                                      // TODO Остальные колонки...

                                      <.td(
                                        ^.`class` := Css.flat( Css.Table.Td.TD, Css.Table.Td.WHITE, Css.Table.Td.Radial.LAST ),
                                        JsFormatUtil.formatPrice( d.price )
                                      )
                                    )
                                  }
                                )
                              )
                            }

                          )
                        ): ReactElement
                      }
                    }
                  )
                }
              )
            )

          }.asInstanceOf[ReactElement]
        }
      )
    }

  }


  val component = ReactComponentB[Props]("ItemsPrices")
    .initialState_P { p =>
      State(
        priceRespPotConn  = p.connect(_.price.resp),
        detailedOptConn   = p.connect(_.detailed)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(respPotProxy: Props) = component( respPotProxy )

}
