package io.suggest.util

import org.elasticsearch.index.mapper.internal._
import cascading.tuple.Fields
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 15:54
 * Description: Набор констант, используемый в кравлере и в веб-морде: имена полей в маппингах и т.д.
 */

object SioConstants {

  // Имена полей, используемых в sio-маппингах
  val FIELD_URL           = "url"
  val FIELD_LANGUAGE      = "lang"
  val FIELD_TITLE         = "title"
  val FIELD_CONTENT_TEXT  = "content_text"
  val FIELD_IMAGE_ID      = "image_id"
  val FIELD_DATE_KILOSEC  = "date_ks"
  val FIELD_DATE          = "date"

  // Имена системных полей, все в одном месте.
  def FIELD_ALL           = AllFieldMapper.NAME
  def FIELD_SOURCE        = SourceFieldMapper.NAME
  def FIELD_ROUTING       = RoutingFieldMapper.NAME
  def FIELD_ANALYZER      = AnalyzerMapper.NAME
  def FIELD_BOOST         = BoostFieldMapper.NAME

  // Названия анализаторов
  val ANALYZER_MINIMAL    = "a_minimal"
  val ANALYZER_EDGE_NGRAM = "a_edge_ngram"
  val ANALYZER_FTS_RU     = "a_fts_ru"

  // Суффиксы multi-полей
  val SUBFIELD_ENGRAM     = "gram"
  val SUBFIELD_FTS        = "fts"

  // Дата храниться в виде инстанта. Нужно убирать лишние нули.
  val DATE_INSTANT_ZEROES = 1000000

  // Имя директории в папке домена, в которой сохраняются картинки, нагенеренные кравлером.
  val THUMBS_SUBDIR       = "thumbs"

  // Конфиги всякие лежат в субдиректориях с таким названием
  val CONF_SUBDIR         = "conf"

  // Название ключевого поля: DkeyUrlKey. В HBase название не сохраняется, но глобально в рамках каждого Flow.
  val HBASE_KEY_FN = "hbaseRowKey"
  val HBASE_KEY_FIELDS = new Fields(HBASE_KEY_FN)

  // Префикс таблиц кравлера.
  val HTABLE_WEBTABLE_NAME_PREFIX = "wt_"

  // Время жизни рядов qi, создаваемых в базе на веб-морде.
  val DOMAIN_QI_TTL_SECONDS = (5 hours).toSeconds.toInt

}
