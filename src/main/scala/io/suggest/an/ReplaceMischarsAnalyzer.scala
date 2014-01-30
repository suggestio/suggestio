package io.suggest.an

import org.apache.lucene.analysis.{TokenStream, TokenFilter}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.14 19:39
 * Description: Бывает, что русские буквы написаны вместо английских и наоборот. Ошибки внешне не видны: 'с' вместо 'c'.
 */
class ReplaceMischarsAnalyzer(stream: TokenStream) extends TokenFilter(stream) {
  protected val termAtt = addAttribute(classOf[CharTermAttribute])

  def incrementToken(): Boolean = {
    if (input.incrementToken()) {
      // Нужно вызвать фиксинг из TextUtil, по возможности не порождая мусора.
      ???
      true
    } else {
      false
    }
  }
}
