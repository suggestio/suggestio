package models.im

import io.suggest.model.EnumValue2Val

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 14:25
 * Description: Программы компрессии, используемые при сохранении картинок.
 */
object CompressModes extends Enumeration with EnumValue2Val {

  /** Экземпляр модели. */
  sealed abstract class Val(strId: String) extends super.Val(strId) {
    /** Используя  */
    def fromDpr(dpr: IDevPixelRatio): ImCompression
  }

  override type T = Val

  /** Компрессия фона. */
  val Bg: T = new Val("bg") {
    override def fromDpr(dpr: IDevPixelRatio): ImCompression = {
      dpr.bgCompression
    }
  }

  /** Компрессия для изображений переднего плана. */
  val Fg: T = new Val("fg") {
    override def fromDpr(dpr: IDevPixelRatio): ImCompression = {
      dpr.fgCompression
    }
  }

}
