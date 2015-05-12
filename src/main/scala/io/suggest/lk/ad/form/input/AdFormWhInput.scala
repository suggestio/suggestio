package io.suggest.lk.ad.form.input

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.ad.form.AdFormConstants._
import org.scalajs.jquery.jQuery

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.05.15 10:27
 * Description: Форма редактирования рекламной карточки содержит поля для задания ширины и длины.
 */
trait AdFormWhInput extends ISize2di {

  /** Получение значения инпута из input'а по его id. */
  private def getValueOf(inputId: String): Int = {
    jQuery("#" + inputId)
      .`val`()
      .toString
      .toInt
  }

  /** Ширина. */
  override def width: Int = getValueOf(WIDTH_INPUT_ID)

  /** Высота. */
  override def height: Int = getValueOf(HEIGHT_INPUT_ID)
}
