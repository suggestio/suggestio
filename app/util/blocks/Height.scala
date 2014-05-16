package util.blocks

import io.suggest.ym.model.common.BlockMeta
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:04
 * Description: Утиль для сборки блоков с heightBf-полем.
 * Жесткий велосипед с height необходим из-за серьезных проблем с расширенияеми маппингов через трейты.
 * Бывают родственные блоки, которые могут иметь или не иметь поля высоты. В таких случаях поле
 * Высоты не рендерится в одном из блоков, но всегда доступно для маппинга.
 */

object Height {
  val BF_HEIGHT_NAME_DFLT = "height"
  val BF_HEIGHT_DFLT_VALUE = Some(300)
  val AVAILABLE_VALS_DFLT = Set(300, 460, 620)
  val BF_HEIGHT_DFLT = BfHeight(
    name = BF_HEIGHT_NAME_DFLT,
    defaultValue = BF_HEIGHT_DFLT_VALUE,
    availableVals = AVAILABLE_VALS_DFLT
  )
}

/** Базовый интерфейс для поля heightBf. */
trait HeightI {
  def heightBf: BfHeight
}

/** Если нужно добавление поля в blockFields (т.е. в форму редактора), то нужен этот трейт. */
trait HeightII extends HeightI with ValT {
  abstract override def blockFieldsRev: List[BlockFieldT] = heightBf :: super.blockFieldsRev
}

/** Трейт, добавляющий статический расшаренный неизменяемый инстанс [[BfHeight]].
  * Полезно когда дефолтовые размеры полностью устраивают. */
trait HeightStatic extends HeightII {
  import Height._
  // final - для защиты от ошибочной перезаписи полей. При наступлении необходимости надо заюзать Height вместо HeightStatic
  override final def heightBf = BF_HEIGHT_DFLT
}

/** Трейт, создающий собственный экземпляр BfHeight. Параметры сборки можно переопределить
  * с дефолтовых через соотв. методы. */
trait HeightPlain extends HeightI {
  import Height._
  def heightDefaultValue: Option[Int] = BF_HEIGHT_DFLT_VALUE
  def heightAvailableVals: Set[Int] = AVAILABLE_VALS_DFLT
  override def heightBf = BfHeight(
    name = BF_HEIGHT_NAME_DFLT,
    defaultValue = heightDefaultValue,
    availableVals = heightAvailableVals
  )
}
/** Тоже самое, что и [[HeightPlain]], но + добавляющее поле в blocksFields. */
trait Height extends HeightPlain with HeightII


/** Бывает, что блок имеет фиксированную высоту. Тут хелпер, немного экономящий время.
  * Высоту можно переопределить. */
trait HeightFixed extends ValT {
  val HEIGHT = 300
  def getBlockMeta: BlockMeta = getBlockMeta(HEIGHT)
}
