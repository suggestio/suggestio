package models

import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 13:34
 * Description:
 */

object UmapTplArgs {

  def nodeGeoLevelsJson(ngls: Seq[NodeGeoLevel])(implicit lang: Lang): JsArray = {
    val lvls = ngls.map { ngl =>
      JsObject(Seq(
        "displayOnLoad" -> JsBoolean(true),
        "name"          -> JsString( Messages("ngls." + ngl.esfn) ),
        "id"            -> JsNumber(ngl.id)
      ))
    }
    JsArray(lvls)
  }

}

/**
 * Набор аргументов для рендера шаблоны с картой Umap.
 * @param nodesMap Карта узлов по категориям. Если пусто, то значит работа идёт в рамках одного узла.
 * @param dlUpdateUrl Заготовка ссылки, которая будет делать сабмит для сохранения слоя.
 *                    На месте вставки id слоя надо использовать шаблон "{pk}".
 */
case class UmapTplArgs(
  dlUpdateUrl: String,
  dlGetUrl: String,
  nodesMap: Map[AdnShownType, Seq[MAdnNode]],
  editAllowed: Boolean = true,
  title: String,
  ngls: Seq[NodeGeoLevel]
) {
  def nglsJson(implicit lang: Lang) = UmapTplArgs.nodeGeoLevelsJson(ngls)

}

