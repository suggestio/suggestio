package io.suggest.common.empty

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.16 15:00
 * Description: Статическая утиль для всяких isEmpty-нужд.
 */
object EmptyUtil {

  /**
   * Генерация неявно-пустого экземпляра модели.
   * @param vOpt Опциональный экземпляр неявно-пустой модели.
   * @param empty Неявно-пустой экземпляр модели.
   * @return Неявно пустой экземпляр модели.
   */
  def opt2ImplEmpty[T](vOpt: Option[T])(empty: T): T = {
    opt2ImplEmpty1(vOpt)(empty)
  }
  def opt2ImplEmpty1[T](vOpt: Option[T])(emptyF: => T): T = {
    vOpt.getOrElse(emptyF)
  }


  // TODO Добавить поддержку GenericCompanion.
  /** Сборка фунции opt2implEmpty. */
  def opt2ImplMEmptyF[T1](model: IEmpty { type T = T1 }): Option[T1] => T1 = {
    opt2ImplEmpty1(_)(model.empty)
  }

  // TODO добавить поддержку scala-collections, которые бывают внутри Option().

  def opt2ImplEmptyF[T](empty: T): Option[T] => T = {
    opt2ImplEmpty1F(empty)
  }
  def opt2ImplEmpty1F[T](empty: => T): Option[T] => T = {
    opt2ImplEmpty1(_)(empty)
  }


  def someF[T]: T => Option[T] = {
    { Some.apply }
  }


  def implEmpty2Opt[T <: IIsEmpty](v: T): Option[T] = {
    if (v.isEmpty) None else Some(v)
  }

  def implEmpty2OptF[T <: IIsEmpty]: T => Option[T] = {
    implEmpty2Opt
  }

}
