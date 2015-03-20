package util.tpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.15 9:41
 * Description: Утиль для рендера аттрибутов классов и стилей.
 */
object CssFormat {

  /** Добавить пробелы в коллекцию/итератор и вернуть новый итератор.
    * Перед каждым исходным элементом добавляется по одному пробелу. */
  def classesPreSpaced(cssClasses: TraversableOnce[String]): Iterator[String] = {
    cssClasses
      .toIterator
      .filter(!_.isEmpty)
      .flatMap { cc => Seq(" ", cc) }
  }


  /** Список css-классов с возможносью опционального добавления act-класса.
    * Используется при рендере панелей с текущим выбранным элементом панели. */
  def classesAct(isAct: Boolean, other: String*): Iterator[String] = {
    var i0 = other.iterator
    if (isAct)
      i0 ++= Iterator("__act")
    i0
  }

}
