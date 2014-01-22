package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import com.scaleunlimited.cascading.BaseDatum
import cascading.tuple.{TupleEntry, Tuple, Fields}
import org.joda.time.Period

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 13:50
 * Description: Описание гарантии в виде короткого кортжа-контейнера.
 */
object YmWarranty extends CascadingFieldNamer {
  val HAS_WARRANTY_FN = fieldName("hasWarranty")
  val PERIOD_FN       = fieldName("period")

  val FIELDS = new Fields(HAS_WARRANTY_FN, PERIOD_FN)

  def serializePeriod(p: Option[Period]): String = {
    if (p.isDefined)  p.get.toString  else  null
  }
  val deserializePeriod: PartialFunction[String, Option[Period]] = {
    case null => None
    case s    => Some(new Period(s))
  }
}


import YmWarranty._
class YmWarranty extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  /**
   * Гарантия производителя на товар.
   * @param hasWarranty Есть ли гарантия вообще?
   * @param periodOpt Срок гарантии, если есть. Когда гарантии нет, то тут должно быть None.
   */
  def this(hasWarranty:Boolean, periodOpt: Option[Period] = None) = {
    this
    setWarranty(hasWarranty)
    setPeriod(periodOpt)
  }


  def hasWarranty: Boolean = _tupleEntry getBoolean HAS_WARRANTY_FN
  def setWarranty(hasWarranty: Boolean) = {
    _tupleEntry.setBoolean(HAS_WARRANTY_FN, hasWarranty)
    this
  }

  def getPeriod: Option[Period] = {
    val raw = _tupleEntry getString PERIOD_FN
    deserializePeriod(raw)
  }
  def setPeriod(p: Option[Period]) = {
    val str = serializePeriod(p)
    _tupleEntry.setString(PERIOD_FN, str)
    this
  }
}
