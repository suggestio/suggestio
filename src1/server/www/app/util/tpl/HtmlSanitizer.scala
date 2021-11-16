package util.tpl

import org.owasp.html.HtmlPolicyBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.06.13 17:40
 * Description: Санитизер для пользовательского контента на сайте.
 */
object HtmlSanitizer {

  /** Удалить весь HTML из текста. */
  def stripAllPolicy = new HtmlPolicyBuilder()
    .toFactory

  /** Удалить все HTML-теги кроме переноса строк. */
  /*
  val brOnlyPolicy = new HtmlPolicyBuilder()
    .allowElements("br")
    .toFactory
  */

  /** Из писем юзеров нужно стрипать всё кроме текста, переносов строк и ссылок. */
  /*
  lazy val supportMsgPolicy = new HtmlPolicyBuilder()
    .allowElements("br", "a")
    .allowAttributes("href", "target").onElements("a")
    .toFactory
  */

  /** Из оформления стрипать html, оставляя только базовое форматирование. */
  def textFmtPolicy = new HtmlPolicyBuilder()
    // 2017.feb.17: Тут был вызов allowCommonBlockElements(), который добавлял h1..h6 теги,
    // Это приводило к проблемам с копипастингом текста с других страниц: например h1 выставлял
    // font-weight по дефолту в bold, тихо ломая рендер всех шрифтов в описании к карточкам.
    // При этом в tinyMCE нельзя было избавиться от этого. Пока просто запрещаем все h#-теги.
    .allowElements("p", "div", "ul", "ol", "li", "blockquote", "a", "img", "span", "em")
    .allowAttributes("style", "class").onElements("span", "p", "div", "img", "strong", "a", "em", "ul", "blockquote")
    .allowAttributes("src").onElements("img")
    .allowAttributes("href").onElements("a")
    // TODO img.src url policy => *.nodes.suggest.io || localhost
    // TODO iframe.src URL policy
    .requireRelNofollowOnLinks()
    .allowUrlProtocols("http", "https")
    .allowCommonInlineFormattingElements()
    .allowStyling()
    .allowAttributes("href", "target").onElements("a")
    .toFactory

}
