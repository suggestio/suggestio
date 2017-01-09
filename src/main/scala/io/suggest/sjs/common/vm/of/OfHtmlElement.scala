package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.HTMLElement

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 11:23
 * Description: Поддержка выборки по html-элементу.
 */

trait OfHtmlElement extends OfBase {

  /** Тестирование на пригодность к заворачиванию в инстанс этой VM. */
  def _isWantedHtmlEl(el: HTMLElement): Boolean

  /**
   * Протестировать элемент по отношению к этой модели.
   * Если тест будет удачен, то вернуть инстанс vm'ки в Some()
   * @param el Возможный элемент этой vm'ки.
   * @return Some(Vm) | None.
   */
  def ofHtmlEl(el: HTMLElement): Option[T] = {
    if (OfUtil.isInstance(el)) {
      ofHtmlElUnsafe(el)
    } else {
      None
    }
  }

  /** ofHtmlEl() без проверку на null, undefined и т.д. */
  def ofHtmlElUnsafe(el: HTMLElement): Option[T] = {
    if ( _isWantedHtmlEl(el) ) {
      val el1 = el.asInstanceOf[Dom_t]
      Some( apply(el1) )
    } else {
      None
    }
  }


  /** Надстройка над ofHtmlEl(), дающая возможность путешествия вверх по дереву в поисках искомого элемента. */
  @tailrec
  final def ofHtmlElUp(el: HTMLElement): Option[T] = {
    // Пишем без orElse, ибо tailrec.
    if (OfUtil.isInstance(el)) {
      val res0 = ofHtmlElUnsafe(el)
      if (res0.isDefined) {
        res0
      } else {
        ofHtmlElUp( el.parentElement )
      }

    } else {
      None
    }
  }

}
