package util

import org.owasp.html.HtmlPolicyBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.06.13 17:40
 * Description: Санитизер для пользовательского контента на сайте.
 */
object HtmlSanitizer {

  /** Удалить весь HTML из текста. */
  val stripAllPolicy = new HtmlPolicyBuilder()
    .toFactory


  /** Удалить все HTML-теги кроме переноса строк. */
  val brOnlyPolicy = new HtmlPolicyBuilder()
    .allowElements("br")
    .toFactory


  /** Из писем юзеров нужно стрипать всё кроме текста, переносов строк и ссылок. */
  val supportMsgPolicy = new HtmlPolicyBuilder()
    .allowElements("br", "a")
    .allowAttributes("href", "target").onElements("a")
    .toFactory

  /** Из оформления стрипать html, оставляя только базовое форматирование. */
  val textFmtPolicy =  new HtmlPolicyBuilder()
    .allowCommonBlockElements()
    .allowElements("a", "span", "em", "i", "ul", "li", "ol")
    .allowAttributes("href", "target").onElements("a")
    .allowAttributes("style").onElements("span", "p")
    .requireRelNofollowOnLinks()
    .allowUrlProtocols("http", "https")
    .allowCommonInlineFormattingElements()
    .toFactory

  /** Политика текстового форматирования для MMartAdText офферов. */
  val adTextFmtPolicy = new HtmlPolicyBuilder()
    .allowElements("span")
    .allowElements("p")
    .allowElements("br")
    // TODO нужно добавить .matching с указанием допустимых значений span.class
    .allowAttributes("class").onElements("span")
    .toFactory

}
