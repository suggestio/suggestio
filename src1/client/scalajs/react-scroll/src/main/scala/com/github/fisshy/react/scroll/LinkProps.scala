package com.github.fisshy.react.scroll

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:10
  */
trait LinkProps extends js.Object {

  val activeClass         : js.UndefOr[String]                    = js.undefined
  // В link оно помечено как required
  val to                  : js.UndefOr[String]                    = js.undefined
  val containerId         : js.UndefOr[String]                    = js.undefined
  val spy                 : js.UndefOr[Boolean]                   = js.undefined

  /** true | false | "easeInOutQuint" | ...
    * @see [[https://github.com/fisshy/react-scroll#scroll-animations]] */
  val smooth              : js.UndefOr[Boolean | String]          = js.undefined

  val hashSpy             : js.UndefOr[Boolean]                   = js.undefined
  val offset              : js.UndefOr[Int]                       = js.undefined
  val duration            : js.UndefOr[Int | js.Function1[js.Any, Int]] = js.undefined
  val delay               : js.UndefOr[Int]                       = js.undefined
  val isDynamic           : js.UndefOr[Boolean]                   = js.undefined
  val onSetActive         : js.UndefOr[js.Function1[js.Any, _]]   = js.undefined
  val onSetInactive       : js.UndefOr[js.Function1[js.Any, _]]   = js.undefined
  val ignoreCancelEvents  : js.UndefOr[Boolean]                   = js.undefined

}
