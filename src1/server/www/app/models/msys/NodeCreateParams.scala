package models.msys

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.15 11:01
 * Description: Модель данных формы создания узла шаблона [[views.html.sys1.market.adn.createAdnNodeFormTpl]].
 */


/**
  * Контейнер параметров создания узла через /sys/.
  * @param billInit Инициализировать биллинг узла.
  * @param extTgsInit Создать внешние таргеты для узла?
  * @param withDfltMads Создать дефолтовые карточки?
  * @param withId Создать узел с указанным id вместо рандомного.
  */
case class NodeCreateParams(
  billInit      : Boolean         = true,
  extTgsInit    : Boolean         = true,
  withDfltMads  : Boolean         = true,
  withId        : Option[String]  = None
)
