package io.suggest.model

import java.nio.ByteBuffer
import java.util.UUID

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.utils.Bytes
import com.websudos.phantom.query.{TruncateQuery, SelectCountQuery, CreateQuery}
import com.websudos.phantom.Implicits._
import io.suggest.util.MyConfig.CONFIG
import io.suggest.util.{MacroLogsImplLazy, JMXBase, UuidUtil}
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import scala.concurrent.Future
import io.suggest.util.SioFutureUtil.guavaFuture2scalaFuture

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 17:21
 * Description: Клиент для cassandra-кластера.
 */
object SioCassandraClient extends MacroLogsImplLazy {

  import LOGGER._

  /** Название кейспейса, который используется в проекте. */
  val SIO_KEYSPACE = CONFIG.getString("cassandra.keyspace.sio.name") getOrElse "Sio"

  /** Ноды, на которые надо долбиться при старте. Это seed-ноды, они сообщат весь список кластера. */
  def CONTACT_NODES: Seq[String] = CONFIG.getStringList("cassandra.cluster.nodes.connect.on.start").map(_.toSeq) getOrElse Seq("localhost")

  /** Можно ли менять keyspace? Через JMX например. [false], если иное явно не указано в конфиге. */
  def CAN_ALTER_KEYSPACE: Boolean = CONFIG.getBoolean("cassandra.session.keyspace.can_alter") getOrElse false

  /** Настройка репликации для создаваемого кейспейса. */
  def SIO_KEYSPACE_REPLICATION: String = CONFIG.getString("cassandra.keyspace.sio.replication") getOrElse "{'class': 'SimpleStrategy', 'replication_factor': 1}"

  /** Собираем инфу по кластеру. */
  val cluster = synchronized {
    Cluster.builder()
      .addContactPoints(CONTACT_NODES: _*)
      .build()
  }

  /** Открываем прочный канал связи с кластером. */
  implicit val session = synchronized {
    cluster.newSession()
  }

  /** Создать пространство ключей под нужды s.io. */
  def createSioKeyspace = {
    val cmd = "CREATE KEYSPACE " + SIO_KEYSPACE +
      " WITH REPLICATION = " + SIO_KEYSPACE_REPLICATION +
      " AND DURABLE_WRITES = false;"
    info(s"Creating keyspace '$SIO_KEYSPACE'...\n $cmd")
    session.executeAsync(cmd)
  }

  private def _useKeyspaceSync(ks: String) = session.execute(s"USE $ks;")

  /** Выставить клиенту используемый keyspace. */
  def useKeyspaceSync(ks: String) = {
    if (!CAN_ALTER_KEYSPACE)
      throw new IllegalAccessException("can_alter_keyspace not explicitly allowed in app config.")
    _useKeyspaceSync(ks)
  }

  // При старте надо выставить keyspace как дефолтовый. В нём живут все модели.
  try {
    _useKeyspaceSync(SIO_KEYSPACE)
  } catch {
    case ex: Throwable =>
      error(s"Suppressing failure to connect to keyspace $SIO_KEYSPACE; Not created? Use JMX console to create it.", ex)
  }
}


trait CassandraJmxBase extends JMXBase {
  override def jmxName = "io.suggest.model:type=cassandra,name=" + getClass.getSimpleName.replace("Jmx", "")
}


trait SioCassandraClientJmxMBean {
  def createSioKeyspace: String
  def useKeyspace(ks: String): String
}
class SioCassandraClientJmx extends CassandraJmxBase with SioCassandraClientJmxMBean {

  override def createSioKeyspace: String = {
    val fut = SioCassandraClient.createSioKeyspace.map { _.toString }
    awaitString(fut)
  }

  override def useKeyspace(ks: String): String = {
    val ks1 = if (ks.isEmpty) {
      SioCassandraClient.SIO_KEYSPACE
    } else {
      ks
    }
    SioCassandraClient.useKeyspaceSync(ks1).toString
  }
}


import SioCassandraClient.session

/** Непараметризованный интерфейс static-моделей. */
trait ICassandraStaticModel {
  def tableName: String

  def createTable: Future[ResultSet]
  def countAll: Future[Long]
  def truncateTable: Future[ResultSet]
}


/** Common-код в static-моделях. */
trait CassandraStaticModel[T <: CassandraTable[T,R], R] extends ICassandraStaticModel {

  def create: CreateQuery[T, R]
  def createTable = create.future()

  def count: SelectCountQuery[T, Long]
  def countAll = count.one.map(_ getOrElse 0L)

  def truncate: TruncateQuery[T, R]
  def truncateTable = truncate.future()

}


/** Добавить idStr() для получения строкового представления поля id. */
trait UuidIdStr {
  def id: UUID
  def idStr = UuidUtil.uuidToBase64(id)
}


/** Реализация ImgWithTimestamp для моделей. */
trait CassandraImgWithTimestamp extends ImgWithTimestamp {
  def timestamp: DateTime
  def img: ByteBuffer

  override def imgBytes: Array[Byte] = Bytes.getArray(img)
  override def timestampMs: Long = timestamp.getMillis
}


/** Общий JMX-интерфейс для cassandra-моделей. */
trait CassandraModelJxmMBeanI {

  def getTableName: String

  def createTable: String

  def countAll: String

  def truncateTable: String
}


trait DeleteByStrId {
  def deleteById(id: UUID): Future[ResultSet]
  def deleteByStrId(id: String) = deleteById(UuidUtil.base64ToUuid(id))
}


/** Реализация общих JXM-методов cassandra-моделей. */
trait CassandraModelJmxMBeanImpl extends CassandraJmxBase with CassandraModelJxmMBeanI {

  def companion: ICassandraStaticModel


  override def getTableName: String = companion.tableName

  override def createTable: String = {
    val fut = companion.createTable.map(_.toString)
    awaitString(fut)
  }

  override def truncateTable: String = {
    val fut = companion.truncateTable.map(_.toString)
    awaitString(fut)
  }

  override def countAll: String = {
    val fut = companion.countAll
      .map { _.toString }
    awaitString(fut)
  }

}

