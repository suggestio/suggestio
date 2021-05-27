package io.suggest.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 15:54
 * Description: Набор констант, используемый в кравлере и в веб-морде: имена полей в маппингах и т.д.
 */

object SioConstants {

  // Названия анализаторов. А зачем они тут, если относятся только к SioEsUtil?
  /** Минимальный анализатор, без излишеств. */
  def MINIMAL_AN        = "a_minimal"

  /** Делает n-граммы с первой буквы. */
  def ENGRAM_AN_1       = "a_engram1"
  
  /** Аналайзер для генерации edge-ngram с 1-й буквы, но без языковой обработки типа стемминга и  */
  def ENGRAM1_NOSTOP_AN = ENGRAM_AN_1 + "_nostop"

  /** Делает n-граммы со второй буквы. */
  def ENGRAM_AN_2       = "a_engram2"

  /** Дробит тексты и слова для обычной индексации. */
  def FTS_RU_AN         = "a_fts_ru"

  /** deep ngram: дробить на ngram'мы по максимуму. Полезно для списка комбинируемых флагов. */
  def DEEP_NGRAM_AN     = "a_deep_ngram"

  /** keyword tokenizer */
  def KEYWORD_TN        = "t_keyword"

  /** KeyWord LowerCase ANalyzer. Для целостной индексации тегов узлов ADN. */
  def KW_LC_AN          = "a_keyword_lc"

  /** Полнотекстовый поиск по [коротким] тегам, без фильтрации стоп-слов. */
  def FTS_NOSTOP_AN     = "a_fts_nostop"

  /** id дефолтового анализатора. */
  def DFLT_AN           = "a_default"

}
