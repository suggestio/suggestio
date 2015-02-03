package util

import java.sql.Connection

import models.SqlModelStaticMinimal

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:59
 * Description: функции-хелперы для dfs-моделей.
 */


/** Чтобы не писать везде def save() с логикой выборки между saveInsert() и saveUpdate(),
  * подмешиваем к классам моделей этот трейт.
  * @tparam T Тип записи.
  */
trait SqlModelSave {

  type T <: SqlModelSave

  def companion: SqlModelStaticMinimal

  /** Доступен ли ключ ряда в текущем инстансе? */
  def hasId: Boolean

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c:Connection): T

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c:Connection): Int

  /** Сохранить в базу текущую запись.
    * @return Вернуть текущую или новую запись.
    */
  def save(implicit c: Connection): T = {
    if (!hasId) {
      saveInsert
    } else {
      saveUpdate match {
        case 0 => saveInsert
        case 1 => this.asInstanceOf[T]
      }
    }
  }
}

