package models.madn

import enumeratum.{Enum, EnumEntry}
import io.suggest.color.{MColorData, MColors}

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 11:58
 * Description: Статическая модель с дефолтовыми цветами оформления новосоздаваемых узлов.
 */
object NodeDfltColors extends Enum[NodeDfltColor] {

  case object C1 extends NodeDfltColor {
    override def bgColorHex = "544d4d"
    override def fgColorHex = "f0eded"
  }

  case object C2 extends NodeDfltColor {
    override def bgColorHex = "774b5d"
    override def fgColorHex = "ffffff"
  }

  case object C3 extends NodeDfltColor {
    override def bgColorHex = "243446"
    override def fgColorHex = "c9dbc4"
  }

  case object C4 extends NodeDfltColor {
    override def bgColorHex = "2f353e"
    override def fgColorHex = "d5e0f8"
  }

  case object C5 extends NodeDfltColor {
    override def bgColorHex = "aabfb0"
    override def fgColorHex = "7e2d3e"
  }

  case object C6 extends NodeDfltColor {
    override def bgColorHex = "def6e8"
    override def fgColorHex = "454849"
  }

  case object C7 extends NodeDfltColor {
    override def bgColorHex = "cf5048"
    override def fgColorHex = "eefaf9"
  }

  case object C8 extends NodeDfltColor {
    override def bgColorHex = "b5b7b6"
    override def fgColorHex = "39445d"
  }

  case object C9 extends NodeDfltColor {
    override def bgColorHex = "e7e7e7"
    override def fgColorHex = "6c849e"
  }

  case object C10 extends NodeDfltColor {
    override def bgColorHex = "9f9295"
    override def fgColorHex = "952f48"
  }


  override val values = findValues

  /** Вернуть рандомный цвет. */
  def getOneRandom(rnd: Random = new Random()): NodeDfltColor = {
    val id = rnd.nextInt(values.size)
    values(id)
  }

}


/** Класс одного дефолтового цвета. */
sealed abstract class NodeDfltColor extends EnumEntry {

  def bgColorHex: String
  def fgColorHex: String

  /** Цвета для adn-узла. */
  final def adnColors: MColors = {
    MColors(
      bg = Some( MColorData(bgColorHex) ),
      fg = Some( MColorData(fgColorHex) )
    )
  }

}

