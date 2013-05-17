package models

import org.joda.time.DateTime
import util.DfsModelUtil
import org.apache.hadoop.fs.Path
import util.{Logs, SiobixFs}, SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import util.DateTimeUtil.dateTimeOrdering
import com.fasterxml.jackson.annotation.JsonIgnore

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

  /**
   * Выдать путь к файлу (который возможно не существует), который хранит (должен хранить) текущий объект.
   * @return
   */
  @JsonIgnore def getFilePath = MBlog.getFilePath(id)

  /**
   * Сохранить запись блога в хранилище.
   * @return саму себя для удобства method chaining.
   */
  def save = {
    JsonDfsBackend.writeTo(getFilePath, this)
    this
  }

  /**
   * Удалить текущий ряд их хранилища
   */
  def delete = MBlog.delete(id)

}


object MBlog extends Logs {

  val model_path = new Path(SiobixFs.siobix_conf_path, "blog")


  /**
   * Прочитать все записи из папки-хранилища.
   * @return Список распарсенных записей в виде классов MBlog.
   */
  def getAll : List[MBlog] = {
    // Читаем и парсим все файлы из папки model_path.
    fs.listStatus(model_path).foldLeft(List[MBlog]()) { (acc, fstatus) =>
      if (!fstatus.isDir) {
        readOneAcc(acc, fstatus.getPath)
      } else acc
    }
      // Далее, надо отсортировать записи в порядке их создания.
      .sortBy(_.date)
      .reverse
  }


  /**
   * Выдать путь к файлу с данными в json.
   * @param id
   * @return
   */
  def getFilePath(id: String) = new Path(model_path, id)


  /**
   * Удалить файл с записью блога из хранилища.
   * @param id id записи.
   * @return
   */
  def delete(id: String) : Boolean = {
    val path = getFilePath(id)
    fs.delete(path, false)
  }

  /**
   * Прочитать запись блога из базы по id
   * @param id id записи
   * @return запись блога, если есть.
   */
  def getById(id: String) : Option[MBlog] = readOne(getFilePath(id))


  /**
   * Аккуратненько прочитать файл. Если файла нет или чтение не удалось, то в логах будет экзепшен и None в результате.
   * @param path путь, который читать.
   * @return Option[MDomainPerson]
   */
  protected def readOne(path:Path) : Option[MBlog] = DfsModelUtil.readOne[MBlog](path)


  /**
   * Враппер над readOne для удобства вызова из foldLeft()().
   * @param acc аккамулятор типа List[MDomainPerson]
   * @param path путь, из которого стоит читать данные
   * @return аккамулятор
   */
  protected def readOneAcc(acc:List[MBlog], path:Path) : List[MBlog] = DfsModelUtil.readOneAcc[MBlog](acc, path)

}
