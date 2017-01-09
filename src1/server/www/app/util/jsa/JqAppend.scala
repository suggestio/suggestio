package util.jsa

import play.api.libs.json._


/** Дописать вызов .insertAdjacentHtml() к билдеру. */
trait JsInsertAdjacentHtmlBeforeEnd extends JsAction {
  def html: JsString
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    super.renderJsAction(sb)
      .append(".insertAdjacentHTML('beforeend',")
      .append(html.toString())
      .append(')')
  }
}

trait DocumentGetElementById extends JsAction {
  def id: String

  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    super.renderJsAction(sb)
      .append("document.getElementById('")
      .append(id)
      .append("')")
  }
}

/** Дописать в тело тега с указанным id. */
case class JsAppendById(id: String, html: JsString) extends DocumentGetElementById with JsInsertAdjacentHtmlBeforeEnd {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    super.renderJsAction(sb)
      .append(';')
  }
}


case class InnerHtmlById(id: String, html: JsString) extends DocumentGetElementById {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    super.renderJsAction(sb)
      .append(".innerHTML=")
      .append(html)
      .append(';')
  }
}
