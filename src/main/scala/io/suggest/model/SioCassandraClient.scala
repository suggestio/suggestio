package io.suggest.model

import java.util.UUID

import com.datastax.driver.core.Cluster
import com.websudos.phantom.query.{TruncateQuery, SelectCountQuery, CreateQuery}
import com.websudos.phantom.Implicits._
import io.suggest.util.MyConfig.CONFIG
import io.suggest.util.{JMXBase, UuidUtil}
import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 17:21
 * Description: Клиент для cassandra-кластера.
 */
object SioCassandraClient {

  /** Название кейспейса, который используется в проекте. */
  val SIO_KEYSPACE = "Sio"

  /** Ноды, на которые надо долбиться при старте. Это seed-ноды, они сообщат весь список кластера. */
  def CONTRACT_NODES: Seq[String] = CONFIG.getStringList("cassandra.cluster.nodes.connect.on.start").map(_.toSeq) getOrElse Seq("localhost")

  /** Можно ли менять keyspace? Через JMX например. [false], если иное явно не указано в конфиге. */
  def CAN_ALTER_KEYSPACE: Boolean = CONFIG.getBoolean("cassandra.session.keyspace.can_alter") getOrElse false

  /** Собираем инфу по кластеру. */
  val cluster = synchronized {
    Cluster.builder()
      .addContactPoints(CONTRACT_NODES: _*)
      .build()
  }

  /** Открываем прочный канал связи с кластером. */
  implicit val session = synchronized {
    cluster.newSession()
  }

  /** Создать пространство ключей под нужды s.io. */
  def createSioKeyspace = {
    session.executeAsync(s"CREATE KEYSPACE $SIO_KEYSPACE " +
      s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1}" +
      s"AND DURABLE_WRITES = false;")
  }

  private def _useKeyspaceSync(ks: String) = session.execute(s"USE $ks;")

  /** Выставить клиенту используемый keyspace. */
  def useKeyspaceSync(ks: String) = {
    if (!CAN_ALTER_KEYSPACE)
      throw new IllegalAccessException("can_alter_keyspace not explicitly allowed in app config.")
    _useKeyspaceSync(ks)
  }

  // При старте надо выставить keyspace как дефолтовый. В нём живут все модели.
  _useKeyspaceSync(SIO_KEYSPACE)
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
    SioCassandraClient.createSioKeyspace.get().toString
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


/** Общий JMX-интерфейс для cassandra-моделей. */
trait CassandraModelJxmMBeanI {

  def getTableName: String

  def createTable: String

  def countAll: Long

  def truncateTable: String
}


/** Реализация общих JXM-методов cassandra-моделей. */
trait CassandraModelJmxMBeanImpl extends CassandraJmxBase with CassandraModelJxmMBeanI {

  def companion: ICassandraStaticModel


  override def getTableName: String = companion.tableName

  override def createTable: String = {
    companion.createTable.map(_.toString)
  }

  override def truncateTable: String = {
    companion.truncateTable.map(_.toString)
  }

  override def countAll: Long = {
    companion.countAll
  }

}

