package io.suggest.dev

import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:48
 * Description: Парсеры для картинок.
 */
object PicSzParsers {

  val picSizeNumRe = "\\d{2,5}".r

  final val WH_DELIM = "x"
  val picSizeDelimRe = s"[${WH_DELIM.toLowerCase}${WH_DELIM.toUpperCase}]".r

  @inline
  final def IMG_RES_DPR_DELIM = HtmlConstants.COMMA

}


import io.suggest.dev.PicSzParsers._


/** Парсеры для размеров картинок. */
trait PicSzParsers extends JavaTokenParsers {

  def picSideP: Parser[Int] = {
    picSizeNumRe ^^ { _.toInt }
  }

  def whP: Parser[MSize2di] = {
    (picSideP ~ (picSizeDelimRe ~> picSideP))
      .map { case w ~ h => MSize2di(w, h) }
  }

}


/** Парсеры для плотности пикселей. */
trait DevicePixelRatioParsers extends JavaTokenParsers {

  def devPixRatioP: Parser[MPxRatio] = {
    decimalNumber ^^ { dnStr =>
      val dprVal = dnStr.toFloat
      MPxRatios.forRatio(dprVal)
    }
  }

}


/** Комбо-парсер для значения device screen. */
trait DevScreenParsers extends PicSzParsers with DevicePixelRatioParsers {

  def devScreenP: Parser[MScreen] = {
    val d = IMG_RES_DPR_DELIM
    (whP ~ (d ~> devPixRatioP)) ^^ {
      case wh ~ dpr =>
        MScreen(
          wh            = wh,
          pxRatio       = dpr
        )
    }
  }

}
final class DevScreenParsersImpl extends DevScreenParsers
