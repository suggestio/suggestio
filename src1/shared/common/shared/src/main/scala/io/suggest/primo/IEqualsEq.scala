package io.suggest.primo

import scala.runtime.ScalaRunTime

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 18:46
  * Description: final-реализации для hashCode и equals(), ориентированные на выжимание O(1)
  * производительности для использования в качестве уникального instance-ключа под HashMap.
  *
  * equals() выдаёт true только для this.
  * hashCode -- это lazy val, и сохраняет своё значение после первого вычисления (для ускорения ключей HashMap).
  *
  * Удобно для рендера в связке со ScalaCSS: реализации трейта можно использовать одновременно как:
  * - чистый контент для рендера
  * - быстрые ключи для styleF()
  *
  * Из проблем: разные инстансы больше нельзя сравнивать между собой, увы.
  */
trait IHashCodeLazyVal extends Product {

  /**
    * Реализация хеширования инстанса, когда операций сравнения на повтоных вызовах сведено к O(1).
    * Это надо для быстрого рендера, который зависит от Map[IDocTag,_] (внутри scalaCSS Domain).
    */
  override final lazy val hashCode = ScalaRunTime._hashCode(this)

}


/** Использовать eq вместо equals. */
trait IEqualsEq {

  /** Сравнивание по указателям, т.е. O(1).
    * Это чрезвычайно суровое решение, но так надо, чтобы подружить scalaCSS Domains и рендеринг. */
  override final def equals(obj: Any): Boolean = {
    obj match {
      case idt: AnyRef => idt eq this
      case _           => false
    }
  }

}
