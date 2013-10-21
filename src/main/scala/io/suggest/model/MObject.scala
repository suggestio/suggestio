package io.suggest.model

import scala.concurrent.{ExecutionContext, Future, future}
import org.apache.hadoop.hbase.HColumnDescriptor
import HTapConversionsBasic._
import io.suggest.util.SioConstants.DOMAIN_QI_TTL_SECONDS
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.JavaConversions._
import org.hbase.async.{DeleteRequest, KeyValue, GetRequest, PutRequest}
import SioHBaseAsyncClient._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.13 10:37
 * Description: Модель мелких объектов suggest.io с random-чтением и записью: доменов, юзеров, новостей и всего остального.
 * Мелкие немногочисленные бессвязные вещи хранятся в одной таблице в разных CF-ках чтобы избежать передозировки
 * неэффективно используемых регионов и сопутствуюих проблем.
 */


// Бывшая MDomain. Используется для хранения мелких объектов типа метаданным по объектам, юзерам и т.д.
// Для разграничения используются CF'ки.
object MObject extends HTableModel {
  val HTABLE_NAME = "obj"

  // Column family для хранения произвольных k-v колонок для доменов (ключ - dkey). В значениях идём по алфавиту.
  val CF_DPROPS      = "a".getBytes   // Пропертисы всякие. js-install и reindex info в частности.
  val CF_DINX_ACTIVE = "b".getBytes
  val CF_DSEARCH_PTR = "c".getBytes
  // Для моделей веб-морды:
  val CF_DDATA       = "d".getBytes   // DomainData - сохранение json'а настроек (веб-морда)
  val CF_DPUBLISH    = "e".getBytes   // Отметки о публикации доменов в галерее.
  val CF_DQI         = "f".getBytes   // QI-отметки, не привязаные к юзеру. Имеют ttl.

  // Инфа по юзерам. Access pattern - веб-морда.
  val CF_UPROPS      = "g".getBytes   // Юзеры. Тут например хранится предпочтительный язык UI и прочие вещи. Аналог DPROPS.
  val CF_UAUTHZ      = "h".getBytes   // Данные по авторизациям юзеров в доменах.

  // Другое (веб-морда).
  val CF_BLOG        = "i".getBytes   // Блог-записи/новости (веб-морда, ключ = некий id).
  val CF_DOMAIN      = "j".getBytes   // Записи MDomain.

  //val CF_DKNOWLEDGE  = ???.getBytes   // Записи MDomainKnowledge, заполняемые кравлером. qualifier является ключом, а value - значением.

  // /!\ При добавлении новых CF-записей нужно также обновлять/запиливать функции createTable() и updateTable().
  def CFs = Seq(
    CF_DPROPS, CF_DINX_ACTIVE, CF_DSEARCH_PTR,
    CF_DDATA, CF_DPUBLISH, CF_DQI,
    CF_UPROPS, CF_UAUTHZ ,
    CF_BLOG, CF_DOMAIN
  )

  def CFs_CRAWLER = Seq(CF_DOMAIN, CF_DPROPS, CF_DINX_ACTIVE, CF_DSEARCH_PTR)


  def getColumnDescriptor(cf: Array[Byte]): HColumnDescriptor = {
    cf match {
      case CF_DPROPS      => hcd(cf, 2)
      case CF_DINX_ACTIVE => MDVIActive.getCFDescriptor
      case CF_DSEARCH_PTR => MDVISearchPtr.getCFDescriptor
      case CF_DDATA       => hcd(cf, 2)
      case CF_DPUBLISH    => hcd(cf, 1)
      case CF_DQI         => hcd(cf, 1).setTimeToLive(DOMAIN_QI_TTL_SECONDS)
      case CF_UPROPS      => hcd(cf, 1)
      case CF_UAUTHZ      => hcd(cf, 2)
      case CF_BLOG        => hcd(cf, 1)
      case CF_DOMAIN      => hcd(cf, 2)
    }
  }

  private def hcd(cf:Array[Byte], maxVsn:Int) = HTableModel.cfDescSimple(cf, maxVsn)


  /** Выставить произвольнон значение для произвольной колонки в CF_PROPS. По идее должно использоваться из других моделей,
   * занимающихся сериализацией.
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @param value сериализованное значение
   * @return Фьючерс для опциональной синхронизации. Любые данные внутри возвращаемого фьючерса не имеют смысла.
   */
  def setProp(dkey:String, key:String, value: Array[Byte]): Future[AnyRef] = {
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], dkey:Array[Byte], CF_DPROPS, key:Array[Byte], value)
    ahclient.put(putReq)
  }


  /** Прочитать провертис, выставленный через setProp().
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @return Фьючерс с опциональным значением, если такое найдено.
   */
  def getProp(dkey: String, key: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    val column: Array[Byte] = key
    val getReq = new GetRequest(HTABLE_NAME, dkey) family CF_DPROPS qualifier column
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        Some(results.head.value)
      }
    }
  }


  /** Пересоздать CF-ки, относящиеся к кравлингу (вычистив тем самым их от данных). Обычно используется при reset'е
    * кравлера при девелопменте/отладке.
    */
  def clearCrawlerCFs = {
    import SioHBaseAsyncClient._
    Future.traverse(CFs_CRAWLER) { cfName =>
      val scanner = ahclient.newScanner(HTABLE_NAME_BYTES)
      scanner.setFamily(cfName)
      val folder = new AsyncHbaseScannerFold[Future[AnyRef]] {
        def fold(acc0: Future[AnyRef], kv: KeyValue): Future[AnyRef] = {
          acc0 andThen { case _ =>
            val delReq = new DeleteRequest(HTABLE_NAME_BYTES, kv.key, cfName)
            ahclient.delete(delReq)
          }
        }
      }
      folder(Future.successful(None), scanner)
        .flatMap(identity)  // Это типа Future.flatten()
    }
  }

  /**
   * Пересоздать указанные. Кривой код, чисто для девелопмента. Опривачен, ибо не нужен пока что.
   * @param cfs Названия CF'ок этой модели.
   * @return Фьючерс для синхронизации исполнения задачи.
   */
  private def recreateCFs(cfs: Seq[Array[Byte]]): Future[Unit] = future {
    val adm = SioHBaseSyncClient.admin
    try {
      adm.disableTable(HTABLE_NAME_BYTES)
      try {
        // удалить CF-ки, которые подлежат
        val desc0 = adm.getTableDescriptor(HTABLE_NAME_BYTES)
        cfs foreach desc0.removeFamily
        adm.modifyTable(HTABLE_NAME_BYTES, desc0)

        // modifyTable - asynchronous operation.
        Thread.sleep(1000)

        // Разудалить CF'ки.
        val desc1 = adm.getTableDescriptor(HTABLE_NAME_BYTES)
        cfs foreach { cf => desc1 addFamily getColumnDescriptor(cf) }
        adm.modifyTable(HTABLE_NAME_BYTES, desc1)

      } finally {
        adm.enableTable(HTABLE_NAME_BYTES)
      }

    } finally {
      adm.close()
    }
  }
}

