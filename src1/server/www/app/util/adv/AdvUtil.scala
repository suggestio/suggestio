package util.adv

import models.MNode
import models.blk.{BlockHeights, BlockMeta, BlockWidths}
import util.PlayMacroLogsImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 21:57
  * Description: Какая-то очень общая утиль для размещения.
  */
class AdvUtil extends PlayMacroLogsImpl {

  /**
    * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
    * другой одноимённый метод.
    *
    * @param mad Рекламная карточка или иная реализация блочного документа.
    * @return Площадь карточки.
    *         NoSuchElementException, если узел не является рекламной карточкой.
    */
  def getAdModulesCount(mad: MNode): Int = {
    val bm = mad.ad.blockMeta.get   // TODO Следует ли отрабатывать ситуацию, когда нет BlockMeta?
    getAdModulesCount(bm)
  }
  def getAdModulesCount(bm: BlockMeta): Int = {
    // Мультипликатор по ширине
    val wmul = BlockWidths(bm.width).relSz
    // Мультипликатор по высоте
    val hmul = BlockHeights(bm.height).relSz
    wmul * hmul
  }

}
