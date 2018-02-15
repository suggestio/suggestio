package io.suggest.common.geom.d2

import java.awt.geom.RectangularShape

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, DocFieldTypes, FieldNumber}
import io.suggest.media.MediaConst.NamesShort

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 15:32
  * Description: Поддержка ES для кросс-платформенной модели MSize2di.
  */
object MSize2diJvm extends IGenEsMappingProps {

  // Имена полей короткие, заданы в MediaConst.NamesShort.

  private def _numberField(fn: String) = {
    FieldNumber(fn, fieldType = DocFieldTypes.integer, index = true, include_in_all = false)
  }

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = NamesShort
    List(
      _numberField( F.WIDTH_FN ),
      _numberField( F.HEIGHT_FN )
    )
  }

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

}
