package io.suggest.an

import org.apache.lucene.analysis.{TokenStream, TokenFilter}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version
import io.suggest.util.TextUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.14 19:39
 * Description: Бывает, что русские буквы написаны вместо английских и наоборот. Ошибки внешне не видны: 'с' вместо 'c'.
 * И тут Lucene-фильтр, который избавляет нас от этой головной боли.
 */
class ReplaceMischarsAnalyzer(vsn:Version, stream: TokenStream) extends TokenFilter(stream) {
  protected val termAtt = addAttribute(classOf[CharTermAttribute])

  def incrementToken(): Boolean = {
    if (input.incrementToken()) {
      // Нужно вызвать фиксинг из TextUtil, по возможности без лишнего мусора.
      TextUtil.mischarFixChArr(termAtt.buffer, 0, termAtt.length)
      true
    } else {
      false
    }
  }
}
