package io.suggest.ads.v

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import diode.react.ReactPot._
import io.suggest.ad.blk.BlockHeights
import io.suggest.ads.m.{MAdProps, SetAdShownAtParent}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.{MJdArgs, MJdRuntime}
import io.suggest.jd.render.v.JdR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
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

  case class PropsVal(
                       ad           : MAdProps,
                       firstInLine  : Boolean,
                       jdRuntime    : MJdRuntime,
                       jdConf       : MJdConf,
                     )
  implicit object MLkAdItemRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.ad ===* b.ad) &&
      (a.firstInLine ==* b.firstInLine) &&
      (a.jdRuntime ===* b.jdRuntime) &&
      (a.jdConf ===* b.jdConf)
    }
  }

  private val _itemStatusesGreen = Set[MItemStatus]( MItemStatuses.Offline, MItemStatuses.Online )

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

        // Покрытие зелёное или синее.
        <.span(
          ^.classSet1(
            ItemCss.AdvStatusOvh.ADV_ITEM_STATUS,
            ItemCss.AdvStatusOvh.ONLINE       -> (s.ad.adResp.advStatuses intersect _itemStatusesGreen).nonEmpty,
            ItemCss.AdvStatusOvh.AWAITING_MDR -> (s.ad.adResp.advStatuses contains MItemStatuses.AwaitingMdr)
          )
        ),

        // Рендер превьюшки карточки: TODO Завернуть в коннекшен или как-то ещё защитить от перерендера.
        <.div(
          ^.`class` := ItemCss.AD_ITEM_PREVIEW,

          // Рендер
          <.div(
            // Рендер карточки:
            propsProxy.wrap { props =>
              val jdData = props.ad.jdDataJs
              MJdArgs(
                data      = jdData,
                jdRuntime = props.jdRuntime,
                conf      = props.jdConf,
              )
            }(jdR.apply)(implicitly, MJdArgs.MJdArgsFastEq),
          ),

          // Если блок по высоте великоват, то нарисовать линию отреза:
          ReactCommonUtil.maybeNode(
            s.ad.adResp.jdAdData.doc.template
              .rootLabel
              .props1
              .heightPx
              .exists(_ >= BlockHeights.H460.value)
          ) {
            <.div(
              ^.`class` := ItemCss.AD_ITEM_PREVIEW_BOTTOM_ZIGZAG
            )
          }
        ),

        // Утиль управления карточкой. Доступна, когда известен id карточки (по факту - всегда):
        s.ad.adResp.jdAdData.doc.tagId.nodeId.whenDefined { adId =>
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
                      ReactCommonUtil.maybe( s.ad.shownAtParentReq.isPending ) {
                        ^.disabled := true
                      },
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
                  ReactCommonUtil.maybeEl( s.ad.shownAtParentReq.isPending ) {
                    LkPreLoaderR.AnimSmall
                  },

                  // Рендер ошибки запроса, если есть.
                  s.ad.shownAtParentReq.renderFailed { ex =>
                    <.span(
                      ^.`class` := Css.Input.ERROR,
                      Messages( MsgCodes.`Error` ),
                      HtmlConstants.SPACE,
                      ex.getMessage
                    )
                  }

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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

  def _apply(props: Props) = component(props)
  val apply: ReactConnectProps[PropsVal] = _apply

}
