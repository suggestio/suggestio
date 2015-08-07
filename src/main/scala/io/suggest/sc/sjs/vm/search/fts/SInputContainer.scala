package io.suggest.sc.sjs.vm.search.fts

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Fts.{ACTIVE_INPUT_CLASS, INPUT_CONTAINER_ID}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 12:04
 * Description: vm div-контейнера input-поля текстового поиска для задач дизайна и оформления.
 */
object SInputContainer extends FindDiv {
  override type T = SInputContainer
  override def DOM_ID = INPUT_CONTAINER_ID
}


/** Трейт с логикой работы экземпляра vm'ки контейнера input-поля текстового поиска. */
trait SInputContainerT extends SafeElT {

  override type T = HTMLDivElement

  /** Доступ к input-полю. */
  def input = SInput.find()

  /** Визуальная активация поля поиска. */
  def activate(): Unit = {
    addClasses(ACTIVE_INPUT_CLASS)
  }

  /** Визуальная деактивация поля поиска. */
  def deactivate(): Unit = {
    removeClass(ACTIVE_INPUT_CLASS)
  }

}


case class SInputContainer(
  override val _underlying: HTMLDivElement
)
  extends SInputContainerT
