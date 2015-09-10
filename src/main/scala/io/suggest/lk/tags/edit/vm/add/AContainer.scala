package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_FORM_ID
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.content.{ReplaceWith, ApplyFromOuterHtml}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.util.{IInitLayoutDummy, IInitLayout}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 12:26
 * Description: Контейнер элементов формы добавления тега.
 */
object AContainer extends FindDiv with ApplyFromOuterHtml {

  override type T     = AContainer
  override def DOM_ID = ADD_FORM_ID

  /**
   * Замена контента вместе с контейнером с помощью переданной верстки outerHTML.
   * @param html Верстка (с сервера).
   */
  def overWriteFromHtml(html: String): Unit = {
    for (aCont <- find()) {
      val cont2 = apply( html )
      aCont.replaceWith( cont2 )
    }
  }

}


trait AContainerT extends IVm with IInitLayoutDummy with ReplaceWith {

  override type T = HTMLDivElement

  /** Поиск input'а ввода имени тега. */
  def nameInput = ANameInput.find()

  /** Поиск кнопки добавления тега. */
  def addBtn    = AddBtn.find()

  override def initLayout(): Unit = {
    super.initLayout()
    // Инициализация дочерних элементов.
    val f = IInitLayout.f
    nameInput.foreach(f)
    addBtn.foreach(f)
  }

}


case class AContainer(
  override val _underlying: HTMLDivElement
)
  extends AContainerT
