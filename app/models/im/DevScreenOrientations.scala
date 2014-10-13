package models.im

import io.suggest.ym.model.common.MImgSizeT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.14 17:24
 * Description: Типы ориентации экранов клиентских устройств.
 * 2014.oct.10: Эта модель по сути нужна только для статистики.
 */
object DevScreenOrientations extends Enumeration {

  protected case class Val(name: String) extends super.Val(name)

  type DevScreenOrientation = Val

  val VERTICAL: DevScreenOrientation    = Val("vert")
  val HORIZONTAL: DevScreenOrientation  = Val("horiz")
  val SQUARE: DevScreenOrientation      = Val("square")

}


import DevScreenOrientations._


/** Можно подмешивать к MImgSizeT, чтобы можно было узнавать ориентацию картинки. */
trait ImgOrientationT extends MImgSizeT {

  /**
   * Ориентация экрана.
   * @return Экземпляр DevScreenOrientation.
   */
  def orientation: DevScreenOrientation = {
    if (isVertical)
      VERTICAL
    else if (isHorizontal)
      HORIZONTAL
    else
      SQUARE
  }

}
