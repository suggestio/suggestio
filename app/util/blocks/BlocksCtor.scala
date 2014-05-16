package util.blocks

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 21:49
 * Description: Мелкие детальки конструктора, из которого собираются блоки.
 */

object BorderColor {
  val BF_NAME_DFLT = "borderColor"
}
trait BorderColor extends ValT {
  import BorderColor._
  def borderColorDefaultValue: Option[String] = None
  def borderColorBf = BfColor(BF_NAME_DFLT, defaultValue = borderColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = borderColorBf :: super.blockFieldsRev
}



object BgColor {
  val BF_NAME_DFLT = "bgColor"
}
trait BgColor extends ValT {
  import BgColor._
  def bgColorDefaultValue: Option[String] = None
  def bgColorBf = BfColor(BF_NAME_DFLT, defaultValue = bgColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = bgColorBf :: super.blockFieldsRev
}


object FillColor {
  val BF_NAME_DFLT = "fillColor"
}
trait FillColor extends ValT {
  import FillColor._
  def fillColorDefaultValue: Option[String] = None
  def fillColorBf = BfColor(BF_NAME_DFLT, defaultValue = fillColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = fillColorBf :: super.blockFieldsRev
}



object DiscoBorderColor {
  val BF_NAME_DFLT = "discoBorderColor"
  val DBC_BF_VALUE_DFLT = Some("FFFFFF")
  val DBC_BF_DFLT = BfColor(BF_NAME_DFLT, defaultValue = DBC_BF_VALUE_DFLT)
}
trait DiscoBorderColorT extends ValT {
  def discoBorderColorBf: BfColor
  abstract override def blockFieldsRev: List[BlockFieldT] = discoBorderColorBf :: super.blockFieldsRev
}
trait DiscoBorderColorStatic extends DiscoBorderColorT {
  override def discoBorderColorBf = DiscoBorderColor.DBC_BF_DFLT
}
trait DiscoBorderColor extends DiscoBorderColorT {
  import DiscoBorderColor._
  def discoBorderColorDefaultValue: Option[String] = DBC_BF_VALUE_DFLT
  override def discoBorderColorBf = BfColor(BF_NAME_DFLT, defaultValue = discoBorderColorDefaultValue)
}


object DiscoIconColor {
  val BF_NAME_DFLT = "discoIconColor"
}
trait DiscoIconColor extends ValT {
  import DiscoIconColor._
  def discoIconColorDefaultValue: Option[String] = None
  def discoIconColorBf = BfColor(BF_NAME_DFLT, defaultValue = discoIconColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = discoIconColorBf :: super.blockFieldsRev
}


object CircleFillColor {
  val BF_NAME_DFLT = "circleFillColor"
}
trait CircleFillColor extends ValT {
  import CircleFillColor._
  def circleFillColorDefaultValue: Option[String] = None
  def circleFillColorBf = BfColor(BF_NAME_DFLT, defaultValue = circleFillColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = circleFillColorBf :: super.blockFieldsRev
}


object MaskColor {
  val BF_NAME_DFLT = "maskColor"
}
trait MaskColor extends ValT {
  import MaskColor._
  def maskColorDefaultValue: Option[String] = None
  def maskColorBf = BfColor(BF_NAME_DFLT, defaultValue = maskColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = maskColorBf :: super.blockFieldsRev
}


object SaleMaskColor {
  val BF_NAME_DFLT = "saleMaskColor"
}
trait SaleMaskColor extends ValT {
  import SaleMaskColor._
  def saleMaskColorDefaultValue: Option[String] = None
  def saleMaskColorBf = BfColor(BF_NAME_DFLT, defaultValue = saleMaskColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = saleMaskColorBf :: super.blockFieldsRev
}


object TopColor {
  val BF_NAME_DFLT = "topColor"
}
trait TopColor extends ValT {
  import TopColor._
  def topColorDefaultValue: Option[String] = None
  def topColorBf = BfColor(BF_NAME_DFLT, defaultValue = topColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = topColorBf :: super.blockFieldsRev
}


object BottomColor {
  val BF_NAME_DFLT = "bottomColor"
}
trait BottomColor extends ValT {
  import BottomColor._
  def bottomColorDefaultValue: Option[String] = None
  def bottomColorBf = BfColor(BF_NAME_DFLT, defaultValue = bottomColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = bottomColorBf :: super.blockFieldsRev
}


object LineColor {
  val BF_NAME_DFLT = "lineColor"
}
trait LineColor extends ValT {
  import LineColor._
  def lineColorDefaultValue: Option[String] = None
  def lineColorBf = BfColor(BF_NAME_DFLT, defaultValue = lineColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = lineColorBf :: super.blockFieldsRev
}

