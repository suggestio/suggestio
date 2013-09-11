package io.suggest.model

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{HTablePool, HBaseAdmin}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.util.Bytes
import org.hbase.async.HBaseClient
import com.stumbleupon.async.{Callback, Deferred}
import scala.concurrent.{ExecutionContext, Promise, Future, future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.13 17:37
 * Description:
 */

object SioHBaseClient {
  val QUORUM_SPEC = "localhost"
}


// Официальный тормозной HBase-клиент, внешне полностью синхронный, несмотря на то, что асинхроннен внутри.
// Эталон неэффективности, неудобный в использовании, но всегда поддерживает нужную версию HBase.
trait SioHBaseSyncClientT {

  /**
   * Выдать пустую конфигурацию для открытия коннекшенов. Является долгоживущим объектом, что позволяет шарить
   * ресурсы HConnection и pool между разными задачами.
   * @return Дефолтовый экземпляр HBaseConfiguration.
   */
  val getConf = HBaseConfiguration.create()

  /**
   * Выдать готовый к работе admin-клиент. Одноразовый, т.к. по доке так рекомендовано.
   */
  def admin = new HBaseAdmin(getConf)

  val pool = new HTablePool(getConf, 16)

  /** Сгенерить простого (НЕ-admin) клиента для работы с указанной таблицей: можно читать и писать ряды в таблицу.
   * Клиент нужно закрывать по окончании работы.
   * @param tableName Имя откываемой таблицы.
   * @return Фьючерс с экземпляром табличного клиента. Клиент не является thread-safe и его нужно закрывать когда он
    *        более не нужен.
   */
  def clientForTable(tableName:String)(implicit executor: ExecutionContext) = future {
    pool.getTable(tableName)
  }
}
object SioHBaseSyncClient extends SioHBaseSyncClientT


// Будущий асинхронный клиент, на который надо будет переехать, когда его наконец запилят поддержку HBase 0.95.x
trait SioHBaseAsyncClientT {

  val asyncClient = new HBaseClient(SioHBaseClient.QUORUM_SPEC)

  /** Оборачивание asynchbase-фьючерса в скаловский фьючерс через два костыля.
   * @param d Экземпляр Deferred, который по сути есть фьючерс.
   * @tparam R Тип возвращаемого значения фьючерса.
   * @return Future[R], который подхватывает как успехи, так и фейлы.
   */
  implicit def deferred2future[R](d: Deferred[R]): Future[R] = {
    throw new NotImplementedError("Hbase 0.95 not yet supported by asynchbase. Please use synchronous client until.")
    val p = Promise[R]()
    d.addCallbacks(
      new Callback[R, R] {
        def call(arg: R): R = {
          p trySuccess arg
          arg
        }
      },
      new Callback[AnyRef, Throwable] {
        def call(arg: Throwable): AnyRef = {
          p tryFailure arg
          arg
        }
      }
    )
    p.future
  }

}


trait HTapConversionsBasicT {
  implicit def bytesToIbw(bytes: Array[Byte]) = new ImmutableBytesWritable(bytes)

  implicit def stringToBytes(s: String) = Bytes.toBytes(s)
  implicit def bytesToString(bytes: Array[Byte]) = Bytes.toString(bytes)
  implicit def ibwToString(ibw: ImmutableBytesWritable): String = ibw.get
  implicit def stringToIbw(s: String): ImmutableBytesWritable = stringToBytes(s)

  implicit def longToBytes(l: Long) = Bytes.toBytes(l)
  implicit def bytesToLong(bytes: Array[Byte]) = Bytes.toLong(bytes)
  implicit def longToIbw(l: Long): ImmutableBytesWritable = longToBytes(l)
  implicit def ibwToLong(ibw: ImmutableBytesWritable): Long = ibw.get
  implicit def juLongToIbw(l: java.lang.Long): ImmutableBytesWritable = l.longValue
  implicit def ibwToJuLong(ibw: ImmutableBytesWritable) = java.lang.Long.valueOf(ibwToLong(ibw))

  implicit def shortToBytes(s: Short) = Bytes.toBytes(s)
  implicit def bytesToShort(b: Array[Byte]) = Bytes.toShort(b)
  implicit def shortToIbw(s: Short): ImmutableBytesWritable = shortToBytes(s)
  implicit def ibwToShort(ibw: ImmutableBytesWritable): Short = ibw.get
  implicit def juShortToIbw(s: java.lang.Short): ImmutableBytesWritable = s.shortValue
  implicit def ibwToJuShort(ibw: ImmutableBytesWritable) = java.lang.Short.valueOf(ibwToShort(ibw))

  implicit def intToBytes(i: Int) = Bytes.toBytes(i)
  implicit def bytesToInt(b: Array[Byte]) = Bytes.toInt(b)
  implicit def intToIbw(i: Int): ImmutableBytesWritable = intToBytes(i)
  implicit def ibwToInt(ibw: ImmutableBytesWritable): Int = ibw.get
  implicit def juIntegerToIbw(i: java.lang.Integer): ImmutableBytesWritable = i.intValue
  implicit def ibwToJuInt(ibw: ImmutableBytesWritable) = java.lang.Integer.valueOf(ibwToInt(ibw))

  implicit def floatToBytes(f: Float) = Bytes.toBytes(f)
  implicit def bytesToFloat(b: Array[Byte]) = Bytes.toFloat(b)
  implicit def floatToIbw(f: Float): ImmutableBytesWritable = floatToBytes(f)
  implicit def ibwToFloat(ibw: ImmutableBytesWritable): Float = ibw.get
  implicit def juFloatToIbw(f: java.lang.Float): ImmutableBytesWritable = f.floatValue
  implicit def ibwToJuFloat(ibw: ImmutableBytesWritable) = java.lang.Float.valueOf(ibwToFloat(ibw))

  implicit def doubleToBytes(d: Double) = Bytes.toBytes(d)
  implicit def bytesToDouble(b: Array[Byte]) = Bytes.toDouble(b)
  implicit def doubleToIbw(d: Double): ImmutableBytesWritable = doubleToBytes(d)
  implicit def ibwToDouble(ibw: ImmutableBytesWritable): Double = ibw.get
  implicit def juDoubleToIbw(d: java.lang.Double): ImmutableBytesWritable = d.doubleValue
  implicit def ibwToJuDouble(ibw: ImmutableBytesWritable) = java.lang.Double.valueOf(ibwToDouble(ibw))

  implicit def boolToBytes(b: Boolean) = Bytes.toBytes(b)
  implicit def bytesToBool(b: Array[Byte]) = Bytes.toBoolean(b)
  implicit def boolToIbw(b: Boolean): ImmutableBytesWritable = boolToBytes(b)
  implicit def ibwToBool(ibw: ImmutableBytesWritable): Boolean = ibw.get
  implicit def juBooleanToIbw(b: java.lang.Boolean): ImmutableBytesWritable = b.booleanValue
  implicit def ibwToJuBool(ibw: ImmutableBytesWritable) = java.lang.Boolean.valueOf(ibwToBool(ibw))

  def any2bytes: PartialFunction[Any, Array[Byte]] = {
    case s: String  => s
    case l: java.lang.Long    => l.longValue
    case i: java.lang.Integer => i.intValue
    case s: java.lang.Short   => s.shortValue
    case d: java.lang.Double  => d.doubleValue
    case f: java.lang.Float   => f.floatValue
    case b: java.lang.Boolean => b.booleanValue
    case b: Array[Byte]       => b
    case ibw: ImmutableBytesWritable => ibw.getBytes
    // TODO Тут надо выпилить аккуратно поддержку примитивных типов данных, ибо компилятор только с референсными типами работает из java.lang.*. И заменить Any на AnyRef.
    case l: Long    => l
    case i: Int     => i
    case f: Float   => f
    case d: Double  => d
    case s: Short   => s
    case b: Boolean => b
    // Можно добавить ещё конверсий для Bytes.to*().
  }

  // AnyRef можно заменить на Any, но это нежелательно.
  def any2ibw(v: AnyRef): ImmutableBytesWritable = any2bytes(v)
}

object HTapConversionsBasic extends HTapConversionsBasicT