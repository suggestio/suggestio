package util.blocks

import models._
import util.blocks.BlocksEditorFields.BefText
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:16
 * Description: Утиль для блоков, содержащих поле descrBf.
 */

object Descr {
  val BF_NAME_DFLT = "descr"
  val LEN_MAX_DFLT = 160
  val DEFAULT_VALUE_DFLT = Some(AOStringField("Только сегодня", AOFieldFont("444444")))
  val BF_DESCR_DFLT = BfText(
    name = BF_NAME_DFLT,
    field = BlocksEditorFields.TextArea,
    maxLen = LEN_MAX_DFLT,
    defaultValue = DEFAULT_VALUE_DFLT
  )
}

/** Базовый трейт для descrBf-трейтов, как статических так и динамических. */
trait DescrT extends ValT {
  def descrBf: BfText
  abstract override def blockFieldsRev: List[BlockFieldT] = descrBf :: super.blockFieldsRev
}

/** Статическая реализация descrBf. Экономит оперативку, когда дефолтовые значения полностью
  * устраивают. */
trait DescrStatic extends DescrT {
  override final def descrBf = Descr.BF_DESCR_DFLT
}

/** Динамическая реализация descrBf. Генерит персональный инстанс descrBf для блока.
  * Параметры сборки поля можно переопределить через соответствующие методы. */
trait Descr extends DescrT {
  import Descr._

  def descrMaxLen: Int = LEN_MAX_DFLT
  def descrDefaultValue: Option[AOStringField] = DEFAULT_VALUE_DFLT
  def descrEditorField: BefText = BlocksEditorFields.TextArea
  def descrFontSizes: Set[Int] = Set.empty

  override def descrBf = BfText(
    name = Descr.BF_NAME_DFLT,
    field = descrEditorField,
    maxLen = descrMaxLen,
    defaultValue = descrDefaultValue
  )
}

