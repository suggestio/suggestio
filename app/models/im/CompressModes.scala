package models.im

import io.suggest.model.EnumValue2Val
import util.FormUtil.StrEnumFormMappings

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 14:25
 * Description: Программы компрессии, используемые при сохранении картинок.
 */
object CompressModes extends Enumeration with EnumValue2Val with StrEnumFormMappings {

  /** Экземпляр модели. */
  sealed abstract class Val(val strId: String) extends super.Val(strId) {
    /** Извлечь компрессию из pxRatio согласно текущему режиму. */
    def fromDpr(dpr: IDevPixelRatio): ImCompression
    override def toString() = strId
    def nameI18n: String
  }

  override type T = Val

  /** Компрессия фона. */
  val Bg: T = new Val("bg") {
    override def fromDpr(dpr: IDevPixelRatio): ImCompression = {
      dpr.bgCompression
    }
    override def nameI18n = "Background"
  }

  /** Компрессия для изображений переднего плана. */
  val Fg: T = new Val("fg") {
    override def fromDpr(dpr: IDevPixelRatio): ImCompression = {
      dpr.fgCompression
    }
    override def nameI18n = "Foreground"
  }

  override protected def _idMaxLen: Int = 6
}
