package io.suggest.es

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.13 16:58
  * Description: Constants for ElasticSearch models.
  */

object EsConstants {

  // Stemmer names
  def STEM_EN_FN = "f_stem_en"
  def STEM_RU_FN = "f_stem_ru"

  // Custom stopwords token filter name.
  def STOP_EN_FN = "f_stop_en"
  def STOP_RU_FN = "f_stop_ru"

  def EDGE_NGRAM_FN_1 = "f_engram1"

  def LOWERCASE_FN = "lowercase"

  def WORD_DELIM_FN = "f_word_delim"

  def STD_TN = "t_std"

  def DEEP_NGRAM_TN = "t_deep_ngram"

  /** keyword tokenizer */
  def KEYWORD_TOKENIZER = "t_keyword"

  /** Minimal analyzer without any luxury (no stemmers, no word-delimiters, etc). */
  def MINIMAL_ANALYZER = "a_minimal"

  /** Analyzer name for produce ngrams from first letter. */
  def ENGRAM_1LETTER_ANALYZER = "a_engram1"

  /** Analyzer for generate edge-ngrams with at least one letter, without stop-words filter. */
  def ENGRAM_1LETTER_NOSTOP_ANALYZER = ENGRAM_1LETTER_ANALYZER + "_nostop"

  /** Analyzer, produces ngrams with two and more letters. */
  def ENGRAM_2LETTER_ANALYZER = "a_engram2"

  /** Analyze russian text for full-text indexing. */
  def FTS_RU_ANALYZER = "a_fts_ru"

  /** deep ngram: produce maximum ngrams. Useful for tree-ed enum flags, tree-like identifiers, etc. */
  def DEEP_NGRAM_ANALYZER = "a_deep_ngram"

  /** KeyWord LowerCase analyzer. Used for non-fts tags indexing and others. */
  def KEYWORD_LOWERCASE_ANALYZER = "a_keyword_lc"

  /** Fts analyzer for short tags without stopwords filtering. */
  def FTS_NOSTOP_ANALYZER = "a_fts_nostop"

  /** default analyzer name. */
  def DEFAULT_ANALYZER = "a_default"

  /** Starting version value for saved ES documents, created at first with no updates.
    *
    * @see [[https://www.elastic.co/blog/elasticsearch-versioning-support]]
    *      That version number is a positive number between 1 and 2**63-1 (inclusive).
    */
  final def DOC_VSN_0 = 1L

}
