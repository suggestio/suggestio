package io.suggest.mbill2.m.tags

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 18:15
  * Description: Интерфейс к полю названия тега для экземпляров моделей.
  */
trait ITagFaceOpt {

  def tagFaceOpt: Option[String]

}
