package io.suggest.ads.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ads.LkAdsFormConst
import io.suggest.ads.m.{MAdProps, MLkAdsRoot}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.{Css, CssR}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.v.{JdCss, JdCssStatic}
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
import io.suggest.spa.SioPages
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:05
  * Description: Корневой react-компонент формы управления карточками узла.
  * Дабы избежать лишних проблем, все коннекшены живут здесь и все под-компоненты дёргаются только отсюда.
  */
class LkAdsFormR(
                  val adItemR     : AdItemR,
                  jdCssStatic     : JdCssStatic,
                ) {

  type Props = ModelProxy[MLkAdsRoot]

  protected[this] case class State(
                                    jdCssC        : ReactConnectProxy[JdCss],
                                    nodeAdsC      : ReactConnectProxy[Pot[Vector[MAdProps]]],
                                    parentNodeIdC : ReactConnectProxy[String]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.`class` := Css.Lk.Page.VIEWPORT,

        p.wrap(_ => jdCssStatic)( CssR.compProxied.apply ),
        s.jdCssC { CssR.compProxied.apply },

        <.article(
          ^.`class` := Css.Lk.Page.PAGE_CNT,

          // Заголовок страницы: "Рекламные карточки"
          <.h1(
            ^.`class` := Css.Lk.Page.PAGE_TITLE,
            Messages( MsgCodes.`Ad.cards` )
          ),
          <.hr(
            ^.`class` := Css.flat( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT )
          ),

          // Список карточек:
          <.div(
            ^.`class` := Css.Lk.Ads.AdsList.ADS_LIST,

            // TODO Кнопка-ссылка создания новой карточки.
            s.parentNodeIdC { parentIdProxy =>
              <.a(
                ^.`class` := Css.flat( Css.Lk.Ads.AdsList.CREATE_AD_BTN, Css.Floatt.LEFT ),
                ^.href := routes.controllers.LkAdEdit.createAd( parentIdProxy.value ).url,
                <.span(
                  Messages( MsgCodes.`Create.ad`, HtmlConstants.SPACE )
                )
              )
            },

            // Список существующих карточек:
            s.nodeAdsC { adsPotProxy =>
              val adsPot = adsPotProxy.value
              <.div(
                adsPot
                  .iterator
                  .flatten
                  .zipWithIndex
                  .flatMap { case (adProps, i) =>
                    val i1mod4eq0 = (i + 1) % LkAdsFormConst.ADS_PER_ROW ==* 0
                    val iStr = i.toString

                    // Отрендерить превьюшку карточки с обвесом:
                    val adItem = p.wrap { mroot =>
                      adItemR.PropsVal(
                        ad          = adProps,
                        firstInLine = i1mod4eq0,
                        jdRuntime   = mroot.ads.jdRuntime,
                        jdConf      = mroot.conf.jdConf
                      )
                    } { adItemR.component.withKey(iStr)(_) }(implicitly, adItemR.MLkAdItemRPropsValFastEq)
                    
                    var vdoms = List[VdomElement](adItem)
                    // Добавить перенос строки сетки после каждого четвертого элемента строки:
                    if (i1mod4eq0) {
                      vdoms ::= <.div(
                        ^.key := (iStr + "d"),
                        ^.`class` := Css.Lk.Ads.AdsList.LINE_DELIMITER
                      )
                    }
                    vdoms
                  }
                  .toVdomArray,

                ReactCommonUtil.maybeEl( adsPot.isPending ) {
                  // TODO Сверстать прелоадер по-нормальному:
                  LkPreLoaderR.AnimMedium
                }
              )
            }
          ),

          // Нижняя строка со ссылкой на выдачу.
          <.div(
            ^.`class` := Css.Lk.SM_NOTE,
            Messages( MsgCodes.`You.can.look.on.ads.in` ),
            HtmlConstants.SPACE,

            // Ссылка на выдачу, может меняться при смене текущего узла формы.
            s.parentNodeIdC { parentIdProxy =>
              val routeArgs = SioPages.Sc3(
                nodeId = Some( parentIdProxy.value )
              )
              val route = routes.controllers.sc.ScSite.geoSite(
                PlayJsonSjsUtil.toNativeJsonObj(
                  Json.toJsObject(routeArgs)
                )
              )
              // Собираем ссылку на выдачу на основе текущего nodeId:
              <.a(
                ^.`class` := Css.Lk.BLUE_LINK,
                // TODO ненужные "a."-префиксы в qs, сейчас спиливаются через replace, но надо-то как-то решить проблему.
                ^.href := ScConstants.ScJsState.fixJsRouterUrl(route.url),
                ^.target.blank,
                Messages( MsgCodes.`_ad.showcase.link` )
              )
            },
            HtmlConstants.`.`
          )

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        jdCssC        = propsProxy.connect(_.ads.jdRuntime.jdCss)( JdCss.JdCssFastEq ),
        nodeAdsC      = propsProxy.connect(_.ads.ads),
        parentNodeIdC = propsProxy.connect(_.conf.nodeKey.last)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
