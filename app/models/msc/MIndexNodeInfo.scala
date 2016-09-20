package models.msc

import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.16 23:22
  * Description: Контейнер данных о выбранном index-узле. Изначально, содержал только N2-узел.
  * Весь цикл жизни модели происходит прямо внутри ScIndex.
  *
  * @param mnode Узел, на котором будет базироваться выдача.
  * @param isRcvr Это узел-ресивер для карточек грядущей выдачи?
  */
case class MIndexNodeInfo(
  mnode       : MNode,
  isRcvr      : Boolean
)
