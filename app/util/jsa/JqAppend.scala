package util.jsa

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 13:36
 * Description: Экшен для добавления в конец какого-либо элемента на станице указанного html.
 * @param target $-селектор целевого элемента
 * @param html Верстка или просто текст, готовые к добавлению в ответ.
 *             implicit convertion'ы в sio-контроллерах быстро подхватят этот формат.
 */
case class JqAppend(target: String, html: JsString) extends JsAction {

  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    // TODO Возможно, в target надо экранировать ковычки.
    sb.append("$('").append(target).append("').append('")
      .append(html.toString())
      .append("');")
  }

}


case class JsAppendByTagName(tagName: String, html: JsString) extends JsAction {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    sb.append("document.getElementsByTagName('").append(tagName)
      .append("')[0].insertAdjacentHTML('beforeend', '").append(html.toString()).append("');")
  }
}
