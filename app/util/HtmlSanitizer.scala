package util

import org.owasp.html.HtmlPolicyBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.06.13 17:40
 * Description:
 */
object HtmlSanitizer {

  // Удалить весь HTML из текста
  val stripAllPolicy = new HtmlPolicyBuilder()
    .toFactory


  // Удалить все HTML-теги кроме переноса строк.
  val brOnlyPolicy = new HtmlPolicyBuilder()
    .allowElements("br")
    .toFactory


  // Из писем юзеров нужно стрипать всё кроме текста, переносов строк и ссылок.
  val supportMsgPolicy = new HtmlPolicyBuilder()
    .allowElements("br", "a")
    .allowAttributes("href", "target").onElements("a")
    .toFactory
}
