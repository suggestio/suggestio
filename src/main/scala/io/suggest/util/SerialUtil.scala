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
  
  /** Сериализация кортежа. */
  def serializeTuple(t:Tuple, ts:TupleSerialization = new TupleSerialization, buffSz0:Int = TUPLE_SER_BUF_SZ_DFLT): Array[Byte] = {
    val serializer = ts.getSerializer(classOf[Tuple]).asInstanceOf[TupleSerializer]
    // TODO Нужно как-то определять необходимый размер буфера исходя из наполнения кортежа.
    val baos = new ByteArrayOutputStream(buffSz0)
    serializer.open(baos)
    serializer.serialize(t)
    serializer.close()
    baos.toByteArray
  }

  
  /** Десериализация кортежа. На выходе - resultTuple */
  def deserializeTuple(data:Array[Byte], resultTuple:Tuple = new Tuple, ts:TupleSerialization = new TupleSerialization): Tuple = {
    val deserializer = ts.getDeserializer(classOf[Tuple]).asInstanceOf[TupleDeserializer]
    // Десериализовать данные
    val bais = new ByteArrayInputStream(data)
    deserializer.open(bais)
    deserializer.deserialize(resultTuple)
    deserializer.close()
    resultTuple
  }
  
}
