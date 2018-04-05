package io.suggest.adn.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.m._
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import io.suggest.css.ScalaCssDefaults._
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.PropTableR
import io.suggest.lk.r.color.{ColorBtnR, ColorPickerR}
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.vdom.TagOf
import io.suggest.spa.OptFastEq
import org.scalajs.dom.html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 19:33
  * Description: Корневой react-компонент формы редактирования ADN-узла.
  */
class LkAdnEditFormR(
                      oneRowR             : OneRowR,
                      val colorPickerR    : ColorPickerR,
                      val colorBtnR       : ColorBtnR
                    ) {

  import colorPickerR.ColorPickerPropsValFastEq
  import colorBtnR.ColorBtnRPropsValFastEq

  type Props = ModelProxy[MLkAdnEditRoot]

  val css = new LkAdnEditCss

  case class State(
                    nameC           : ReactConnectProxy[Option[String]],
                    townC           : ReactConnectProxy[Option[String]],
                    addressC        : ReactConnectProxy[Option[String]],
                    siteUrlC        : ReactConnectProxy[Option[String]],
                    infoAboutC      : ReactConnectProxy[Option[String]],
                    humanTrafficC   : ReactConnectProxy[Option[Int]],
                    audienceDescrC  : ReactConnectProxy[Option[String]],
                    bgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    fgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    colorPickerC    : ReactConnectProxy[colorPickerR.Props_t],
                  )

  /** Рендер одного text-input'а. */
  private def __textInput[T](
                              msgCode      : String,
                              onChangeF    : ReactEventFromInput => Callback,
                              conn         : ReactConnectProxy[Option[T]],
                              tag          : TagOf[html.Element],
                              isRequired   : Boolean = false
                            ): VdomElement = {
    val inputId = msgCode.toLowerCase
    oneRowR(
      oneRowR.PropsVal(
        nameCode    = msgCode,
        inputId     = inputId,
        isRequired  = isRequired
      )
    )(
      conn { valueProxy =>
        tag(
          ^.id        := inputId,
          ^.value     := valueProxy.value.fold("")(_.toString),
          ^.onChange ==> onChangeF
        )
      }
    )
  }


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

    def render(props: Props, s: State): VdomElement = {
      val delimHr = <.hr(
        ^.`class` := Css.flat( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT )
      )

      val textArea = <.textarea
      val inputText = <.input(
        ^.`type` := HtmlConstants.Input.text
      )

      val paddingS = Css.Lk.Paddings.S

      <.div(
        // Отрендерить доп.стили
        <.styleTag(
          css.render[String]
        ),

        <.div(
          css.logoBar,
          "LOGO"
        ),

        <.div(
          css.infoBar,

          delimHr,

          // Поле названия узла:
          __textInput( MsgCodes.`Name`, onNameChange, s.nameC, inputText, true ),

          // Поле города узла:
          __textInput( MsgCodes.`Town`, onTownChange, s.townC, inputText ),

          // Поле адреса:
          __textInput( MsgCodes.`Address`, onAddressChange, s.addressC, textArea ),

          // Поле URL сайта:
          __textInput( MsgCodes.`Site`, onSiteUrlChange, s.siteUrlC, inputText ),

          // Инфа о товарах и услугах:
          __textInput( MsgCodes.`Info.about.prods.and.svcs`, onInfoAboutProductsChange, s.infoAboutC, textArea ),


          delimHr,

          // Поле задания человеческого трафика.
          __textInput( MsgCodes.`Daily.people.traffic`, onHumanTrafficChange, s.humanTrafficC, inputText ),

          // Поле описания аудитории.
          __textInput( MsgCodes.`Audience.descr`, onAudienceDescrChange, s.audienceDescrC, textArea ),


          delimHr,

          // TODO Экран приветствия

          // TODO Фотографии магазина.


          // Цвета фона и контента
          PropTableR.Outer(
            <.tr(
              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                Messages( MsgCodes.`Bg.color` )
              ),
              __colorTd( paddingS :: Nil )(
                s.bgColorC { colorBtnR.apply }
              ),

              // TODO линия-разделитель

              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                "TODO Messages( MsgCodes.`Fg.color` )"
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

      def __getColorBtnC(colorF: MColors => Option[MColorData]): ReactConnectProxy[colorBtnR.Props_t] = {
        propsProxy.connect { props =>
          for {
            mcd <- colorF(props.node.meta.colors)
          } yield {
            colorBtnR.PropsVal(
              color = mcd
            )
          }
        }( OptFastEq.Wrapped )
      }

      State(
        nameC = propsProxy.connect[Option[String]] { props =>
          // TODO Opt Надо бы коннектить просто String, а не Option[String], но функция __textInput пока заточена под Option.
          Some( props.node.meta.name )
        }( OptFastEq.Plain ),
        townC = propsProxy.connect { props =>
          props.node.meta.address.town
        }( OptFastEq.Plain ),
        addressC = propsProxy.connect { props =>
          props.node.meta.address.address
        }( OptFastEq.Plain ),
        siteUrlC = propsProxy.connect { props =>
          props.node.meta.business.siteUrl
        }( OptFastEq.Plain ),
        infoAboutC = propsProxy.connect { props =>
          props.node.meta.business.info
        }( OptFastEq.Plain ),
        humanTrafficC = propsProxy.connect { props =>
          props.node.meta.business.humanTrafficAvg
        }( OptFastEq.OptValueEq ),
        audienceDescrC = propsProxy.connect { props =>
          props.node.meta.business.audienceDescr
        }( OptFastEq.Plain ),
        bgColorC = __getColorBtnC( MColors.bgF ),
        fgColorC = __getColorBtnC( MColors.fgF ),
        colorPickerC = propsProxy.connect { props =>
          for {
            // TODO Тут хрень полная. Нужна поддержка ColorPicker'а, но понимающего разные режимы: Bg и Fg. Как раз недавно была удалена соответствующая enum-модель.
            _ <- props.node.colorPicker
            mcd <- props.node.meta.colors.allColorsIter.toStream.headOption
          } yield {
            colorPickerR.PropsVal(
              color         = mcd,
              colorPresets  = props.node.colorPresets,
              topLeftPx     = props.node.colorPicker
            )
          }
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProps: Props) = component(rootProps)

}
