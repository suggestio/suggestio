package io.suggest.model

import HTapConversionsBasic._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import org.apache.hadoop.hbase.{HTableDescriptor, HColumnDescriptor}
import io.suggest.util.JMXBase

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 15:47
 * Description: Трейт с общим кодом для обобщенных моделей, обслуживающих конкретную HTable.
 */

object HTableModel {

  def cfDescSimple(name: String, maxVersions:Int) = {
    new HColumnDescriptor(name).setMaxVersions(maxVersions)
  }

}


trait HTableModel {

  def HTABLE_NAME: String

  def CFs: Seq[String]

  // TODO Тогда имя получается изменяемое. Это может быть небезопасно для якобы неизменяемого состояния.
  def HTABLE_NAME_BYTES: Array[Byte] = HTABLE_NAME.getBytes

  /** Существует ли указанная таблица? */
  def isTableExists: Future[Boolean] = {
    Future {
      val adm = SioHBaseSyncClient.admin
      try {
        adm.tableExists(HTABLE_NAME_BYTES)
      } finally {
        adm.close()
      }
    }
  }

  /** Генератор дескрипторов CFок. */
  def getColumnDescriptor: PartialFunction[String, HColumnDescriptor]


  /** Асинхронно создать таблицу. Полезно при первом запуске. Созданная таблица относится и к подчиненным моделям.
   * @return Пустой фьючерс, который исполняется при наступлении эффекта созданной таблицы.
   */
  def createTable: Future[_] = {
    val tableDesc = new HTableDescriptor(HTABLE_NAME)
    CFs foreach { cfName => tableDesc addFamily getColumnDescriptor(cfName) }
    Future {
      val adm = SioHBaseSyncClient.admin
      try {
        adm.createTable(tableDesc)
      } finally {
        adm.close()
      }
    }
  }


  /** Убедиться, что таблица существует.
   * TODO Сделать, чтобы был updateTable при необходимости (если схема таблицы слегка устарела).
   */
  def ensureTableExists: Future[_] = {
    isTableExists flatMap {
      case false => createTable
      case true  => Future successful None
    }
  }

  def dropTableSync() {
    val adm = SioHBaseSyncClient.admin
    adm.deleteTable(HTABLE_NAME)
  }

  def disableTableSync() {
    SioHBaseSyncClient.admin.disableTable(HTABLE_NAME)
  }

  def enableTableSync() {
    SioHBaseSyncClient.admin.enableTable(HTABLE_NAME)
  }

  def getTableDescriptorSync = {
    SioHBaseSyncClient.admin
      .getTableDescriptor(HTABLE_NAME)
  }
}


// Поддержка JMX для remote-управления конкретными моделями.

trait HBaseModelJMXBeanCommon {
  def ensureTableExists()
  def createTable()
  def isTableExists: Boolean
  def CFs: String
  def HTABLE_NAME: String
  def dropTable()
  def disableTable()
  def enableTable()
  def getTableDescriptor: String
}

trait JMXBaseHBase extends JMXBase {
  override def jmxName = "io.suggest.model:type=hbase,name=" + getClass.getSimpleName.replace("Jmx", "")
}

trait HBaseModelJMXBase extends JMXBaseHBase with HBaseModelJMXBeanCommon {

  def companion: HTableModel

  override def ensureTableExists() {
    awaitFuture(companion.ensureTableExists)
  }

  override def createTable() {
    awaitFuture(companion.createTable)
  }

  override def isTableExists: Boolean = {
    companion.isTableExists
  }

  override def CFs: String = {
    companion.CFs.mkString(", ")
  }

  override def HTABLE_NAME: String = companion.HTABLE_NAME

  override def dropTable() {
    companion.dropTableSync()
  }

  override def disableTable() {
    companion.disableTableSync()
  }

  override def enableTable() {
    companion.enableTableSync()
  }

  override def getTableDescriptor: String = {
    companion.getTableDescriptorSync.toString
  }
}



