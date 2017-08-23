package models.blk.ed

import io.suggest.model.n2.node.MNode


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.15 14:43
  * Description: Контейнер с результатом маппинга формы редактирования рекламной карточки.
  */
case class AdFormResult(
  mad: MNode,
  bim: BlockImgMap
)
