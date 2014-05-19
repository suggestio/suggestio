package util.blocks

import play.api.data.{FormError, Mapping}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 21:49
 * Description: Мелкие детальки конструктора, из которого собираются блоки.
 */

object BlocksCtorUtil {

  // Закинуть новый цвет в bind-аккамулятор.
  def mergeBindAccWithColor(maybeAcc: Either[Seq[FormError], BindAcc],
                            colorName: String,
                            maybeColor: Either[Seq[FormError], String]): Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeColor) match {
      case (a @ Right(acc0), Right(color)) =>
        acc0.colors ::= (colorName -> color)
        maybeAcc

      case (Left(accFE), Right(color)) =>
        maybeAcc

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

}


object BorderColor {
  val BF_NAME_DFLT = "borderColor"
  val DFLT = BfColor(BF_NAME_DFLT, defaultValue = Some("FFFFFF"))
}
trait BorderColor extends ValT {
  import BorderColor._
  def borderColorBf: BfColor = DFLT

  abstract override def blockFieldsRev: List[BlockFieldT] = borderColorBf :: super.blockFieldsRev

  // Mapping
  private def m = borderColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, borderColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val bf = borderColorBf
    val v = m.unbind( value.unapplyColor(bf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(borderColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}



object BgColor {
  val BF_NAME_DFLT = "bgColor"
}
trait BgColor extends ValT {
  import BgColor._
  def bgColorBf = BfColor(BF_NAME_DFLT)
  abstract override def blockFieldsRev: List[BlockFieldT] = bgColorBf :: super.blockFieldsRev

  // Mapping
  private def m = bgColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, bgColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val bf = bgColorBf
    val v = m.unbind( value.unapplyColor(bf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(bgColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object FillColor {
  val BF_NAME_DFLT = "fillColor"
}
trait FillColor extends ValT {
  import FillColor._
  def fillColorDefaultValue: Option[String] = None
  def fillColorBf = BfColor(BF_NAME_DFLT, defaultValue = fillColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = fillColorBf :: super.blockFieldsRev

  // Mapping
  private def m = fillColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, fillColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val bf = fillColorBf
    val v = m.unbind( value.unapplyColor(bf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(fillColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}



object DiscoBorderColor {
  val BF_NAME_DFLT = "discoBorderColor"
  val DBC_BF_VALUE_DFLT = Some("FFFFFF")
  val DBC_BF_DFLT = BfColor(BF_NAME_DFLT, defaultValue = DBC_BF_VALUE_DFLT)
}
trait DiscoBorderColorT extends ValT {
  def discoBorderColorBf: BfColor
  abstract override def blockFieldsRev: List[BlockFieldT] = discoBorderColorBf :: super.blockFieldsRev

  // Mapping
  private def m = discoBorderColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, discoBorderColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(discoBorderColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(discoBorderColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
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

  // Mapping
  private def m = discoIconColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, discoIconColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(discoIconColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(discoIconColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object CircleFillColor {
  val BF_NAME_DFLT = "circleFillColor"
}
trait CircleFillColor extends ValT {
  import CircleFillColor._
  def circleFillColorDefaultValue: Option[String] = None
  def circleFillColorBf = BfColor(BF_NAME_DFLT, defaultValue = circleFillColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = circleFillColorBf :: super.blockFieldsRev

  // Mapping
  private def m = circleFillColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, circleFillColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(circleFillColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(circleFillColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object MaskColor {
  val BF_NAME_DFLT = "maskColor"
}
trait MaskColor extends ValT {
  import MaskColor._
  def maskColorDefaultValue: Option[String] = None
  def maskColorBf = BfColor(BF_NAME_DFLT, defaultValue = maskColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = maskColorBf :: super.blockFieldsRev

  // Mapping
  private def m = maskColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, maskColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(maskColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(maskColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object SaleMaskColor {
  val BF_NAME_DFLT = "saleMaskColor"
}
trait SaleMaskColor extends ValT {
  import SaleMaskColor._
  def saleMaskColorDefaultValue: Option[String] = None
  def saleMaskColorBf = BfColor(BF_NAME_DFLT, defaultValue = saleMaskColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = saleMaskColorBf :: super.blockFieldsRev

  // Mapping
  private def m = saleMaskColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, saleMaskColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(saleMaskColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(saleMaskColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object TopColor {
  val BF_NAME_DFLT = "topColor"
}
trait TopColor extends ValT {
  import TopColor._
  def topColorDefaultValue: Option[String] = None
  def topColorBf = BfColor(BF_NAME_DFLT, defaultValue = topColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = topColorBf :: super.blockFieldsRev

  // Mapping
  private def m = topColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, topColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(topColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(topColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object BottomColor {
  val BF_NAME_DFLT = "bottomColor"
}
trait BottomColor extends ValT {
  import BottomColor._
  def bottomColorDefaultValue: Option[String] = None
  def bottomColorBf = BfColor(BF_NAME_DFLT, defaultValue = bottomColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = bottomColorBf :: super.blockFieldsRev

  // Mapping
  private def m = bottomColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, bottomColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(bottomColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(bottomColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object LineColor {
  val BF_NAME_DFLT = "lineColor"
}
trait LineColor extends ValT {
  import LineColor._
  def lineColorDefaultValue: Option[String] = None
  def lineColorBf = BfColor(BF_NAME_DFLT, defaultValue = lineColorDefaultValue)
  abstract override def blockFieldsRev: List[BlockFieldT] = lineColorBf :: super.blockFieldsRev

  // Mapping
  private def m = lineColorBf.getStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeColor = m.bind(data)
    BlocksCtorUtil.mergeBindAccWithColor(maybeAcc0, lineColorBf.name, maybeColor)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyColor(lineColorBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyColor(lineColorBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

