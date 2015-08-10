package io.suggest.sc.sjs.vm.util.domvm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:33
 * Description: Интерфейс для доступа к id элемента в DOM.
 */
trait DomId {

  /** id элемента в рамках DOM.
    * В случае [[IndexedDomId]] тут префикс элемента. */
  def DOM_ID: String

}


/** Бывает что нужны динамические id на основе какого-то параметра какого-то типа.
  * Тут очень абстрактный трейт для более конкретный trait-реализаций этой функции. */
trait DynDomId {

  type DomIdArg_t

  def getDomId(arg: DomIdArg_t): String

}


/** Трейт для генерации id элементов по как "строка" + индекс.
  * id1, id2, id3... */
trait IndexedDomId extends DynDomId with DomId {

  override type DomIdArg_t = Int

  override def getDomId(arg: DomIdArg_t): String = {
    DOM_ID + arg
  }
}
