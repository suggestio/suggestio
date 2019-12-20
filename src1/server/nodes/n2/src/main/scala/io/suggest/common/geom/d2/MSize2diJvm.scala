package io.suggest.common.geom.d2

import java.awt.geom.RectangularShape

import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 15:32
  * Description: Поддержка ES для кросс-платформенной модели MSize2di.
  */
object MSize2diJvm {

  object Implicits {

    /** JVM-only утиль для связывания [[MSize2di]] с jvm-only-моделями. */
    implicit class AwtRectangle2dExtOps(val rect2d: RectangularShape) extends AnyVal {

      /** Приведение awt-прямоугольников к [[MSize2di]], попутно скругляя размеры. */
      def toSize2di: MSize2di = {
        MSize2di(
          width  = rect2d.getWidth.toInt,
          height = rect2d.getHeight.toInt
        )
      }

    }

  }


  /** Form-маппинг для SysImgMake. Код вынесен из BlockMetaJvm. */
  def formMapping: Mapping[MSize2di] = {
    import play.api.data.Forms._
    val sizeMapping = number(1, 5000)
    mapping(
      "width"   -> sizeMapping,
      "height"  -> sizeMapping,
    )
    { MSize2di.apply }
    { MSize2di.unapply }
  }

}
