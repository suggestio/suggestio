package models.im

import io.suggest.model.geom.d2.MOrientation2d
import io.suggest.ym.model.common.MImgSizeT
import io.suggest.model.geom.d2.MOrientations2d._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.14 17:24
 * Description: Типы ориентации экранов клиентских устройств.
 * 2014.oct.10: Эта модель по сути нужна только для статистики.
 */

/** Можно подмешивать к MImgSizeT, чтобы можно было узнавать ориентацию картинки. */
trait ImgOrientationT extends MImgSizeT {

  /**
   * Ориентация экрана.
   * @return Экземпляр DevScreenOrientation.
   */
  def orientation: MOrientation2d = {
    if (isVertical) {
      Vertical
    } else if (isHorizontal) {
      Horizontal
    } else {
      Square
    }
  }

}
