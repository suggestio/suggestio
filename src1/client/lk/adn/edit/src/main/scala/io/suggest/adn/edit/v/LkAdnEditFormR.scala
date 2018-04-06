package io.suggest.adn.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.m._
import io.suggest.css.Css
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import io.suggest.css.ScalaCssDefaults._
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.DocBodyClick
import io.suggest.lk.r.PropTableR
import io.suggest.lk.r.color.{ColorBtnR, ColorPickerR}
import io.suggest.model.n2.node.meta.colors.{MColorType, MColorTypes}
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.spa.OptFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 19:33
  * Description: Корневой react-компонент формы редактирования ADN-узла.
  */
class LkAdnEditFormR(
                      val oneRowR         : OneRowR,
                      val colorPickerR    : ColorPickerR,
                      val colorBtnR       : ColorBtnR
                    ) {

  import oneRowR.OneRowRValueValFastEq
  import colorPickerR.ColorPickerPropsValFastEq
  import colorBtnR.ColorBtnRPropsValFastEq

  type Props = ModelProxy[MLkAdnEditRoot]

  val css = new LkAdnEditCss

  case class State(
                    nameC           : ReactConnectProxy[oneRowR.ValueVal],
                    townC           : ReactConnectProxy[oneRowR.ValueVal],
                    addressC        : ReactConnectProxy[oneRowR.ValueVal],
                    siteUrlC        : ReactConnectProxy[oneRowR.ValueVal],
                    infoAboutC      : ReactConnectProxy[oneRowR.ValueVal],
                    humanTrafficC   : ReactConnectProxy[oneRowR.ValueVal],
                    audienceDescrC  : ReactConnectProxy[oneRowR.ValueVal],
                    bgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    fgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    colorPickerC    : ReactConnectProxy[colorPickerR.Props_t],
                  )

  private def __colorTd(cssClasses: List[String])(content: TagMod): VdomElement = {
    <.td(
      ^.`class` := Css.flat1( Css.Table.Td.TD :: cssClasses ),
      content
    )
  }


  class Backend($: BackendScope[Props, State]) {

    private def onNameChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetName(e.target.value) )
    }

    private def onTownChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetTown(e.target.value) )
    }

    private def onAddressChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetAddress(e.target.value) )
    }

    private def onSiteUrlChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetSiteUrl(e.target.value) )
    }

    private def onInfoAboutProductsChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetInfo(e.target.value) )
    }

    private def onHumanTrafficChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetHumanTraffic(e.target.value) )
    }

    private def onAudienceDescrChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetAudienceDescr(e.target.value) )
    }

    private def onDocumentClick: Callback = {
      dispatchOnProxyScopeCB( $, DocBodyClick )
    }

    def render(props: Props, s: State): VdomElement = {
      val delimHr = <.hr(
        ^.`class` := Css.flat( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT )
      )

      val paddingS = Css.Lk.Paddings.S

      <.div(
        ^.onClick --> onDocumentClick,

        // Отрендерить доп.стили
        <.styleTag(
          css.render[String]
        ),

        <.div(
          css.logoBar,
          // TODO Логотип узла
          "LOGO"
        ),

        <.div(
          css.infoBar,

          delimHr,

          // Поле названия узла:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Name`, onNameChange, s.nameC, isRequired = true) ),

          // Поле города узла:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Town`, onTownChange, s.townC ) ),

          // Поле адреса:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Address`, onAddressChange, s.addressC, isTextArea = true) ),

          // Поле URL сайта:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Site`, onSiteUrlChange, s.siteUrlC )),

          // Инфа о товарах и услугах:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Info.about.prods.and.svcs`, onInfoAboutProductsChange, s.infoAboutC, isTextArea = true )),


          delimHr,

          // Поле задания человеческого трафика.
          oneRowR( oneRowR.PropsVal( MsgCodes.`Daily.people.traffic`, onHumanTrafficChange, s.humanTrafficC ) ),

          // Поле описания аудитории.
          oneRowR( oneRowR.PropsVal( MsgCodes.`Audience.descr`, onAudienceDescrChange, s.audienceDescrC, isTextArea = true ) ),

          delimHr,
          // TODO Экран приветствия

          delimHr,
          // TODO Фотографии магазина.

          delimHr,

          // Цвета фона и контента
          PropTableR.Outer(
            <.tr(
              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                Messages( MsgCodes.`Bg.color` )
              ),
              __colorTd( paddingS :: Nil )(
                s.bgColorC { colorBtnR.apply }
              ),

              // Вертикальная линия-разделитель:
              __colorTd( paddingS :: Nil )(
                <.div(
                  css.colorTdVerticalHr
                )
              ),

              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                Messages( MsgCodes.`Fg.color.of.sc.hint` )
              ),
              __colorTd( paddingS :: Nil )(
                s.fgColorC { colorBtnR.apply }
              )
            )
          )

        ),

        s.colorPickerC { colorPickerR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Form")
    .initialStateFromProps { propsProxy =>
      // Сборка коннекшена до цвета:
      def __getColorBtnC(colorType: MColorType): ReactConnectProxy[colorBtnR.Props_t] = {
        val colorTypeSome = Some(colorType)
        propsProxy.connect { props =>
          for {
            mcd <- props.node.meta.colors.ofType( colorType )
          } yield {
            colorBtnR.PropsVal(
              color     = mcd,
              colorType = colorTypeSome
            )
          }
        }( OptFastEq.Wrapped )
      }

      val emptyStr = ""
      def emptyStrF: String = emptyStr
      // Сборка тривиального коннекшена до ValueVal
      def __getStringOptConn(getValF: MLkAdnEditRoot => Option[String]): ReactConnectProxy[oneRowR.ValueVal] = {
        propsProxy.connect { mroot =>
          oneRowR.ValueVal(
            value = getValF(mroot).getOrElse(emptyStrF)
          )
        }( OneRowRValueValFastEq )
      }

      State(
        nameC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.name,
            error = props.node.errors.name
          )
        }( OneRowRValueValFastEq ),
        townC = __getStringOptConn( _.node.meta.address.town ),
        addressC = __getStringOptConn( _.node.meta.address.address ),
        siteUrlC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.business.siteUrl.getOrElse(emptyStrF),
            error = props.node.errors.siteUrl
          )
        }( OneRowRValueValFastEq ),
        infoAboutC = __getStringOptConn( _.node.meta.business.info ),
        humanTrafficC = __getStringOptConn( _.node.meta.business.humanTraffic ),
        audienceDescrC = __getStringOptConn( _.node.meta.business.audienceDescr ),
        bgColorC = __getColorBtnC( MColorTypes.Bg ),
        fgColorC = __getColorBtnC( MColorTypes.Fg ),
        colorPickerC = propsProxy.connect { props =>
          for {
            cps <- props.node.colorPicker
            mcd <- props.node.meta.colors.ofType( cps.ofColorType )
          } yield {
            colorPickerR.PropsVal(
              color         = mcd,
              colorPresets  = props.node.colorPresets,
              topLeftPx     = Some( cps.topLeftPx ),
              cssClass      = Some( css.colorPicker.htmlClass )
            )
          }
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProps: Props) = component(rootProps)

}
