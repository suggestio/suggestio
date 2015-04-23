package models.msys

import play.api.data._, Forms._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.15 11:01
 * Description: Модель данных формы параметров создания узла через /sys/.
 */

object NodeCreateParams {

  /** Маппинг для форм, содержащих [[NodeCreateParams]]. */
  def mappingM: Mapping[NodeCreateParams] = {
    val b = boolean
    mapping(
      "billInit"      -> b,
      "extTgsInit"    -> b,
      "withDfltMads"  -> b
    )
    { NodeCreateParams.apply }
    { NodeCreateParams.unapply }
  }

}


/**
 * Контейнер параметров создания узла через /sys/.
 * @param billInit Инициализировать биллинг узла.
 * @param extTgsInit Создать внешние таргеты для узла?
 * @param withDfltMads Создать дефолтовые карточки?
 */
case class NodeCreateParams(
  billInit      : Boolean = true,
  extTgsInit    : Boolean = true,
  withDfltMads  : Boolean = true
) {

  def nonEmpty: Boolean = {
    productIterator.exists {
      case b: Boolean               => b
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case str: String              => !str.isEmpty
      case _                        => true
    }
  }

  def isEmpty = !nonEmpty

}

