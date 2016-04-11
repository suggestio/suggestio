package io.suggest.sjs.common.vm.attr

import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 11:02
  * Description: Аддоны для быстрого чтения-записи input.value с парсингом.
  */
trait InputValueT[VT] extends IVm {

  override type T <: HTMLInputElement

  def value: Option[VT] = {
    val v = _underlying.value
    if (v.isEmpty)
      None
    else
      Some( _parseInputValue(v) )
  }

  protected def _parseInputValue(v: String): VT

  def value_=(v: VT): Unit = {
    _underlying.value = v.toString
  }

}


trait DoubleInputValueT extends InputValueT[Double] {
  override protected def _parseInputValue(v: String): Double = {
    v.toDouble
  }
}


trait IntInputValueT extends InputValueT[Int] {
  override protected def _parseInputValue(v: String): Int = {
    v.toInt
  }
}


trait StringInputValueT extends InputValueT[String] {
  override protected def _parseInputValue(v: String): String = {
    v
  }
}


/** Расширение [[StringInputValueT]] для поддержки парсинга JSON. */
trait JsonStringInputValueT extends StringInputValueT with SjsLogger {

  /** Тип возвращаемого значения из JSON.parse(). Например js.Object. */
  type JsonVal_t <: js.Any

  /** Прочитать и распарсить строковое значение.
    * Если парсинг не удался, то будет исключение. */
  def valueJson: Option[JsonVal_t] = {
    try {
      for (v <- value) yield {
        JSON.parse(v)
          .asInstanceOf[JsonVal_t]
      }
    } catch {
      case ex: Throwable =>
        error(ErrorMsgs.JSON_PARSE_ERROR, ex)
        None
    }
  }

}
