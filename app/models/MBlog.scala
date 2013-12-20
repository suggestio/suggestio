package models

import org.joda.time.DateTime
import util._
import org.apache.hadoop.fs.Path
import StorageUtil.StorageType._
import io.suggest.model._
import util.DateTimeUtil.dateTimeOrdering
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits._
import org.hbase.async.{KeyValue, DeleteRequest, GetRequest, PutRequest}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:37
 * Description: Записи в блоге. По сути json-файлы в хранилище.
 * Порт модели blog_record из старого sioweb.
 */

case class MBlog(
  id            : String,               // Имя файла в ФС. Нельзя переименовывать. Юзеру не отображается.
  var title     : String,
  var description: String,
  var bg_image  : String,
  var bg_color  : String,
  var text      : String,
  date          : DateTime = DateTime.now   // ctime не поддерживается в fs, поэтому дата хранится внутри файла
) {
  import MBlog.BACKEND

  /**
   * Сохранить запись блога в хранилище.
   * @return саму себя для удобства method chaining.
   */
  @JsonIgnore
  def save = BACKEND.save(this)

  /**
   * Удалить текущий ряд их хранилища
   */
  @JsonIgnore
  def delete = MBlog.delete(id)

}


object MBlog extends Logs {

  // Выбираем storage backend этой модели.
  private val BACKEND: Backend = {
    StorageUtil.STORAGE match {
      case DFS    => new DfsBackend
      case HBASE  => new HBaseBackend
    }
  }

  /**
   * Прочитать все записи из папки-хранилища.
   * @return Список распарсенных записей в виде классов MBlog.
   */
  def getAll = {
    BACKEND.getAll
      // Отсортировать записи в порядке убывания.
      .map { _.sortBy(_.date).reverse }
  }


  /**
   * Удалить файл с записью блога из хранилища.
   * @param id id записи.
   * @return
   */
  def delete(id: String) = BACKEND.delete(id)

  /**
   * Прочитать запись блога из базы по id
   * @param id id записи
   * @return запись блога, если есть.
   */
  def getById(id: String) = BACKEND.getById(id)


  /** Интерфейс для storage-backend'ов этой модели. */
  trait Backend {
    /**
     * Сохранение экземпляра MBlog в хранилище. Если запись уже существует, то она будет перезаписана
     * (или появится её новая версия в случае с HBase).
     * @param data Данные - экземпляр MBlog.
     * @return Фьючес с сохраненным MBlog. Обычно, это тот же экземпляр, что и исходный data.
     */
    def save(data:MBlog): Future[MBlog]

    /**
     * Чтения элемента по id.
     * @param id id записи блога.
     * @return Фьючерс с найденной записью, если такая существует.
     */
    def getById(id: String): Future[Option[MBlog]]

    /**
     * Прочитать все данные блога.
     * @return Фьючерс со списком записей, новые в начале.
     */
    def getAll: Future[List[MBlog]]

    /**
     * Удалить запись блога.
     * @param id id записи блога.
     * @return Фьючерс без значения.
     */
    def delete(id: String): Future[Any]
  }


  /** Backend для хранения в DFS. */
  class DfsBackend extends Backend {
    import SiobixFs.fs

    val modelPath = new Path(SiobixFs.siobix_conf_path, "blog")
    def getFilePath(id: String) = new Path(modelPath, id)
    def readOne(path:Path) : Option[MBlog] = DfsModelUtil.readOne[MBlog](path)
    def readOneAcc(acc:List[MBlog], path:Path) : List[MBlog] = DfsModelUtil.readOneAcc[MBlog](acc, path)

    def save(data:MBlog): Future[MBlog] = future {
      JsonDfsBackend.writeToPath(getFilePath(data.id), data)
      data
    }

    def getById(id: String): Future[Option[MBlog]] = future {
      readOne(getFilePath(id))
    }

    def getAll: Future[List[MBlog]] = future {
      // Читаем и парсим все файлы из папки model_path.
      fs.listStatus(modelPath).foldLeft(List[MBlog]()) { (acc, fstatus) =>
        if (!fstatus.isDir) {
          readOneAcc(acc, fstatus.getPath)
        } else acc
      }
    }

    def delete(id: String): Future[Any] = future {
      val path = getFilePath(id)
      fs.delete(path, false)
    }
  }


  /** Backend для хранения в HBase. Используем таблицу obj. */
  class HBaseBackend extends Backend with ModelSerialJson {
    import SioHBaseAsyncClient._
    import MObject.{HTABLE_NAME_BYTES, CF_BLOG}
    import io.suggest.model.HTapConversionsBasic._

    // TODO проверить и убедится, что таблица существует.

    val KEYPREFIX = "mblog:"

    private val CF_BYTES = CF_BLOG.getBytes
    private def QUALIFIER = CF_BYTES

    // TODO Ключ надо использовать для сортировки по дате.
    def id2key(id: String): Array[Byte] = KEYPREFIX + id

    def deserialize(data: Array[Byte]) = deserializeTo[MBlog](data)

    def save(data: MBlog): Future[MBlog] = {
      val key = id2key(data.id)
      val putReq = new PutRequest(HTABLE_NAME_BYTES, key, CF_BYTES, QUALIFIER, serialize(data))
      ahclient.put(putReq).map(_ => data)
    }

    def getById(id: String): Future[Option[MBlog]] = {
      val key = id2key(id)
      val getReq = new GetRequest(HTABLE_NAME_BYTES, key).family(CF_BLOG).qualifier(QUALIFIER)
      ahclient.get(getReq) map { r =>
        if (r.isEmpty) {
          None
        } else {
          val vb = r.head.value()
          val v = deserialize(vb)
          Some(v)
        }
      }
    }

    def getAll: Future[List[MBlog]] = {
      val scanner = ahclient.newScanner(HTABLE_NAME_BYTES)
      scanner.setFamily(CF_BLOG)
      scanner.setQualifier(QUALIFIER)
      val folder = new AsyncHbaseScannerFold[List[MBlog]] {
        def fold(acc0: List[MBlog], kv: KeyValue): List[MBlog] = {
          deserialize(kv.value()) :: acc0
        }

        override def mapThrowable(acc: List[MBlog], ex: Throwable): Throwable = MBlogAsyncFoldException(ex, acc)
      }
      folder(Nil, scanner)
    }

    def delete(id: String): Future[Any] = {
      val key = id2key(id)
      val delReq = new DeleteRequest(HTABLE_NAME_BYTES, key, CF_BYTES, QUALIFIER)
      ahclient.delete(delReq)
    }
  }


  case class MBlogAsyncFoldException(ex:Throwable, accLast:List[MBlog]) extends RuntimeException
}

