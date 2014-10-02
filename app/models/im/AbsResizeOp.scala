package models.im

import io.suggest.ym.model.common.{MImgInfoMeta, MImgSizeT}
import org.im4java.core.IMOperation

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 14:16
 * Description: поддержка resize WxH[flag], например "1024x768c"
 */

object AbsResizeOp extends JavaTokenParsers {
  
  val numRe = "\\d{2,4}".r
  val flagRe = {
    val flags = ImResizeFlags.values
    val sb = new StringBuilder(flags.size + 6, "(?i)[")
    flags.foreach { flag =>
      sb append flag.urlSafeChar
    }
    sb.append("]").r
  }
  val whDelimRe = "[xX]".r

  def flagsP = rep(flagRe ^^ ImResizeFlags.maybeWithName) ^^ {
    _.flatten
  }

  def sizeP: Parser[MImgSizeT] = {
    val numP = numRe ^^ { _.toInt }
    (numP ~ (whDelimRe ~> numP)) ^^ {
      case width ~ height  =>  MImgInfoMeta(width = width, height = height)
    }
  }

  def aroParser: Parser[AbsResizeOp] = {
    (sizeP ~ flagsP) ^^ {
      case sz ~ flags  =>  AbsResizeOp(sz, flags = flags)
    }
  }

  def apply(raw: String): AbsResizeOp = {
    parse(aroParser, raw).get
  }

}


case class AbsResizeOp(sz: MImgSizeT, flags: Seq[ImResizeFlag] = Nil) extends ImOp {
  override def opCode = ImOpCodes.AbsResize

  override def addOperation(op: IMOperation): Unit = {
    if (flags.nonEmpty) {
      val flagsStr = flags.iterator.map(_.imChar).mkString("")
      op.resize(sz.width, sz.height, flagsStr)
    } else {
      op.resize(sz.width, sz.height)
    }
  }

  override def qsValue: String = {
    val sb = new StringBuilder(16)
    sb.append(sz.width)
      .append('x')
      .append(sz.height)
    flags.foreach { f =>
      sb append f.urlSafeChar
    }
    sb.toString()
  }
}




/** Флаги для ресайза. */
object ImResizeFlags extends Enumeration {
  protected case class Val(imChar: Char, urlSafeChar: Char) extends super.Val(urlSafeChar.toString)

  type ImResizeFlag = Val

  val IgnoreAspectRatio: ImResizeFlag   = Val('!', 'a')
  val OnlyShrinkLarger: ImResizeFlag    = Val('>', 'b')
  val OnlyEnlargeSmaller: ImResizeFlag  = Val('<', 'c')
  val FillArea: ImResizeFlag            = Val('^', 'd')
  // Другие режимы ресайза тут пока опущены, т.к. не подходят для AbsResize, а другой пока нет.

  implicit def value2val(x: Value): ImResizeFlag = x.asInstanceOf[ImResizeFlag]
  
  def maybeWithName(s: String): Option[ImResizeFlag] = {
    val ch = s.charAt(0)
    values
      .find { v =>
        val irf: ImResizeFlag = v
        irf.urlSafeChar == ch  ||  irf.imChar == ch
      }
      .asInstanceOf[Option[ImResizeFlag]]
  }

  override val values = super.values
}
