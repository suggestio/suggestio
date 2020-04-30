package com.github.souporserious.react.measure

import io.suggest.log.Log
import japgolly.scalajs.react._
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.08.2019 17:08
  */
object Measure extends Log {

  val component = JsComponent[MeasureProps, Children.None, Null]( MeasureJs )

  def apply(measureProps: MeasureProps) = component(measureProps)

}


@js.native
@JSImport( NPM_PACKAGE_NAME, JSImport.Default )
object MeasureJs extends js.Object

@js.native
trait MeasureChildrenArgs extends js.Object {
  val measureRef      : raw.React.RefFn[html.Element]     = js.native
  val measure         : js.Function0[Unit]                = js.native
  val contentRect     : ContentRect                       = js.native
}

trait MeasureProps extends js.Object {
  val client: js.UndefOr[Boolean] = js.undefined
  val offset: js.UndefOr[Boolean] = js.undefined
  val scroll: js.UndefOr[Boolean] = js.undefined
  val bounds: js.UndefOr[Boolean] = js.undefined
  val margin: js.UndefOr[Boolean] = js.undefined
  val innerRef: js.UndefOr[js.Function1[RefHandle_t, _]] = js.undefined
  val onResize: js.UndefOr[js.Function1[ContentRect, Unit]] = js.undefined

  /** Children rendering function.
    *
    * For example:
    * {{{
    *   import ReactCommonUtil.Implicits._
    *
    *   children = { args =>
    *     <.div(
    *       ^.refGeneric := args.measureRef,
    *       ...
    *     )
    *   }
    * }}}
    */
  val children: js.Function1[MeasureChildrenArgs, raw.PropsChildren]
}
