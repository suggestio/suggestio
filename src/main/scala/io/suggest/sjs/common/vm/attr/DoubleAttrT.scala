package io.suggest.sjs.common.vm.attr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 10:59
  * Description: Поддержка чтения/запились double-аттрибутов.
  */
trait DoubleAttrT extends AttrVmT{

  /** Извлечь decimal-значение аттрибута, даже если тот содержит какие-то строковые данные. */
  def getDoubleAttrStrict(name: String): Option[Double] = {
    getNonEmptyAttribute(name)
      .map { _.toDouble }
  }

  def setDoubleAttr(name: String, value: Double): Unit = {
    setAttribute(name, value.toString)
  }

}
