package io.suggest.img

import io.suggest.img.crop.MCrop

import scala.util.parsing.combinator.JavaTokenParsers

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 17:04
  * Description: Парсинг сериализованных в строку [[MCrop]].
  */

object ImgCropParsers {

  /** Метод для парсинга offset-чисел, которые всегда знаковые. */
  def parseOffInt(offStr: String): Int = {
    // При URL_SAFE-кодировании используется _ вместо +. Этот символ нужно отбросить.
    val offStr1 = if (offStr.charAt(0) == '_') {
      offStr.substring(1)
    } else {
      offStr
    }
    Integer.parseInt(offStr1)
  }

}

/** Утиль для парсинга значений кропа. */
trait ImgCropParsers extends JavaTokenParsers {

  /** Сгенерить парсер, который будет заниматься десериализацией кропа. */
  def cropStrP: Parser[MCrop] = {
    val whP: Parser[Int] = "\\d+".r ^^ { Integer.parseInt }
    val offIntP: Parser[Int] = "[+-_]\\d+".r ^^ { ImgCropParsers.parseOffInt }
    (whP ~ ("[xX]".r ~> whP) ~ offIntP ~ offIntP) ^^ {
      case w ~ h ~ offX ~ offY =>
        MCrop(width=w, height=h, offX=offX, offY=offY)
    }
  }

}


class ImgCropParsersImpl extends ImgCropParsers {

  private def parseCropStr(cropStr: String) = parse(cropStrP, cropStr)

  def maybeApply(cropStr: String): Option[MCrop] = {
    val pr = parseCropStr(cropStr)
    if (pr.successful)
      Some(pr.get)
    else
      None
  }

  def apply(cropStr: String): MCrop = parseCropStr(cropStr).get

}
