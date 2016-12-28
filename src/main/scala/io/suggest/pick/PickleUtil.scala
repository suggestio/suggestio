package io.suggest.pick

import java.nio.ByteBuffer

import boopickle.Default._
import io.suggest.bin.{IConvCodec, IDataConv}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 16:55
  * Description: Утиль для клиент-серверной сериализации и десериализации (pickle/unpickle).
  */
object PickleUtil {

  /** Имя поля с какими-то сериализованными данными.
    * Например, если надо внутри JSON вставить base64-строку. */
  final val PICKED_FN = "_"


  def pickle[From](v: From)(implicit u: Pickler[From]): ByteBuffer = {
    Pickle.intoBytes(v)
  }

  def unpickle[To](bbuf: ByteBuffer)(implicit u: Pickler[To]): To = {
    Unpickle(u).fromBytes(bbuf)
  }


  /** Бывает, что нужно совместить сериализацию и конвертацию. Например, требуется выхлоп в base64.
    * Тогда можно задействовать эту функцию, импортировав implicit-реализацию конвертера в scope.
    * @param v Сериализуемая модель.
    * @tparam V Тип сериализуемых данных.
    * @tparam Codec Маркер-тип используемого алгоритма кодирования.
    * @tparam Encoded Тип возвращаемого значения.
    * @return Сериализованный и закодированный выхлоп.
    */
  def pickleConv[V, Codec <: IConvCodec, Encoded](v: V)
                                                 (implicit u: Pickler[V], dataConv: IDataConv[ByteBuffer, Codec, Encoded]): Encoded = {
    val bbuf = pickle[V](v)
    dataConv.convert(bbuf)
  }


  /**
    * Совмещение декодирования из произвольного формата данных и десериализации байтов в некую модель.
    * @param encoded Закодированные данные.
    * @param u pickler.
    * @param dataConv Декодер исходных закодированных данных.
    * @tparam Encoded Тип закодированных данных.
    * @tparam Codec Маркер-тип используемого алгоритма кодирования.
    * @tparam V Тип возвращаемого значения.
    * @return Десериализованное значение типа V.
    */
  def unpickleConv[Encoded, Codec <: IConvCodec, V](encoded: Encoded)
                                                   (implicit u: Pickler[V], dataConv: IDataConv[Encoded, Codec, ByteBuffer]): V = {
    val bbuf = dataConv.convert(encoded)
    unpickle(bbuf)
  }

}
