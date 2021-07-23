package io.suggest.sc.m.in

import diode.data.Pot
import io.suggest.i18n.{MCommonReactCtx, MLanguage}
import io.suggest.msg.JsonPlayMessages
import japgolly.univeq.UnivEq
import monocle.macros.GenLens


object MScReactCtx {

  def default = apply()

  @inline implicit def univEq: UnivEq[MScReactCtx] = UnivEq.force

  def context = GenLens[MScReactCtx]( _.context )
  def langSwitch = GenLens[MScReactCtx]( _.langSwitch )
  def language = GenLens[MScReactCtx]( _.language )

}


/** Data container for language-related stuff and react view context.
  *
  * @param context React context value, listened by all dependent components in showcase.
  * @param language Defined system language.
  *                 Cordova: None - using system default.
  *                 WebBrowser: Sent from server via config.
  * @param langSwitch Lang switching request state.
  */
final case class MScReactCtx(
                              context         : MCommonReactCtx         = MCommonReactCtx.default,
                              langSwitch      : Pot[JsonPlayMessages]   = Pot.empty,
                              language        : Option[MLanguage]       = None,
                            )
