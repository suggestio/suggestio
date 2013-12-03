package io.suggest.model

import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits._
import io.suggest.util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import io.suggest.util.{JacksonWrapper, SiobixFs}
import org.hbase.async._
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.13 11:52
 * Description: hbase-модель для domain-данных. Таблица включает в себя инфу по доменам, выраженную props CF и в др.моделях.
 */

/**
 * Класс записи этой модели.
 * @param dkey ключ домена.
 * @param addedBy Описание того, кто добавил домен в индексацию.
 * @param addedAt Дата добавления в базу.
 */
case class MDomain(
  dkey: String,
  addedBy: String,
  addedAt: DateTime = DateTime.now
) {
  import MDomain._

  /**
   * Сохранить текущую запись в хранилище. Если такая уже существует, то она будет перезаписана.
   * @return Фьючерс с сохраненным экземпляром записи.
   */
  def save = BACKEND.save(this)

  def delete = MDomain.delete(dkey)
}


object MDomain {

  val BACKEND: Backend = new HBaseBackend

  /**
   * Прочитать экземпляр модели из хранилища по ключу.
   * @param dkey Ключ домена.
   * @return Фьючерс с опциональным экземпляром класса этой модели.
   */
  def getForDkey(dkey: String) = BACKEND.getForDkey(dkey)

  /**
   * Удалить из хранилища указанный домен.
   * @param dkey Ключ домена.
   * @return Фьючерс с неопределенными данными. Чисто для синхронизации.
   */
  def delete(dkey: String) = BACKEND.delete(dkey)


  /**
   * Выдать все домены.
   * @return Фьючерс со списком записей в неопределенном порядке.
   */
  def getAll = BACKEND.getAll


  /**
   * Выдать несколько доменов из общей кучи.
   * HBase-функция оптимизирована под продакшен и максимально эффективное потребление ресурсов.
   * @param dkeys Список ключей. Он будет унимально отсортирован.
   * @return Фьючерс со списком записей в неопределенном порядке.
   */
  def getSeveral(dkeys: Seq[String]) = BACKEND.getSeveral(dkeys)


  /** Интерфейс хранилищ модели. */
  trait Backend {
    def getForDkey(dkey: String): Future[Option[MDomain]]
    def getAll: Future[List[MDomain]]
    def save(d: MDomain): Future[MDomain]
    def delete(dkey: String): Future[Any]
    def getSeveral(dkeys: Seq[String]): Future[List[MDomain]]
  }


  /** Backend для хранения данных модели в DFS. */
  class DfsBackend extends Backend {
    val filename = "m_domain"

    private def dkeyPath(dkey: String): Path = {
      val dkeyDir = SiobixFs.dkeyPathConf(dkey)
      new Path(dkeyDir, filename)
    }

    def getForDkey(dkey: String): Future[Option[MDomain]] = future {
      val filePath = dkeyPath(dkey)
      fs.exists(filePath) match {
        // Файл с данными по юзеру пуст - поэтому можно его не читать, а просто сделать необходимый объект.
        case true =>
          val person = JsonDfsBackend.getAs[MDomain](filePath, fs).get
          Some(person)

        case false => None
      }
    }

    def save(d: MDomain): Future[MDomain] = future {
      val filePath = dkeyPath(d.dkey)
      JsonDfsBackend.writeToPath(filePath, overwrite=true, value=d)
      d
    }

    def getAll: Future[List[MDomain]] = future {
      val globPath = dkeyPath("*")
      fs.globStatus(globPath).foldLeft[List[MDomain]] (Nil) { (acc, fstatus) =>
        if (!fstatus.isDir) {
          JsonDfsBackend.getAs[MDomain](fstatus.getPath, fs) match {
            case Some(mdomain) => mdomain :: acc
            case None => acc
          }
        } else {
          acc
        }
      }
    }

    def delete(dkey: String): Future[Any] = future {
      val filePath = dkeyPath(dkey)
      fs.delete(filePath, true)
    }

    // Тут примитивная фунцкия, ибо DfsBackend не для продакшена и эффективность работы не требуется вообще.
    def getSeveral(dkeys: Seq[String]): Future[List[MDomain]] = {
      getAll.map(_.filter(dkeys contains _))
    }

  }


  /** Backend для хранения данных модели в HBase. */
  class HBaseBackend extends Backend {
    import SioHBaseAsyncClient._
    import HTapConversionsBasic._
    import MObject.{HTABLE_NAME_BYTES, CF_DOMAIN}

    val QUALIFIER = CF_DOMAIN

    def serialize(d: MDomain) = JacksonWrapper.serialize(d).getBytes
    def deserialize(d: Array[Byte]) = JacksonWrapper.deserialize[MDomain](d)

    def getForDkey(dkey: String): Future[Option[MDomain]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, dkey:Array[Byte]).family(CF_DOMAIN).qualifier(QUALIFIER)
      ahclient.get(getReq).map { kvs =>
        if (kvs.isEmpty) {
          None
        } else {
          val result = deserialize(kvs.head.value)
          Some(result)
        }
      }
    }

    def getAll: Future[List[MDomain]] = collectAllFromScanner(getScanner)


    def save(d: MDomain): Future[MDomain] = {
      val putReq = new PutRequest(HTABLE_NAME_BYTES, d.dkey:Array[Byte], CF_DOMAIN, QUALIFIER, serialize(d))
      ahclient.put(putReq).map {_ => d}
    }

    def delete(dkey: String): Future[Any] = {
      val delReq = new DeleteRequest(HTABLE_NAME_BYTES, dkey:Array[Byte], CF_DOMAIN, QUALIFIER)
      ahclient.delete(delReq)
    }

    def getSeveral(dkeys: Seq[String]): Future[List[MDomain]] = {
      val scanner = getScanner
      val dkeysSorted = dkeys.sorted.distinct   // TODO Надо бы использовать какой-нибудь usort.
      scanner.setStartKey(dkeysSorted.head)
      scanner.setStopKey(dkeysSorted.last + " ")
      // Чтобы сервер hbase отсеивал лишние ключи сразу, нужен regexp
      val keyRe = "^(" + dkeys.mkString("|") + ")$"
      scanner.setKeyRegexp(keyRe)
      collectAllFromScanner(scanner)
    }

    private def getScanner = {
      val scanner = ahclient.newScanner(HTABLE_NAME_BYTES)
      scanner.setFamily(CF_DOMAIN)
      scanner.setQualifier(QUALIFIER)
      scanner
    }

    private def collectAllFromScanner(scanner: Scanner): Future[List[MDomain]] = {
      val folder = new AsyncHbaseScannerFold[List[MDomain]] {
        def fold(acc0: List[MDomain], kv: KeyValue): List[MDomain] = {
          deserialize(kv.value) :: acc0
        }
      }
      folder(Nil, getScanner)
    }
  }

}
