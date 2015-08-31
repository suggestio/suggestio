package io.suggest.xadv.ext.js.fb.m

import io.suggest.common.menum.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 16:30
 * Description: Модель типов узлов фейсбука: люди, страницы, группы, события и т.д.
 */
object FbNodeTypes extends LightEnumeration {

  /** Интерфейс экземпляра модели. */
  protected sealed trait ValT extends super.ValT {
    val mdType: String
    def needPageToken: Boolean = false
    def wallImgSz: FbWallImgSize = FbWallImgSizes.FbPostLink
    override def toString = mdType
    def publishPerms: Seq[FbPermission]
  }

  /**
   * Экземпляр данной модели.
   * @param mdType Строковое значение поля metadata.type в ответе GET /node-id?metadata=1 .
   */
  sealed protected abstract class Val(val mdType: String) extends ValT

  /** Помесь Val с выставленными дефолтовыми publishPerms(). Чисто для укорачивания кода и оптимизации. */
  sealed protected class ValDflt(val mdType: String) extends ValT {
    override def publishPerms = Seq(FbPermissions.PublishActions)
  }


  /** Тип значений модели. */
  override type T = ValT


  /** Юзер, т.е. человек. */
  val User: T = new ValDflt("user")

  /** Страница, т.е. некий "сайт". */
  val Page: T = new Val("page") {
    override def needPageToken = true
    override def publishPerms = Seq(FbPermissions.ManagePages, FbPermissions.PublishPages)
  }

  /** Группа юзеров. */
  val Group: T = new ValDflt("group")

  /** Календарное событие. Редкость. */
  val Event: T = new ValDflt("event")


  /** Все значения этой модели в виде последовательности. */
  def values = Seq[T](User, Page, Group, Event)

  /** Поиск элемента модели по имени. */
  override def maybeWithName(n: String): Option[T] = {
    values.find { v => v.mdType == n }
  }

}
