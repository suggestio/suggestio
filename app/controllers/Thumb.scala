package controllers

import play.api.mvc._
import util.SiobixFs
import SiobixFs.fs
import org.apache.hadoop.fs.Path
import play.api.libs.iteratee.Enumerator
import scala.concurrent.duration._
import io.suggest.util.SioConstants._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Раздача картинок, относящихся к поисковой выдаче. Точнее, превьюшек картинок.
 */

object Thumb extends Controller {

  val domainsRoot   = SiobixFs.siobix_out_path
  val thumbsSubpath = new Path(THUMBS_SUBDIR)

  // Хидер Cache-Control для всех одинаковый
  val cacheControlHdr = {
    val hdrValue = "public, max-age=" + 10.days.toSeconds
    "Cache-Control" -> hdrValue
  }


  /**
   * Выдать картинку из HDFS. Используется для визуализации выдачи.
   * Валидность параметров проверяется в роутере регэкспами.
   * @param dkey Ключ домена. Возможно содержит www и иной мусор.
   * @param imageId Хеш-ключ картинки в хранилище домена.
   * @return
   */
  def getThumb(dkey:String, imageId:String) = Action {
    val imageFilePath = getImageFilePath(dkey, imageId)
    if (fs.exists(imageFilePath)) {
      // Файл есть в хранилище. пора его отправить
      val fileStream = fs.open(imageFilePath, 16384)
      // Используется Chunked как самый простой вариант раздачи картинок из InputStream.
      val fileEnumerator = Enumerator.fromStream(fileStream)
      Ok.chunked(fileEnumerator)
        .as("image/jpeg")
        .withHeaders(cacheControlHdr)

    } else {
      // Не найдено картинки в базе
      NotFound
    }
  }


  /**
   * Сгенерить путь до картинки в HDFS.
   * @param dkey Нормализованный ключ домена
   * @param imageId Заведомо валидный ключ картинки
   * @return Path
   */
  protected def getImageFilePath(dkey:String, imageId:String) = {
    val dkeyPath   = new Path(domainsRoot, dkey)
    val imagesPath = new Path(thumbsSubpath, imageId + ".jpeg")
    new Path(dkeyPath, imagesPath)
  }

}
