package io.suggest.xadv.ext.js.fb.m

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 16:30
 * Description: Модель типов узлов фейсбука: люди, страницы, группы, события и т.д.
 */
object FbNodeTypes extends LightEnumeration {

  /**
   * Экземпляр данной модели.
   * @param mdType Строковое значение поля metadata.type в ответе GET /node-id?metadata=1 .
   */
  sealed protected class Val(val mdType: String) extends ValT {
    def needPageToken: Boolean = false
  }

  /** Тип значений модели. */
  override type T = Val

  /** Юзер, т.е. человек. */
  val User: T = new Val("user")

  /** Страница, т.е. некий "сайт". */
  val Page: T = new Val("page") {
    override def needPageToken = true
  }

  /** Группа юзеров. */
  val Group: T = new Val("group")

  /** Календарное событие. Редкость. */
  val Event: T = new Val("event")


  /** Все значения этой модели в виде последовательности. */
  def values = Seq[T](User, Page, Group, Event)

  /** Поиск элемента модели по имени. */
  override def maybeWithName(n: String): Option[T] = {
    values.find { v => v.mdType == n }
  }

}
