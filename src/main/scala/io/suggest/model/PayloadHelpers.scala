package io.suggest.model

import cascading.tuple.coerce.Coercions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 18:32
 * Description: При работе с payload-полем необходимо уметь быстро упаковывать и распаковывать данные.
 * Тут хелперы, подмешиваемые в подклассы PayloadDatum'ов, которые позволяют более эффективно работать с базовыми
 * типами данных, нормальной поддержки которых так не хватает в [[com.scaleunlimited.cascading.PayloadDatum]].
 */
trait PayloadHelpers {
  def getPayloadValue(key: String): AnyRef
  def setPayloadValue(key: String, value: AnyRef)

  def getPayloadInt(key: String): Option[Int] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(INTEGER.coerce(v).intValue)
  }

  def getPayloadFloat(key: String): Option[Float] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(FLOAT.coerce(v).floatValue)
  }

  def getPayloadDouble(key: String): Option[Double] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(DOUBLE.coerce(v).doubleValue)
  }
  def getPayloadShort(key: String): Option[Short] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(SHORT.coerce(v).shortValue)
  }

  def getPayloadLong(key: String): Option[Long] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(LONG.coerce(v).longValue)
  }

  def getPayloadBoolean(key: String): Option[Boolean] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(BOOLEAN.coerce(v).booleanValue)
  }

  def getPayloadString(key: String): Option[String] = {
    val v = getPayloadValue(key)
    if (v == null)  None  else  Some(STRING.coerce(v))
  }

}
