package io.suggest.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 15:54
 * Description: Набор констант, используемый в кравлере и в веб-морде: имена полей в маппингах и т.д.
 */

object SioConstants {

  // Имена полей, используемых в sio-маппингах
  def FIELD_URL           = "url"
  def FIELD_LANGUAGE      = "lang"
  def FIELD_TITLE         = "title"
  def FIELD_CONTENT_TEXT  = "contentText"
  def FIELD_IMAGE_ID      = "imageId"
  def FIELD_DATE_KILOSEC  = "dateKs"
  def FIELD_DATE          = "date"
  def FIELD_PAGE_TAGS     = "pageTags"
  def FIELD_DKEY          = "dkey"


  def CURRENCY_CODE_DFLT  = "RUB"

  // Названия анализаторов. А зачем они тут, если относятся только к SioEsUtil?
  /** Минимальный анализатор, без излишеств. */
  def MINIMAL_AN        = "aMinimal"

  /** Делает n-граммы с первой буквы. */
  def ENGRAM_AN_1       = "aEdgeNgram1"
  
  /** Аналайзер для генерации edge-ngram с 1-й буквы, но без языковой обработки типа стемминга и  */
  def ENGRAM1_NOSTOP_AN = ENGRAM_AN_1 + "NoStop"

  /** Делает n-граммы со второй буквы. */
  def ENGRAM_AN_2       = "aEdgeNgram2"

  /** Дробит тексты и слова для обычной индексации. */
  def FTS_RU_AN         = "aFtsRu"

  /** deep ngram: дробить на ngram'мы по максимуму. Полезно для списка комбинируемых флагов. */
  def DEEP_NGRAM_AN     = "deepNgram"

  /** keyword tokenizer */
  def KEYWORD_TN        = "kw"

  /** KeyWord LowerCase ANalyzer. Для целостной индексации тегов узлов ADN. */
  def KW_LC_AN          = "tag"

  /** Полнотекстовый поиск по [коротким] тегам, без фильтрации стоп-слов. */
  def FTS_NOSTOP_AN     = "ftsNoStop"

  /** id дефолтового анализатора. */
  def DFLT_AN           = "default"


  //v1: Суффиксы multi-полей
  def SUBFIELD_ENGRAM     = "gram"
  def SUBFIELD_FTS        = "fts"

}
