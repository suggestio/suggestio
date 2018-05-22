package models.im

import enumeratum.{Enum, EnumEntry}
import io.suggest.common.geom.d2.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 12:27
 * Description: Базовые размеры экранов.
 * @see [[http://en.wikipedia.org/wiki/Comparison_of_Android_devices]].
 */
case object BasicScreenSizes extends Enum[BasicScreenSize] {

  // 5:3 (~1.6)
  case object FWQVGA_V extends BasicScreenSize {
    override def height = 400
    override def width  = 240
  }
  case object FWQVGA_H extends BasicScreenSize {
    override def width = 400
    override def height = 240
  }

  // 16:9 (~1.7)
  case object NHD_V extends BasicScreenSize {
    override def width = 360
    override def height = 640
  }
  case object NHD_H extends BasicScreenSize {
    override def width = 640
    override def height = 360
  }

  // 16:9
  case object WVGA_V extends BasicScreenSize {
    override def width = 480
    override def height = 800
  }
  case object WVGA_H extends BasicScreenSize {
    override def width = 800
    override def height = 480
  }

  // 16:10 (1.6), близкое к WSVGA, но безымянное разрешение.
  case object WSVGA_V extends BasicScreenSize {
    override def width = 640
    override def height = 1024
  }
  case object WSVGA_H extends BasicScreenSize {
    override def width = 1024
    override def height = 640
  }

  // 16:10
  case object WXGA_V extends BasicScreenSize {
    override def width = 800
    override def height = 1280
  }
  case object WXGA_H extends BasicScreenSize {
    override def width = 1280
    override def height = 800
  }

  // 16:10
  case object WXGAPLUS_V extends BasicScreenSize {
    override def width = 900
    override def height = 1440
  }
  case object WXGAPLUS_H extends BasicScreenSize {
    override def width = 1440
    override def height = 900
  }

  // 16:9
  case object FHD_V extends BasicScreenSize {
    override def width = 1080
    override def height = 1920
  }
  case object FHD_H extends BasicScreenSize {
    override def width = 1920
    override def height = 1080
  }

  // 16:9
  case object QWXGA_V extends BasicScreenSize {
    override def width = 1152
    override def height = 2048
  }
  case object QWXGA_H extends BasicScreenSize {
    override def width = 2048
    override def height = 1152
  }

  // 16:10
  case object WUXGA_V extends BasicScreenSize {
    override def width = 1200
    override def height = 1920
  }
  case object WUXGA_H extends BasicScreenSize {
    override def width = 1920
    override def height = 1200
  }

  // 16:9
  case object QHD_V extends BasicScreenSize {
    override def width = 1440
    override def height = 2560
  }
  case object QHD_H extends BasicScreenSize {
    override def width = 2569
    override def height = 1440
  }

  // 16:10
  case object WQXGA_V extends BasicScreenSize {
    override def width = 1600
    override def height = 2560
  }
  case object WQXGA_H extends BasicScreenSize {
    override def width = 2560
    override def height = 1600
  }


  override val values = findValues

  /** Найти подходящее разрешение, если есть.
    * (Найти базовое разрешение окна по модели)
    */
  def includesSize(sz: ISize2di): Option[BasicScreenSize] = {
    values
      .find { ISize2di.isIncudesSz(_, sz) }
  }

}


sealed abstract class BasicScreenSize extends EnumEntry with ISize2di {
  override def toString = s"${width}x$height"
}
