package models.maps.umap

import models._
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 13:34
 * Description: Модель контейнера аргументов для передачи в редактор umap из контроллера.
 */

object UmapTplArgs {


  def nodeGeoLevelsJson(ngls: Seq[NodeGeoLevel])(implicit messages: Messages): JsArray = {

    implicit val nglWrites: OWrites[NodeGeoLevel] = (
      (__ \ "displayOnLoad").write[Boolean] and
      (__ \ "name").write[String] and
      (__ \ "id").write[Int]
    )(ngl => (true, messages(ngl.l10nPlural), ngl.id))

    Json.toJson(ngls)
      .asInstanceOf[JsArray]
  }

}


/** Интерфейс модели аргументов рендера карты. */
trait IUmapTplArgs {

  /**
    * Заготовка ссылки, которая будет делать сабмит для сохранения слоя.
    * На месте вставки id слоя надо использовать шаблон "{pk}".
    */
  def dlUpdateUrl : String
  def dlGetUrl    : String
  def editAllowed : Boolean
  def title       : String
  def ngls        : Seq[NodeGeoLevel]

  def nglsJson(implicit lang: Messages) = UmapTplArgs.nodeGeoLevelsJson(ngls)

}


/** Набор аргументов для рендера шаблоны с картой Umap.
  * Дефолтовая реализация [[IUmapTplArgs]]. */
case class UmapTplArgs(
  override val dlUpdateUrl : String,
  override val dlGetUrl    : String,
  override val editAllowed : Boolean = true,
  override val title       : String,
  override val ngls        : Seq[NodeGeoLevel]
)
  extends IUmapTplArgs

