package io.suggest.lk.r

import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:56
  * Description: react-компоненты для прелоадеров в ЛК.
  */
object LkPreLoaderR {

  private def pleaseWait = Messages( MsgCodes.`Please.wait` )


  val TextC = ScalaComponent.builder[Unit]("TextPreLoader")
    .stateless
    .render { _ =>
      <.span(
        pleaseWait,
        HtmlConstants.ELLIPSIS
      )
    }
    .build
  def Text = TextC()


  val Anim = ScalaComponent.builder[Int]("PreLoader")
    .stateless
    .render_P { widthPx =>
      <.span(
        // Чтобы alt был маленькими буквами, если картинка не подгрузилась
        ^.`class` := Css.Font.Sz.S,

        LkPreLoader.PRELOADER_IMG_URL
          .fold[VdomElement]( Text ) { url =>
            <.img(
              ^.src   := url,
              ^.title := pleaseWait,
              ^.width := widthPx.px
            )
          }
      )
    }
    .build


  def AnimSmall  = Anim(15)

  def AnimMedium = Anim(22)

}
