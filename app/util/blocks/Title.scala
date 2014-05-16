package util.blocks

import models._
import util.blocks.BlocksEditorFields.BefText
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:13
 * Description: Утиль для блоков, содержащих titleBf.
 */

object Title {
  val BF_NAME_DFLT = "title"
  val LEN_MAX_DFLT = 128
  val DEFAULT_VALUE_DFLT = Some(AOStringField("Платье", AOFieldFont("444444")))
  val BF_TITLE_DFLT = BfText(
    name = BF_NAME_DFLT,
    field = BlocksEditorFields.TextArea,
    maxLen = LEN_MAX_DFLT,
    defaultValue = DEFAULT_VALUE_DFLT
  )
}

/** Базовый трейт для статических и динамических bfTitle. Добавляет поле в форму. */
trait TitleT extends ValT {
  def titleBf: BfText
  abstract override def blockFieldsRev: List[BlockFieldT] = titleBf :: super.blockFieldsRev
}
trait Title extends TitleT {
  import Title._
  def titleMaxLen: Int = LEN_MAX_DFLT
  def titleDefaultValue: Option[AOStringField] = DEFAULT_VALUE_DFLT
  def titleEditorField: BefText = BlocksEditorFields.TextArea
  def titleFontSizes: Set[Int] = Set.empty
  override def titleBf: BfText = BfText(
    name = BF_NAME_DFLT,
    field = titleEditorField,
    maxLen = titleMaxLen,
    defaultValue = titleDefaultValue,
    withFontSizes = titleFontSizes
  )
}
trait TitleStatic extends TitleT {
  import Title._
  override final def titleBf = BF_TITLE_DFLT
}
