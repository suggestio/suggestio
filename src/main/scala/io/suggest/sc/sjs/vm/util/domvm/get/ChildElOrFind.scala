package io.suggest.sc.sjs.vm.util.domvm.get

import io.suggest.sc.sjs.vm.util.domvm.{IApplyEl, DomId, IFindEl}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.Element

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:24
 * Description: Утиль для упрощенного поиска дочерних элементов текущего тега.
 */
trait SubTagFind {

  /** Тип экземпляра VM-модели субтега. */
  type SubTagVm_t

  protected def _subtagCompanion: IFindEl { type T = SubTagVm_t }

  protected def _findSubtag(): Option[SubTagVm_t] = {
    _subtagCompanion.find()
  }

}

/** Базовый трейт для Child-реализаций, использующих ISafe. */
sealed trait _ChildSubTagSafe extends ISafe {
  override type T <: Element
}


/**
 * Часто при поиске дочерних элементов тега возникает ситуация, когда поиск не требуется, т.к.
 * архитектурно искомый тег является близким (первым) дочерним тегом, поиск которого не требует лишних телодвижений.
 */
trait ChildElOrFind extends SubTagFind with _ChildSubTagSafe {

  /** Тип DOM-элемента, используемого для сборки суб-тега. */
  protected type SubTagEl_t <: Element

  /** Модель-компаньон для статических данных искомого суб-тега или его поиска. */
  protected def _subtagCompanion: IFindEl with DomId with IApplyEl {
    type T = SubTagVm_t
    type Dom_t = SubTagEl_t
  }

  /** Найти субтег среди дочерних тегов или по id. */
  override protected def _findSubtag(): Option[SubTagVm_t] = {
    DomListIterator(_underlying.children)
      .find {
        _.id == _subtagCompanion.DOM_ID
      }
      .map { node =>
        _subtagCompanion( node.asInstanceOf[SubTagEl_t] )
      }
      .orElse {
        super._findSubtag()
      }
  }

}


/** Бывает необходимо окучить несколько дочерних элементов с помощью [[ChildElOrFind]],
  * поэтому нужен внутренний трейт вместо обычного. */
trait ChildElOrFindInner extends _ChildSubTagSafe { that =>

  /** Вспомогательный внутренний трейт. */
  protected trait ChildFinderT extends ChildElOrFind {
    override type T = that.T
    override def _underlying: T = that._underlying
    // Заменяем protected на public для возможности вызова снаружи.
    override def _findSubtag() = super._findSubtag()
  }

}
