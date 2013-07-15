package io.suggest.model

import io.suggest.index_info._
import org.apache.hadoop.fs.{PathFilter, Path}
import io.suggest.util.SiobixFs.{dkeyPathConf, fs}
import scala.concurrent.{Future, future}
import io.suggest.util.Logs
import scala.concurrent.ExecutionContext.Implicits._
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 14:30
 * Description: Сабж хранит инфу о связях текущего dkey с индексами, а также о необходимости произведения действий
 * над ними. Логически, хранилище данных об индексе устроено в виде директории с файлами:
 * /vasya.com/index_info/
 *  | N.also_replicate_to_this_index          // Скопировать уже имеющиеся данные в этот индекс.
 *  | A.writing_now_to_this_index
 *  | A.writing_now_also_to_other_index
 *  | X.writing_now_also_to_other_index       // Удалить A-индекс с указанным названием.
 *  | S.default
 *
 * Т.е. для разделения типов используюся односимвольные префиксы имён файлов.
 * Сами имена файлов соответствуют именам индексов-шард в ES. Имена используются при поиске всех dkey, которые используют указанный индекс.
 *
 * - 'A' (Active) означает, что это - файл с описанием активного индекса. Этот индекс содержит актуальную информацию,
 *   и в него должна идти запись результатов map-reduce.
 *   Содержимое этого файла - JSON, содержащий экспортированное состояние классов, описывающих индекс (см. sioutil io.suggest.index_info.*).
 *
 * - 'N' (New) означает, что необходимо также скопировать имеющиеся данные из @-индекса(-ов) в указанный, и затем сделать его @-индексом.
 *   Добавляемый индекс содержит теже данные, что и @-индекс, и может иметь установленный флаг use_for_search,
 *   означающий, что после завершения вышеописанной процедуры, нужно обновить S-указатель на этот индекс.
 *   Если флаг не установлен, но остался только один @-индекс, то SEARCH будет обновлён и будет указывать на единственный индекс.
 *   Если есть несколько @-индексов, то подразумевается, что они эквивалентны по содержанию друг другу.
 *
 * - 'X' означает, что этот индекс необходимо удалить. Имя файла соответствует удаляемому A-индексу, а содержимое - пустое.
 *   Если такого @-индекса не существует, то файл будет просто удален.
 *
 * - 'S'-Файл содержит название активного индекса (т.е. без префиксов), который будет использоваться для поиска.
 *   По сути, S-файл имитирует тормозные алиасы ES, но с учетом особенностей suggest.io.
 *   Отсутствие S-файла считается маловероятной ситуацией, поэтому это должно отрабатываться на веб-морде: должен
 *   быть выбран любой A-индекс и будет создан SEARCH-файл.
 *
 * Таким образом, для перемещения данных из индекса в индекс нужно создать N и X файлы, где имя X-файла соответствует имеющимуся A-файлу.
 *
 * Конкретные реализации вышеперечисленных файлов MIndexInfo находятся в классах io.suggest.index_info.Mii*.
 * В этом объекте - кое-какой общий код работы с моделью и разные высокоуровневые операции.
 */

object MDkeyVirtualIndex extends Logs with Serializable {

  // Имя поддиректории модели в папке $dkey. Используется как основа для всего остального в этой модели.
  val dirName = "inxs"

  // Фильтр файлов, которыми интересуется функция ensure при начальном листинге.
  val pathFilterEnsure = {
    new PathFilter with Serializable {
      val allowedPrefixesCh = List[Char](MiiActive.prefixCh, MiiAdd.prefixCh, MiiRemove.prefixCh)

      def accept(path: Path): Boolean = {
        val prefixCh = path.getName.charAt(0)
        allowedPrefixesCh contains prefixCh
      }
    }
  }


  /**
   * Сгенерить путь для dkey куда сохраняются данные модели.
   * @param dkey ключ домена.
   * @return путь.
   */
  def getDkeyPath(dkey:String) = new Path(dkeyPathConf(dkey), dirName)

  /**
   * Выдать путь к файлу в папке модели.
   * @param dkey ключ домена
   * @param filename имя файла
   * @return
   */
  def getFilePath(dkey:String, filename:String) = new Path(getDkeyPath(dkey), filename)


  /**
   * Главная frontend-функция, вызываемая между итерациями MR в рамках домена. Производит осмотр метаданных
   * индексов, производит необходимые действия, и возвращает список индексов, в которые необходимо проводить запись на следующей итерации.
   * @param dkey ключ домена.
   * @return Left, если требуется дождаться выполнения каких-то операций (фьючерс со списком обновлённых активных индексов).
   *         Right со списком active-индексов, если все готово к индексации.
   */
  def ensureDkey(dkey:String)(implicit client:Client): Either[Future[List[MiiActive]], List[MiiActive]] = {
    lazy val logPrefix = "ensureDkey(%s): " format dkey
    val path = getDkeyPath(dkey)
    debug(logPrefix + "path = " + path)
    val s = fs.listStatus(path, pathFilterEnsure)
      .toList
      .groupBy(_.getPath.getName.charAt(0))
      .mapValues(_.map(_.getPath))
    val activeInxsPaths = s.getOrElse(MiiActive.prefixCh, Nil)
    val activeInxs = MiiActive.readThese(activeInxsPaths)
    // Бывает, что заданы операции, которые необходимо произвести над индексом.
    val addInxPaths = s.getOrElse(MiiAdd.prefixCh, Nil)
    val rmInxPaths  = s.getOrElse(MiiRemove.prefixCh, Nil)
    if (!addInxPaths.isEmpty || !rmInxPaths.isEmpty) {
      val fut = future {
        info(logPrefix + "Index changes requested for %s:\nadd: %s\nremove: %s\nactive: %s" format(dkey, paths2names(addInxPaths), paths2names(rmInxPaths), paths2names(activeInxsPaths)))
        if (!addInxPaths.isEmpty) {
          val addInxs = MiiAdd.readThese(addInxPaths)
          // Нужно убедиться, что все добавляемые индексы существуют.
        }


        // TODO Для добавляемых индексов: нужно их создать (если ещё не созданы) и залить туда данные из любого из текущих индексов.

        // TODO Возможно тут уже пора обновить SEARCH-указатель.
        // TODO Для удаляемых: затем, если всё ок, снести удаляемые индексы.
        activeInxs
      }
      Left(fut)
    } else {
      Right(activeInxs)
    }
  }

  private def paths2names(l: Seq[Path]) = l.map(_.getName)

}

/**
 * Экземпляр записи, которая сериализуется в файл.
 * @param vinName Virtual Index Basename. Поле Basename в MVirtualIndex
 * @param subshards Подшарды (типы, ограниченные по временному интервалу)
 * @param generation поколение. Обычно когда 
 */
case class MDkeyVirtualIndex(
  vinName:    String,
  subshards:  List[DkeySubshard],
  generation: Int = 0
)


case class DkeySubshard(
  typename:      String,
  lowerDateDays: Int,
  shardNumber:   Int
)
