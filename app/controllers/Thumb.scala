package controllers

import play.api.mvc._
import util.{Logs, SiobixFs}
import org.apache.hadoop.fs.Path
import scala.concurrent.duration._
import io.suggest.util.SioConstants._
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.model.MImgThumb
import util.DateTimeUtil
import org.joda.time.Instant
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Раздача картинок, относящихся к поисковой выдаче. Точнее, превьюшек картинок.
 */

object Thumb extends Controller with Logs {

  import LOGGER._

  val domainsRoot   = SiobixFs.siobix_out_path
  val thumbsSubpath = new Path(THUMBS_SUBDIR)

  val CACHE_THUMB_CLIENT_SECONDS = {
    current.configuration.getInt("thumb.cache.client.seconds") getOrElse 36000
  }

  /**
   * Выдать картинку из HDFS. Используется для визуализации выдачи.
   * Валидность параметров проверяется в роутере регэкспами.
   * @param dkey Ключ домена. Возможно содержит www и иной мусор.
   * @param imageId Хеш-ключ картинки в хранилище домена.
   * @return
   */
  def getThumb(dkey:String, imageId:String) = Action.async { request =>
    lazy val logPrefix = s"getThumb($dkey, $imageId): "
    MImgThumb.getThumbById(imageId) map {
      case Some(result) =>
        val ts0 = new Instant(result.timestamp) // не lazy, ибо всё равно понадобиться хотя бы в одной из веток.
        val isCached = request.headers.get(IF_MODIFIED_SINCE) flatMap {
          DateTimeUtil.parseRfcDate
        } exists { dt =>
          ts0 isBefore dt
        }
        if (isCached) {
          trace(logPrefix + "304 Not Modified")
          NotModified

        } else {
          trace(s"${logPrefix}200 Ok. size = ${result.img.length} bytes")
          Ok(result.img)
            .as("image/jpeg")
            .withHeaders(
              LAST_MODIFIED -> DateTimeUtil.df.print(ts0),
              CACHE_CONTROL -> ("public, max-age=" + CACHE_THUMB_CLIENT_SECONDS)
            )
        }

      case None =>
        warn(logPrefix + "404")
        NotFound
    }
  }

}
