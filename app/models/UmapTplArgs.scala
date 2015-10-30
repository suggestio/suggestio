package models

import play.api.i18n.Messages
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 13:34
 * Description: Модель контейнера аргументов для передачи в редактор umap из контроллера.
 */

object UmapTplArgs {

  def nodeGeoLevelsJson(ngls: Seq[NodeGeoLevel])(implicit lang: Messages): JsArray = {
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


/** Интерфейс модели аргументов рендера карты. */
trait IUmapTplArgs {

  def dlUpdateUrl : String
  def dlGetUrl    : String
  def nodesMap    : Map[AdnShownType, Seq[MNode]]
  def editAllowed : Boolean
  def title       : String
  def ngls        : Seq[NodeGeoLevel]

  def nglsJson(implicit lang: Messages) = UmapTplArgs.nodeGeoLevelsJson(ngls)

}


/**
 * Набор аргументов для рендера шаблоны с картой Umap.
 * @param nodesMap Карта узлов по категориям. Если пусто, то значит работа идёт в рамках одного узла.
 * @param dlUpdateUrl Заготовка ссылки, которая будет делать сабмит для сохранения слоя.
 *                    На месте вставки id слоя надо использовать шаблон "{pk}".
 */
case class UmapTplArgs(
  override val dlUpdateUrl : String,
  override val dlGetUrl    : String,
  override val nodesMap    : Map[AdnShownType, Seq[MNode]],
  override val editAllowed : Boolean = true,
  override val title       : String,
  override val ngls        : Seq[NodeGeoLevel]
)
  extends IUmapTplArgs

