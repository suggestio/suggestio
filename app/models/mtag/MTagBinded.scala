package models.mtag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.11.15 17:30
 * Description: Контейнер данных "существующего" тега. В понятиях маппинга формы это тег, который
 * должен быть связан с редактируемым узлом.
 */
case class MTagBinded(
  face    : String,
  nodeId  : Option[String] = None
)

