package io.suggest.primo

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, OFormat, __}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:46
  * Description: Модель, хранящая абстрактное значение set(x) или unset.
  *
  * По сути своей напоминает Option[X], но подразумевает явную сериализацию в json-объект при любом раскладе.
  * UnSetVal подразумевает необходимость стирания любого значения, а не отсутствие значение.
  * SetVal подразумевает выставление нового значения.
  */

object ISetUnset {

  /** Сборка поддержки play-json. */
  implicit def setUnsetFormat[T: Format]: OFormat[ISetUnset[T]] = {
    (__ \ "v").formatNullable[T]
      .inmap[ISetUnset[T]]( apply, _.toOption )
  }

  implicit def univEq[T: UnivEq]: UnivEq[ISetUnset[T]] = UnivEq.force

  /** Собрать из option'а. */
  def apply[T](vOpt: Option[T]): ISetUnset[T] = {
    vOpt.fold[ISetUnset[T]](UnSetVal)(SetVal.apply)
  }

}

/** Интерфейс модели Set/unset. */
sealed trait ISetUnset[+T] {
  def isSet: Boolean
  def get: T
  def toList: List[T]
  def toOption: Option[T]
  def map[A](f: T => A): ISetUnset[A]
  def foreach[U](f: T => U): Unit
}


object SetVal {
  implicit def univEq[T: UnivEq]: UnivEq[SetVal[T]] = UnivEq.derive
}

/** Выставление значения указанного типа. */
case class SetVal[T](value: T) extends ISetUnset[T] {
  override def isSet    = true
  override def get      = value
  override def toList   = value :: Nil
  override def toOption = Some(value)
  override def map[A](f: T => A) = SetVal(f(value))
  override def foreach[U](f: (T) => U): Unit = f(value)
}


/** Отмена значения. */
case object UnSetVal extends ISetUnset[Nothing] {
  override def isSet    = false
  override def get      = throw new IllegalArgumentException
  override def toList   = Nil
  override def toOption = None
  override def map[A](f: Nothing => A) = this
  override def foreach[U](f: (Nothing) => U): Unit = {}

  implicit def univEq: UnivEq[UnSetVal.type] = UnivEq.derive
}
