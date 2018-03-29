package io.suggest.ads.v

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.ad.blk.BlockHeights
import io.suggest.ads.m.{MAdProps, SetAdShownAtParent}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil
import ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.Messages
import io.suggest.routes.routes
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.03.18 18:28
  * Description: react-компонент для рендера одной карточки в списке карточек узла.
  */
class AdItemR(
               jdR: JdR
             ) {

  import MJdArgs.MJdArgsFastEq

  case class PropsVal(
                       ad           : MAdProps,
                       firstInLine  : Boolean,
                       jdCss        : JdCss,
                       jdConf       : MJdConf
                     )
  implicit object MLkAdItemRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.ad ===* b.ad) &&
        (a.firstInLine ==* b.firstInLine) &&
        (a.jdCss ===* b.jdCss) &&
        (a.jdConf ===* b.jdConf)
    }
  }


  type Props = ModelProxy[PropsVal]

  class Backend($: BackendScope[Props, PropsVal]) {

    /** Callback клика по галочке отображения карточки на продьюсере. */
    private def _adShownOnParentChanged(adId: String)(event: ReactEventFromInput): Callback = {
      val isShown = event.target.checked
      dispatchOnProxyScopeCB($, SetAdShownAtParent(adId, isShown))
    }

    def render(propsProxy: Props, s: PropsVal): VdomElement = {
      val ItemCss = Css.Lk.Ads.AdsList.Item
      <.div(
        ^.`class` := {
          var cssAcc = List.empty[String]
          if (s.firstInLine)
            cssAcc ::= Css.Lk.Ads.AdsList.FIRST_IN_LINE
          cssAcc ::= ItemCss.AD_ITEM
          Css.flat1( cssAcc )
        },

        // Рендер превьюшки карточки: TODO Завернуть в коннекшен или как-то ещё защитить от перерендера.
        <.div(
          ^.`class` := ItemCss.AD_ITEM_PREVIEW,

          // Рендер
          <.div(
            // TODO Почему-то все карточки сжимаются. Вероятно, этот стиль неактуален.
            //^.`class` := ItemCss.AD_ITEM_PREVIEW_CONTAINER,

            // Рендер карточки:
            propsProxy.wrap { props =>
              val jdData = props.ad.adResp.jdAdData
              MJdArgs(
                template  = jdData.template,
                edges     = jdData.edgesMap
                  .mapValues( MEdgeDataJs(_) ),
                jdCss     = props.jdCss,
                conf      = props.jdConf
              )
            }(jdR.apply)
          ),

          // Если блок по высоте великоват, то нарисовать линию отреза:
          if (s.ad.adResp.jdAdData.template.rootLabel.props1.bm.exists(_.height >= BlockHeights.H460.value)) {
            <.div(
              ^.`class` := ItemCss.AD_ITEM_PREVIEW_BOTTOM_ZIGZAG
            )
          } else {
            EmptyVdom
          }
        ),

        // Утиль управления карточкой. Доступна, когда известен id карточки (по факту - всегда):
        s.ad.adResp.jdAdData.nodeId.whenDefined { adId =>
          <.div(
            // Блок с ссылкой на редактор и галочкой размещения на parent-узле.
            <.div(
              // TODO В оригинальном быдлокоде здесь было js-equal-height, выставлявший height = 49px. Может быть, надо этот стиль выставить здесь?
              <.a(
                ^.`class` := ItemCss.EDIT_BTN,
                ^.href := routes.controllers.LkAdEdit.editAd( adId ).url,
                <.span
              ),

              // TODO Завернуть в коннекшен, который переключается только при изменении галочки.
              <.div(
                ^.`class` := ItemCss.CONTROLS,
                <.label(
                  VdomArray(
                    <.input(
                      ^.key := "1",
                      ^.`class` := Css.Input.CHECKBOX,
                      ^.`type`  := HtmlConstants.Input.checkbox,
                      ^.checked := s.ad.adResp.shownAtParent,
                      ^.onChange ==> _adShownOnParentChanged( adId )
                    ),
                    <.span(
                      ^.key := "2",
                      ^.`class` := Css.Input.STYLED_CHECKBOX,
                      HtmlConstants.NBSP_STR
                    )
                  ),
                  Messages( MsgCodes.`Show._ad` ),

                  // Прелоадер.
                  if (s.ad.shownAtParentReq.isPending)
                    LkPreLoaderR.AnimSmall
                  else EmptyVdom
                )
              )
            ),

            // Ссылка на управление карточкой
            <.a(
              ^.`class` := Css.flat( Css.Buttons.BTN, Css.Size.XM, Css.Buttons.RADIAL_MAJOR ),
              ^.href := routes.controllers.LkAdvGeo.forAd( adId ).url,
              Messages( MsgCodes.`ad.Manage` )
            )
          )
        }  // adId

      )
    }

  }


  val component = ScalaComponent.builder[Props]("MLkAdItem")
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

  def _apply(props: Props) = component(props)
  val apply: ReactConnectProps[PropsVal] = _apply

}
