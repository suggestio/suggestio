package io.suggest.model.es

import io.suggest.util.SioEsUtil.DocField

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:37
 * Description: Файл с трейтами поддержки deprecated AkvMut-парсинга.
 * Маппинг результата достигался через комбинирование partial-фунцкий и mutable-аккамулятору.
 * Нужно однажды это удалить.
 */


/**
 * Трейт для статической части модели, построенной через Stackable trait pattern.
 * Для нормального stackable trait без подсветки красным цветом везде, надо чтобы была базовая реализация отдельно
 * от целевой реализации и stackable-реализаций (abstract override).
 * Тут реализованы методы-заглушки для хвоста стэка декораторов. */
trait EsModelStaticMutAkvEmptyT extends EsModelStaticMutAkvT {
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    PartialFunction.empty
  }

  def generateMappingProps: List[DocField] = Nil
}


/** Дополнение к [[EsModelStaticMutAkvEmptyT]], но в applyKeyValue() не происходит MatchError. Втыкается в последнем with. */
trait EsModelStaticMutAkvIgnoreT extends EsModelStaticMutAkvT {
  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case other => // Do nothing
    }
  }
}


/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelPlayJsonT]]. */
trait EsModelStaticMutAkvT extends EsModelCommonStaticT {

  protected def dummy(id: Option[String], version: Option[Long]): T

  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T = {
    val acc = dummy(id, version)
    m foreach applyKeyValue(acc)
    acc
  }

}

