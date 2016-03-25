package models.maps.umap

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 14:31
  * Description: JSON-описание layer'а в UMap.
  */
object Layer {

  /** Поддержка JSON. */
  implicit def FORMAT: Format[Layer] = (
    (__ \ "name").format[String] and
    (__ \ "id").format[Int] and
    (__ \ "displayOnLoad").format[Boolean]
  )(apply, unlift(unapply))

}


/**
  * Класс одного слоя карты.
  *
  * @param name Название слоя.
  * @param id Уникальный номер слоя.
  * @param displayOnLoad Отображать сразу при загрузке? [true]
  */
case class Layer(
  name          : String,
  id            : Int,
  displayOnLoad : Boolean
)
