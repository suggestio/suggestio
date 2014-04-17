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

  // Column family для хранения произвольных k-v колонок для доменов (ключ - reversed dkey). В значениях идём по алфавиту.
  val CF_DPROPS         = "a" // Пропертисы всякие. js-install в частности.
  val CF_MVI            = "b"

  // Для моделей веб-морды:
  val CF_DDATA          = "d" // DomainData - сохранение json'а настроек (веб-морда)
  val CF_DQI            = "f" // QI-отметки, не привязаные к юзеру. Имеют ttl.

  // Инфа по юзерам. Access pattern - веб-морда.
  val CF_UAUTHZ         = "h" // Данные по авторизациям юзеров в доменах.

  // Другое (веб-морда).
  val CF_BLOG           = "i" // Блог-записи/новости (веб-морда, ключ = некий id).
  val CF_DOMAIN         = "j" // Записи MDomain.
  val CF_FACET_INVLINK  = "k" // У кравлера есть хранимые данные sio.analysis.facet.invlink.

  val CF_RA_PROPS       = "l" // Хлам с random-access, не предназначенный для ввода-вывода во flow. Конфиги глобальных процессов например.


  // /!\ При добавлении новых CF-записей нужно также обновлять/запиливать функции createTable() и updateTable().
  def CFs = Seq(
    CF_DPROPS, CF_MVI,
    CF_DDATA, CF_DQI,
    CF_UAUTHZ ,
    CF_BLOG, CF_DOMAIN,
    CF_FACET_INVLINK,
    CF_RA_PROPS
  )

  def CFs_CRAWLER = Seq(CF_DOMAIN, CF_DPROPS, CF_MVI, CF_FACET_INVLINK)


  def getColumnDescriptor: PartialFunction[String, HColumnDescriptor] = {
    case cf @ CF_DPROPS          => hcd(cf, 2)
    case cf @ CF_MVI             => MVIUnit.getCFDescriptor
    case cf @ CF_DDATA           => hcd(cf, 2)
    case cf @ CF_DQI             => hcd(cf, 1).setTimeToLive(DOMAIN_QI_TTL_SECONDS)
    case cf @ CF_UAUTHZ          => hcd(cf, 2)
    case cf @ CF_BLOG            => hcd(cf, 1)
    case cf @ CF_DOMAIN          => hcd(cf, 2)
    case cf @ CF_FACET_INVLINK   => hcd(cf, 1)
    case cf @ CF_RA_PROPS        => hcd(cf, 1)
  }

  private def hcd(cf:String, maxVsn:Int) = HTableModel.cfDescSimple(cf, maxVsn)


  /** Выставить произвольнон значение для произвольной колонки в CF_PROPS. По идее должно использоваться из других моделей,
   * занимающихся сериализацией.
   * @param key Ключ.
   * @param value сериализованное значение
   * @param qualifier опциональное имя колонки.
   * @return Фьючерс для опциональной синхронизации. Любые данные внутри возвращаемого фьючерса не имеют смысла.
   */
  def setProp(key:String, value:Array[Byte], qualifier:String = CF_RA_PROPS): Future[_] = {
    val putReq = new PutRequest(HTABLE_NAME_BYTES, key.getBytes, CF_RA_PROPS.getBytes, qualifier.getBytes, value)
    ahclient.put(putReq)
  }


  /** Прочитать провертис, выставленный через setProp().
   * @param key Ключ.
   * @param qualifier опциональный подключ.
   * @return Фьючерс с опциональным значением, если такое найдено.
   */
  def getProp(key: String, qualifier: String = CF_RA_PROPS)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    val getReq = new GetRequest(HTABLE_NAME, key)
      .family(CF_RA_PROPS)
      .qualifier(qualifier)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        Some(results.head.value)
      }
    }
  }


  /**
   * Удалить ряд из таблицы из CF_RA_PROPS.
   * @param key Ключ.
   * @param qualifierOpt Опциональное имя колонки. Если None, то будет удален весь ряд из CF.
   * @return Фьючерс для синхронизации.
   */
  def deleteProp(key: String, qualifierOpt: Option[String] = None)(implicit ec: ExecutionContext): Future[_] = {
    val delReq: DeleteRequest =
    if (qualifierOpt.isEmpty) {
      new DeleteRequest(HTABLE_NAME, key, CF_RA_PROPS)
    } else {
      new DeleteRequest(HTABLE_NAME, key, CF_RA_PROPS, qualifierOpt.get)
    }
    ahclient.delete(delReq)
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
            val delReq = new DeleteRequest(HTABLE_NAME_BYTES, kv.key, cfName.getBytes)
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

