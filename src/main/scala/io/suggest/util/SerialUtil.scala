package io.suggest.util

import cascading.tuple.Tuple
import cascading.tuple.hadoop.TupleSerialization
import cascading.tuple.hadoop.io.{TupleDeserializer, TupleSerializer}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 18:00
 * Description: Утиль для сериализации/десериализации данных.
 */
object SerialUtil {
  val TUPLE_SER_BUF_SZ_DFLT = 512

  /** Кешируем тут TupleSerialization. Возможно, это не безопасно. Можно юзать только для локальной работы. */
  val TS_DFLT = new TupleSerialization

  def getSerializer(ts: TupleSerialization = TS_DFLT)   = ts.getSerializer(classOf[Tuple]).asInstanceOf[TupleSerializer]
  def getDeserializer(ts: TupleSerialization = TS_DFLT) = ts.getDeserializer(classOf[Tuple]).asInstanceOf[TupleDeserializer]
  
  /** Сериализация кортежа. */
  def serializeTuple(t:Tuple, ts:TupleSerialization = TS_DFLT, buffSz0:Int = TUPLE_SER_BUF_SZ_DFLT): Array[Byte] = {
    val serializer = getSerializer(ts)
    // TODO Нужно как-то определять необходимый размер буфера исходя из наполнения кортежа.
    val baos = new ByteArrayOutputStream(buffSz0)
    serializer.open(baos)
    serializer.serialize(t)
    serializer.close()
    baos.toByteArray
  }

  
  /** Десериализация кортежа. На выходе - resultTuple */
  def deserializeTuple(data:Array[Byte], resultTuple:Tuple = new Tuple, ts:TupleSerialization = TS_DFLT): Tuple = {
    val deserializer = getDeserializer(ts)
    // Десериализовать данные
    val bais = new ByteArrayInputStream(data)
    deserializer.open(bais)
    deserializer.deserialize(resultTuple)
    deserializer.close()
    resultTuple
  }
  
}
