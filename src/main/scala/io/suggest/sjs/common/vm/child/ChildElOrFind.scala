package io.suggest.sjs.common.vm.child

import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.{IApplyEl, IFindEl}
import io.suggest.sjs.common.vm.util.IDomIdApi
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Element, Node}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:24
 * Description: Утиль для упрощенного поиска дочерних элементов текущего тега.
 */

trait ISubTag {
  /** Тип экземпляра VM-модели субтега. */
  type SubTagVm_t
}


trait IFindSubTag extends ISubTag {
  protected def _findSubtag(): Option[SubTagVm_t]
}

trait ISubTagCompanion {
  /** Сложный тип компаньона требует отдельного объявления. */
  protected type SubtagCompanion_t
  /** Модель-компаньон для статических данных искомого суб-тега или его поиска. */
  protected def _subtagCompanion: SubtagCompanion_t
}

/** Примесь для финальный трейтов и реализаций моделей, требующих _findSubtag()
  * для суб-модели со статическим поиском модели. */
trait SubTagFind extends IFindSubTag with ISubTag with ISubTagCompanion {
  override protected type SubtagCompanion_t <: IFindEl { type T = SubTagVm_t }

  protected def _findSubtag(): Option[SubTagVm_t] = {
    _subtagCompanion.find()
  }

}

/** Базовый трейт для Child-реализаций, использующих ISafe. */
sealed trait ISafeElement extends IVm {
  override type T <: Element
}


trait ISubTagElT {
  protected type SubTagEl_t <: Node
}


/**
 * Часто при поиске дочерних элементов тега возникает ситуация, когда поиск не требуется, т.к.
 * архитектурно искомый тег является близким (первым) дочерним тегом, поиск которого не требует лишних телодвижений.
 */
trait ChildElOrFind extends IFindSubTag with ISafeElement with ISubTagCompanion with ISubTagElT {

  /** Тип DOM-элемента, используемого для сборки суб-тега. */
  override protected type SubTagEl_t <: Element

  override protected type SubtagCompanion_t <: IDomIdApi with IApplyEl {
    type T = SubTagVm_t
    type Dom_t = SubTagEl_t
  }

  /** Найти субтег среди дочерних тегов или по id. */
  abstract override protected def _findSubtag(): Option[SubTagVm_t] = {
    DomListIterator(_underlying.children)
      .find { el =>
        _subtagCompanion.isDomIdRelated( el.id )
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
trait ChildElOrFindInner extends ISafeElement { that =>

  /** Вспомогательный внутренний трейт. */
  protected trait ChildFinderT extends ChildElOrFind {
    override type T = that.T
    override def _underlying: T = that._underlying
    // Заменяем protected на public для возможности вызова снаружи.
    abstract override def _findSubtag() = super._findSubtag()
  }

}


/** Root-элемент, имеющий поле wrapper. */
trait RootChildWrapper extends ChildElOrFind {
  override type T <: HTMLElement
  override type SubTagVm_t <: WrapperChildContent
  def wrapper = _findSubtag()
}

/** Wrapper-элемент, имеющий поле content. */
trait WrapperChildContent extends ChildElOrFind {
  override type T <: HTMLElement
  override type SubTagVm_t <: ContentElT
  def content = _findSubtag()

  def vScrollByPx(delta: Int): Unit = {
    _underlying.scrollTop += delta
  }
}

/** Content-элемент. */
trait ContentElT extends IVm {
  override type T <: HTMLElement
}
