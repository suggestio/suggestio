package models.im

import java.text.DecimalFormat

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.primo.IStrId
import io.suggest.util.logs.MacroLogsImpl
import org.im4java.core.IMOperation
import models._
import util.FormUtil

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:49
 * Description: Код для работы с трансформациями картинок ImageMagic в рамках query-string.
 * Что-то подобное было в зотонике, где список qs биндился в список im-действий над картинкой.
 */

object ImOp extends MacroLogsImpl with JavaTokenParsers {

  val SPLIT_ON_BRACKETS_RE = "[\\[\\]]+".r

  /** qsb для биндинга списка трансформаций картинки (последовательности ImOp). */
  implicit def imOpsSeqQsb = new ImOpsQsb

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


  /** Дефолтовый StringBuilder для unbind-методов. */
  def unbindSbDflt = new StringBuilder(64)

  /**
   * Для разных вариантов unbind'а используется этот метод
   * @param keyDotted Префикс ключа.
   * @param value Значения (im-операции).
   * @param withOrderInx Добавлять ли индексы операций? true если нужен будет вызов bind() над результатом.
   * @param sb Используемый для работы StrinbBuilder. По умолчанию - создавать новый.
   * @return Строка im-операций.
   */
  def unbindImOps(keyDotted: String, value: Seq[ImOp], withOrderInx: Boolean, sb: StringBuilder = unbindSbDflt): String = {
    unbindImOpsSb(keyDotted, value, withOrderInx, sb)
      .toString()
  }
  def unbindImOpsSb(keyDotted: String, value: Seq[ImOp], withOrderInx: Boolean, sb: StringBuilder = unbindSbDflt): StringBuilder = {
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
    sb
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
class ImOpsQsb extends QueryStringBindableImpl[Seq[ImOp]] {

  import LOGGER._

  /**
   * Забиндить список операций, ранее сериализованный в qs.
   * @param keyDotted Ключ-префикс вместе с точкой. Может быть и пустой строкой.
   * @param params ListMap с параметрами.
   */
  override def bind(keyDotted: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[ImOp]]] = {
    val splitOnBracketsRe = SPLIT_ON_BRACKETS_RE
    try {
      val ops0 = params
        .iterator
        // в карте params содержит также всякий посторонний мусор. Он нам неинтересен.
        .filter { _._1 startsWith keyDotted }
        // Извлечь порядковый номер из ключа.
        .flatMap { case (k, v) =>
          splitOnBracketsRe.split(k) match {
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
  def i18nValueCode: Option[String] = None
  def unwrappedValue: Option[String] = None
}


object ImOpCodes extends EnumMaybeWithName {

  abstract protected class Val(val strId: String)
    extends super.Val(strId)
      with IStrId
  {
    def mkOp(vs: Seq[String]): ImOp
  }

  override type T = Val

  val AbsCrop: T = new Val("a") {
    override def mkOp(vs: Seq[String]) = {
      AbsCropOp(ImgCrop(vs.head))
    }
  }
  val Gravity: T = new Val("b") {
    override def mkOp(vs: Seq[String]) = {
      ImGravities.withName(vs.head)
    }
  }
  val AbsResize: T = new Val("c") {
    override def mkOp(vs: Seq[String]): ImOp = {
      AbsResizeOp(vs.head)
    }
  }
  val Interlace: T = new Val("d") {
    override def mkOp(vs: Seq[String]) = {
      ImInterlace(vs)
    }
  }
  val GaussBlur: T = new Val("e") {
    override def mkOp(vs: Seq[String]) = {
      GaussBlurOp(vs.head.toDouble)
    }
  }
  val Quality: T = new Val("f") {
    override def mkOp(vs: Seq[String]): ImOp = {
      QualityOp(vs.head.toDouble)
    }
  }
  val Extent: T = new Val("g") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ExtentOp(vs.head)
    }
  }
  val Strip: T = new Val("h") {
    override def mkOp(vs: Seq[String]): ImOp = {
      StripOp
    }
  }
  val Filter: T = new Val("i") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImFilters(vs)
    }
  }
  val SamplingFactor: T = new Val("j") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImSamplingFactors.withName( vs.head )
    }
  }
  val PercentSzCrop: T = new Val("k") {
    override def mkOp(vs: Seq[String]): ImOp = {
      PercentSzCropOp(ImgCrop(vs.head))
    }
  }

  /** Не ясно, надо ли оверрайдить. Этот код написан до написания EnumMaybeWithName. */
  override def maybeWithName(n: String): Option[T] = {
    valuesT
      .find(_.strId == n)
  }

}



object ImGravities extends EnumMaybeWithName {

  protected case class Val(strId: String, imName: String) extends super.Val(strId) with ImOp with IStrId {
    override def opCode = ImOpCodes.Gravity
    override def addOperation(op: IMOperation): Unit = {
      op.gravity(imName)
    }
    override def qsValue: String = strId
    override def unwrappedValue = Some(imName)
  }

  override type T = Val

  // Некоторые значения помечены как lazy, т.к. не используются по факту.
  val Center: T         = Val("c", "Center")
  lazy val North: T     = Val("n", "North")
  lazy val South: T     = Val("s", "South")
  lazy val West: T      = Val("w", "West")
  lazy val East: T      = Val("e", "East")


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


object ImInterlace extends EnumMaybeWithName {

  protected case class Val(qsValue: String, imName: String) extends super.Val(qsValue) with ImOp {
    override def opCode = ImOpCodes.Interlace
    override def addOperation(op: IMOperation): Unit = {
      op.interlace(imName)
    }
    override def unwrappedValue: Option[String] = Some(imName)
  }

  override type T = Val

  val Plane: T            = Val("a", "Plane")
  lazy val None: T        = Val("0", "None")
  lazy val Line: T        = Val("l", "Line")
  lazy val Jpeg: T        = Val("j", "JPEG")
  lazy val Gif: T         = Val("g", "GIF")
  lazy val Png: T         = Val("p", "PNG")
  lazy val Partition: T   = Val("r", "Partition")

  def apply(vs: Seq[String]): T = apply(vs.head)
  def apply(v: String): T = withName(v)

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

