package io.suggest.model

import HTapConversionsBasic._
import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits._
import org.apache.hadoop.hbase.{HTableDescriptor, HColumnDescriptor}

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

  protected def CFs: Seq[String]

  // TODO Тогда имя получается изменяемое. Это может быть небезопасно для якобы неизменяемого состояния.
  def HTABLE_NAME_BYTES: Array[Byte] = HTABLE_NAME.getBytes

  /** Существует ли указанная таблица? */
  def isTableExists: Future[Boolean] = {
    future {
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
  def createTable: Future[Unit] = {
    val tableDesc = new HTableDescriptor(HTABLE_NAME)
    CFs foreach { cfName => tableDesc addFamily getColumnDescriptor(cfName) }
    future {
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
  def ensureTableExists: Future[Unit] = {
    isTableExists flatMap {
      case false => createTable
      case true  => Future.successful(())
    }
  }

}
