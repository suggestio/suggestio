package models.mtag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.11.15 17:18
 * Description: Модель результата маппинга формы добавления тегов.
 */
case class MTagsAddFormBinded(
  added     : List[String],
  existing  : List[MTagBinded]
)


