package models

import org.joda.time.DateTime
import util.DfsModelUtil
import org.apache.hadoop.fs.Path
import util.{Logs, SiobixFs, StorageUtil}
import StorageUtil.StorageType._
import io.suggest.model.{SioHBaseAsyncClient, JsonDfsBackend, MObject}
import util.DateTimeUtil.dateTimeOrdering
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits._
import org.hbase.async.{DeleteRequest, GetRequest, PutRequest}
import io.suggest.util.JacksonWrapper
import java.io.ByteArrayInputStream
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
  def save = BACKEND.save(id, this)

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
  def getAll = BACKEND.getAll


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


  trait Backend {
    def save(id: String, data:MBlog): Future[MBlog]
    def getById(id: String): Future[Option[MBlog]]
    def getAll: Future[List[MBlog]]
    def delete(id: String): Future[Unit]
  }

  /** Backend для хранения в DFS. */
  class DfsBackend extends Backend {
    import SiobixFs.fs

    val modelPath = new Path(SiobixFs.siobix_conf_path, "blog")
    def getFilePath(id: String) = new Path(modelPath, id)
    def readOne(path:Path) : Option[MBlog] = DfsModelUtil.readOne[MBlog](path)
    def readOneAcc(acc:List[MBlog], path:Path) : List[MBlog] = DfsModelUtil.readOneAcc[MBlog](acc, path)

    def save(id: String, data:MBlog): Future[MBlog] = future {
      JsonDfsBackend.writeToPath(getFilePath(id), data)
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
      // Далее, надо отсортировать записи в порядке их создания.
      .sortBy(_.date)
      .reverse
    }

    def delete(id: String): Future[Unit] = future {
      val path = getFilePath(id)
      fs.delete(path, false)
    }
  }


  /** Backend для хранения в HBase. Используем таблицу obj. */
  class HBaseBackend extends Backend {
    import SioHBaseAsyncClient._
    import MObject.{HTABLE_NAME_BYTES, CF_BLOG}

    val KEYPREFIX = "MBlog:"
    val QUALIFIER = "blog".getBytes

    // TODO проверить и убедится, что таблица существует.
    def id2key(id: String) = (KEYPREFIX + id).getBytes

    def serialize(data: MBlog) = JacksonWrapper.serialize(data).getBytes

    def deserialize(data: Array[Byte]): MBlog = {
      val bais = new ByteArrayInputStream(data)
      JacksonWrapper.deserialize[MBlog](bais)
    }

    def save(id: String, data: MBlog): Future[MBlog] = {
      val key = id2key(id)
      val putReq = new PutRequest(HTABLE_NAME_BYTES, key, CF_BLOG, QUALIFIER, serialize(data))
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

    def getAll: Future[List[MBlog]] = ???

    def delete(id: String): Future[Unit] = {
      val key = id2key(id)
      val delReq = new DeleteRequest(HTABLE_NAME_BYTES, key)
      ahclient.delete(delReq).map(_ => ())
    }
  }

}
