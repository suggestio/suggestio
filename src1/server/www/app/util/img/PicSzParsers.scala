package util.img

import models.im.{DevScreen, DevPixelRatios, DevPixelRatio}

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:48
 * Description: Парсеры для картинок.
 */
object PicSzParsers {

  val picSizeNumRe = "\\d{2,5}".r
  val picSizeDelimRe = "[xX]".r

  def IMG_RES_DPR_DELIM = ","
}


import PicSzParsers._


/** Парсеры для размеров картинок. */
trait PicSzParsers extends JavaTokenParsers {

  def picSideP: Parser[Int] = {
    picSizeNumRe ^^ { _.toInt }
  }

  def resolutionRawP: Parser[Int ~ Int] = {
    picSideP ~ (picSizeDelimRe ~> picSideP)
  }

}


/** Парсеры для плотности пикселей. */
trait DevicePixelRatioParsers extends JavaTokenParsers {

  def devPixRatioP: Parser[DevPixelRatio] = {
    decimalNumber ^^ { dnStr =>
      val dprVal = dnStr.toFloat
      DevPixelRatios.forRatio(dprVal)
    }
  }

}


/** Комбо-парсер для значения device screen. */
trait DevScreenParsers extends PicSzParsers with DevicePixelRatioParsers {

  def devScreenP: Parser[DevScreen] = {
    val d = IMG_RES_DPR_DELIM
    (resolutionRawP ~ opt(d ~> devPixRatioP)) ^^ {
      case w ~ h ~ dprOpt  =>  DevScreen(width = w, height = h, pixelRatioOpt = dprOpt)
    }
  }

}
