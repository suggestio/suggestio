package models.im

import java.text.DecimalFormat

import org.im4java.core.IMOperation
import models._
import play.api.mvc.QueryStringBindable
import play.core.parsers.FormUrlEncodedParser
import util.{FormUtil, PlayMacroLogsImpl}

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:49
 * Description: Код для работы с трансформациями картинок ImageMagic в рамках query-string.
 * Что-то подобное было в зотонике, где список qs биндился в список im-действий над картинкой.
 */

object ImOp extends PlayMacroLogsImpl with JavaTokenParsers {

  val SPLIT_ON_BRACKETS_RE = "[\\[\\]]+".r

  /** qsb для биндинга списка трансформаций картинки (последовательности ImOp). */
  implicit def qsbSeq = new ImOpsQsb

  def twoFracZeroesFormat: DecimalFormat = {
    val df = new DecimalFormat("0.00")
    qsDoubleFormat(df)
  }

  def optFracZeroesFormat: DecimalFormat = {
    val df = new DecimalFormat("0.##")
    qsDoubleFormat(df)
  }

  private def qsDoubleFormat(df: DecimalFormat): DecimalFormat = {
    val dcs = df.getDecimalFormatSymbols
    dcs.setDecimalSeparator('.')
    dcs.setMinusSign('-')
    df.setDecimalFormatSymbols(dcs)
    df
  }


  /**
   * Для разных вариантов unbind'а используется этот метод
   * @param keyDotted Префикс ключа.
   * @param value Значения (im-операции).
   * @param withOrderInx Добавлять ли индексы операций? true если нужен будет вызов bind() над результатом.
   * @return Строка im-операций.
   */
  def unbindImOps(keyDotted: String, value: Seq[ImOp], withOrderInx: Boolean): String = {
    val sb = new StringBuilder(value.size + 32)
    value
      // Порядковый номер нужен для восстановления порядка вызовов при bind'е.
      .iterator
      .zipWithIndex
      .foreach { case (imOp, i) =>
        sb.append(keyDotted)
          .append(imOp.opCode.strId)
        if (withOrderInx) {
          sb.append('[').append(i).append(']')
        }
        sb
          .append('=')
          .append(imOp.qsValue)
          .append('&')
      }
    // Убрать последний '&'
    if (value.nonEmpty)
      sb.setLength(sb.length - 1)
    sb.toString()
  }

  /** Забиндить распарсенное по итератору. */
  def bindImOps(keyDotted: String, rawOps: Iterator[(String, Seq[String])]): Iterator[ImOp] = {
    rawOps
      .flatMap { case (k, vs) =>
        val opCodeStr = k.substring(keyDotted.length)
        ImOpCodes.maybeWithName(opCodeStr)
          .map { _ -> vs }
      }
      // Попытаться сгенерить результат
      .map {
        case (opCode, vs)  =>  opCode.mkOp(vs)
      }
  }

  /** Забиндить исходную qs-строку, предварительно протокенизировав. */
  def bindImOps(keyDotted: String, raw: String): Iterator[ImOp] = {
    val iter0 = FormUtil.parseToPairs(raw)
      .map { case (k, v)  =>  k -> List(v) }
    bindImOps(keyDotted, iter0)
  }

}


import ImOp._


/** qsb-биндер. */
class ImOpsQsb extends QueryStringBindable[Seq[ImOp]] {

  import LOGGER._

  /**
   * Забиндить список операций, ранее сериализованный в qs.
   * @param keyDotted Ключ-префикс вместе с точкой. Может быть и пустой строкой.
   * @param params ListMap с параметрами.
   */
  override def bind(keyDotted: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[ImOp]]] = {
    try {
      val ops0 = params
        .iterator
        // в карте params содержит также всякий посторонний мусор. Он нам неинтересен.
        .filter { _._1 startsWith keyDotted }
        // Извлечь порядковый номер из ключа.
        .flatMap { case (k, v) =>
          SPLIT_ON_BRACKETS_RE.split(k) match {
            case Array(k2, iStr) =>
              val i = iStr.toInt
              Seq( ((k2, v), i) )
            case _ => Seq.empty
          }
        }
        // Восстановить исходный порядок.
        .toSeq
        .sortBy(_._2)
        // Распарсить команды.
        .iterator
        .map(_._1)
      val ops = ImOp.bindImOps(keyDotted, ops0)
        .toSeq
      if (ops.isEmpty) {
        None
      } else {
        Some(Right(ops))
      }
    } catch {
      case ex: Throwable =>
        // Should not happen, if unbind() written ok:
        error(s"Failed to parse IM Ops\n  keyDotted=$keyDotted:\n  params = ${params.mkString(", ")}", ex)
        Some(Left("Failed to parse query string."))
    }
  }

  override def unbind(keyDotted: String, value: Seq[ImOp]): String = {
    unbindImOps(keyDotted, value, withOrderInx = true)
  }

}


/** Действие, запрограммирование в аргументах. */
trait ImOp {
  def opCode: ImOpCode
  def addOperation(op: IMOperation)
  def qsValue: String
}


object ImOpCodes extends Enumeration {
  abstract protected class Val(val strId: String) extends super.Val(strId) {
    def mkOp(vs: Seq[String]): ImOp
  }

  type ImOpCode = Val

  val AbsCrop: ImOpCode = new Val("a") {
    override def mkOp(vs: Seq[String]) = {
      AbsCropOp(ImgCrop(vs.head))
    }
  }
  val Gravity: ImOpCode = new Val("b") {
    override def mkOp(vs: Seq[String]) = {
      ImGravities.withName(vs.head)
    }
  }
  val AbsResize: ImOpCode = new Val("c") {
    override def mkOp(vs: Seq[String]): ImOp = {
      AbsResizeOp(vs.head)
    }
  }
  val Interlace: ImOpCode = new Val("d") {
    override def mkOp(vs: Seq[String]) = {
      ImInterlace(vs)
    }
  }
  val GaussBlur: ImOpCode = new Val("e") {
    override def mkOp(vs: Seq[String]) = {
      GaussBlurOp(vs.head.toDouble)
    }
  }
  val Quality: ImOpCode = new Val("f") {
    override def mkOp(vs: Seq[String]): ImOp = {
      QualityOp(vs.head.toDouble)
    }
  }
  val Extent: ImOpCode = new Val("g") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ExtentOp(vs.head)
    }
  }
  val Strip: ImOpCode = new Val("h") {
    override def mkOp(vs: Seq[String]): ImOp = {
      StripOp
    }
  }
  val Filter: ImOpCode = new Val("i") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImFilters(vs)
    }
  }
  val SamplingFactor: ImOpCode = new Val("j") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImSamplingFactors.withName( vs.head )
    }
  }
  val RelSzCrop: ImOpCode = new Val("k") {
    override def mkOp(vs: Seq[String]): ImOp = {
      RelSzCropOp(ImgCrop(vs.head))
    }
  }


  implicit def value2val(x: Value): ImOpCode = x.asInstanceOf[ImOpCode]

  def maybeWithName(n: String): Option[ImOpCode] = {
    values
      .find(_.strId == n)
      .asInstanceOf[Option[ImOpCode]]
  }
}



object ImGravities extends Enumeration {

  protected case class Val(strId: String, imName: String) extends super.Val(strId) with ImOp {
    override def opCode = ImOpCodes.Gravity
    override def addOperation(op: IMOperation): Unit = {
      op.gravity(imName)
    }
    override def qsValue: String = strId
  }

  type ImGravity = Val

  // Некоторые значения помечены как lazy, т.к. не используются по факту.
  val Center: ImGravity         = Val("c", "Center")
  lazy val North: ImGravity     = Val("n", "North")
  lazy val South: ImGravity     = Val("s", "South")
  lazy val West: ImGravity      = Val("w", "West")
  lazy val East: ImGravity      = Val("e", "East")

  // TODO Добавить ещё?

  implicit def value2val(x: Value): ImGravity = x.asInstanceOf[ImGravity]

}


/** quality для результата. */
case class QualityOp(quality: Double) extends ImOp {
  override def opCode = ImOpCodes.Quality
  override def addOperation(op: IMOperation): Unit = {
    op.quality(quality)
  }
  override def qsValue: String = {
    optFracZeroesFormat
      .format(quality)
  }
}


/** strip для урезания картинки. */
case object StripOp extends ImOp {
  override def opCode = ImOpCodes.Strip
  override def qsValue = ""
  override def addOperation(op: IMOperation): Unit = {
    op.strip()
  }
}


object ImInterlace extends Enumeration {
  protected case class Val(qsValue: String, imName: String) extends super.Val(qsValue) with ImOp {
    override def opCode = ImOpCodes.Interlace
    override def addOperation(op: IMOperation): Unit = {
      op.interlace(imName)
    }
  }

  type ImInterlacing = Val

  val Plane: ImInterlacing            = Val("a", "Plane")
  lazy val None: ImInterlacing        = Val("0", "None")
  lazy val Line: ImInterlacing        = Val("l", "Line")
  lazy val Jpeg: ImInterlacing        = Val("j", "JPEG")
  lazy val Gif: ImInterlacing         = Val("g", "GIF")
  lazy val Png: ImInterlacing         = Val("p", "PNG")
  lazy val Partition: ImInterlacing   = Val("r", "Partition")

  implicit def value2val(x: Value): ImInterlacing = x.asInstanceOf[ImInterlacing]

  def apply(vs: Seq[String]): ImInterlacing = apply(vs.head)
  def apply(v: String): ImInterlacing = withName(v)
}



/** Размывка по гауссу. */
case class GaussBlurOp(blur: Double) extends ImOp {
  override def opCode = ImOpCodes.GaussBlur
  override def addOperation(op: IMOperation): Unit = {
    op.gaussianBlur(blur)
  }
  override def qsValue: String = {
    twoFracZeroesFormat
      .format(blur)
  }
}

