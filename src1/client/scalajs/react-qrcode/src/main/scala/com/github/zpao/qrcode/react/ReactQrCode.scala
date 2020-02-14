package com.github.zpao.qrcode.react

import japgolly.scalajs.react._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 15:50
  * Description: sjs-биндинг для react-компонента qr-code.
  */
object ReactQrCode {

  val component = JsComponent[ReactQrCodeProps, Children.None, Null]( ReactQrCodeJs )

  def apply(props: ReactQrCodeProps) =
    component( props )


  type RenderAs_t <: js.Any
  object RenderAs {
    final def CANVAS = "canvas".asInstanceOf[RenderAs_t]
    final def SVG = "svg".asInstanceOf[RenderAs_t]
  }


  type Level_t <: js.Any
  object Levels {
    final def L = "L".asInstanceOf[Level_t]
    final def M = "M".asInstanceOf[Level_t]
    final def Q = "Q".asInstanceOf[Level_t]
    final def H = "H".asInstanceOf[Level_t]
  }

}


@js.native
@JSImport("qrcode.react", JSImport.Namespace)
object ReactQrCodeJs extends js.Object


trait ReactQrCodeProps extends js.Object {
  val value: String
  val renderAs: js.UndefOr[ReactQrCode.RenderAs_t] = js.undefined
  val size: js.UndefOr[Int] = js.undefined
  val bgColor: js.UndefOr[String] = js.undefined
  val fgColor: js.UndefOr[String] = js.undefined
  val level: js.UndefOr[ReactQrCode.Level_t] = js.undefined
  val includeMargin: js.UndefOr[Boolean] = js.undefined
  val imageSettings: js.UndefOr[ReactQrCodeImageSettings] = js.undefined
}


trait ReactQrCodeImageSettings extends js.Object {
  val src: String
  val x: js.UndefOr[Int] = js.undefined
  val y: js.UndefOr[Int] = js.undefined
  val height: js.UndefOr[Int] = js.undefined
  val width: js.UndefOr[Int] = js.undefined
  val excavate: js.UndefOr[Boolean] = js.undefined
}
