package io.suggest.model

import scala.concurrent.{ExecutionContext, Future, future}
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor}
import org.apache.hadoop.hbase.client.{Get, Put}
import HTapConversionsBasic._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.13 11:52
 * Description: hbase-модель для domain-таблицы. Таблица включает в себя инфу по доменам, выраженную props CF и в других моделях:
 * - mdvi: Данные по индексам хранятся тут.
 */

object MDomain {

  val HTABLE_NAME = "domain"

  // Column family для хранения произвольных k-v колонок.
  val CF_PROPS      = "p".getBytes
  val CF_SEARCH_PTR = "sp".getBytes
  val CF_INX_ACTIVE = "a".getBytes

  /** Одноразовый клиент для этой таблицы. Нужно вызывать close() по завершению его работы.
   * @return Фьючерс с клиентом. Если пул клиентов исчерпан, то фьючерс будет исполнен через некоторое время.
   */
  def hclient(implicit executor: ExecutionContext) = SioHBaseSyncClient.clientForTable(HTABLE_NAME)


  /** Асинхронно создать таблицу. Полезно при первом запуске. Созданная таблица относится и к подчиненным моделям.
   * @return Пустой фьючерс, который исполняется при наступлении эффекта созданной таблицы.
   */
  def createTable(implicit executor:ExecutionContext): Future[Unit] = {
    val tableDesc = new HTableDescriptor(HTABLE_NAME)
    tableDesc.addFamily(MDVIActive.getCFDescriptor)    // MDVIActive
    tableDesc.addFamily(MDVISearchPtr.getCFDescriptor) // MDVISearchPtr
    // Props
    val propsCFDesc = new HColumnDescriptor(CF_PROPS).setMaxVersions(3)
    tableDesc.addFamily(propsCFDesc)
    future {
      SioHBaseSyncClient.admin.createTable(tableDesc)
    }
  }


  /** Выставить произвольнон значение для произвольной колонки в CF_PROPS. По идее должно использоваться из других моделей,
   * занимающихся сериализацией.
   * @param dkey ключ домена
   * @param key ключ
   * @param value сериализованное значение
   * @return Пустой фьючерс для опциональной синхронизации.
   */
  def setProp(dkey:String, key:String, value: Array[Byte])(implicit ec:ExecutionContext): Future[Unit] = {
    val putReq = new Put(dkey).add(CF_PROPS, key, value)
    hclient map { _client =>
      try {
        _client.put(putReq)
      } finally {
        _client.close()
      }
    }
  }


  /** Прочитать провертис, выставленный через setProp().
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @return Фьючерс с опциональным значением, если такое найдено.
   */
  def getProp(dkey: String, key: String)(implicit ec:ExecutionContext): Future[Option[Array[Byte]]] = {
    val column: Array[Byte] = key
    val getReq = new Get(dkey).addColumn(CF_PROPS, column)
    hclient map { _client =>
      try {
        val result = _client.get(getReq)
        if (result.isEmpty) {
          None
        } else {
          Some(result.getColumnLatest(CF_PROPS, column).getValue)
        }

      } finally {
        _client.close()
      }
    }
  }

}


// В трейт вынесен динамический функционал модели, чтобы легко использовать его в над-проектах.
trait MDomainT {
  def dkey: String

  def getDVIByVin(vin: String)(implicit ec: ExecutionContext) = MDVIActive.getForDkeyVin(dkey, vin)
  def getAllDVI(implicit ec: ExecutionContext) = MDVIActive.getAllForDkey(dkey)
  def getSearchPtr(idOpt:Option[String] = None)(implicit ec: ExecutionContext) = MDVISearchPtr.getForDkey(dkey)
  def getProp(key: String)(implicit ec: ExecutionContext) = MDomain.getProp(dkey, key)
  def setProp(key: String, value: Array[Byte])(implicit ec:ExecutionContext) = MDomain.setProp(dkey, key, value)
  def getDomainSettings(implicit ec: ExecutionContext)    = DomainSettings.getForDkey(dkey)
  def getAnyDomainSettings(implicit ec:ExecutionContext)  = DomainSettings.getAnyForDkey(dkey)
}

// ActiveRecord для использования в рамках sio.util.
case class MDomain(dkey: String) extends MDomainT

