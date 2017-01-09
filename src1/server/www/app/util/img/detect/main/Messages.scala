package util.img.detect.main

import models.blk.AdColorFns.IMG_BG_COLOR_FN.{toString => IMG_BG_COLOR_FN}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.01.16 16:49
 * Description: akka-сообщения детектора.
 */

sealed trait ImgBgColorUpdateAction {
  def updateColors(colors: Map[String, String]): Map[String, String]
}

case object Keep extends ImgBgColorUpdateAction {
  override def updateColors(colors: Map[String, String]): Map[String, String] = {
    colors
  }
}

case class Update(newColorHex: String) extends ImgBgColorUpdateAction {
  override def updateColors(colors: Map[String, String]): Map[String, String] = {
    colors + (IMG_BG_COLOR_FN -> newColorHex)
  }
}

case object Remove extends ImgBgColorUpdateAction {
  override def updateColors(colors: Map[String, String]): Map[String, String] = {
    // TODO Opt проверять карту colors на наличие цвета фона?
    colors - IMG_BG_COLOR_FN
  }
}
